#!/usr/bin/env python3
"""Boot remapped production JARs with the real Prism Loader/JVM, without opening a user world."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import tempfile
import threading
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def metadata(path):
    with zipfile.ZipFile(path) as archive:
        return json.loads(archive.read('fabric.mod.json'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--prism-root', type=Path, default=Path.home() / 'Library/Application Support/PrismLauncher')
    parser.add_argument('--loader', default='0.16.5')
    parser.add_argument('--addon-jar', type=Path, required=True)
    parser.add_argument('--core-jar', type=Path, default=ROOT / 'build/libs/obsidium-1.1.0.jar')
    parser.add_argument('--probe-mod', type=Path, help='Remapped server test mod that prints RAILS_INDUSTRY_ALL_PASSED and halts')
    parser.add_argument('--timeout', type=int, default=150)
    parser.add_argument('--expect-redirect-crash', action='store_true', help='Negative control for the original 0.4.0 failure')
    args = parser.parse_args()
    base = args.prism_root / 'libraries'
    loader = base / ('net/fabricmc/fabric-loader/' + args.loader) / ('fabric-loader-' + args.loader + '.jar')
    java = args.prism_root / 'java/java-runtime-delta/bin/java'
    if not java.is_file():
        parser.error('Prism Java 21 runtime missing')
    mapping = base / 'net/fabricmc/intermediary/1.21/intermediary-1.21.jar'
    minecraft = Path.home() / '.gradle/caches/fabric-loom/1.21/minecraft-server.jar'
    eula = ROOT / 'run/eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text().lower():
        parser.error('Requires the existing project EULA acceptance; no new acceptance is created')
    jars = [loader, mapping, minecraft]
    with zipfile.ZipFile(loader) as archive:
        installer = json.loads(archive.read('fabric-installer.json'))
        for library in installer['libraries']['common']:
            group, name, version = library['name'].split(':')
            path = base / group.replace('.', '/') / name / version / (name + '-' + version + '.jar')
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != library['sha256']:
                parser.error('Missing or mismatched Prism library: ' + str(path))
            jars.append(path)
    api = None
    for path in (args.prism_root / 'instances/Obsidium-TechReborn-QA/minecraft/mods').glob('*.jar'):
        try:
            if metadata(path)['id'] == 'fabric-api':
                api = path
                break
        except (KeyError, ValueError, zipfile.BadZipFile):
            pass
    if api is None:
        parser.error('Fabric API not found in QA instance')
    report = ROOT / 'build/reports/production-smoke' / datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    report.mkdir(parents=True)
    addon_meta = metadata(args.addon_jar)
    print('Production boot: Loader ' + args.loader + ', addon ' + addon_meta['version'] + ' -> ' + str(report), flush=True)
    with tempfile.TemporaryDirectory(prefix='obsidium-production-smoke-') as temporary:
        work = Path(temporary)
        (work / 'mods').mkdir()
        for source in [args.core_jar, args.addon_jar, api] + ([args.probe_mod] if args.probe_mod else []):
            shutil.copy2(source, work / 'mods' / source.name)
        shutil.copy2(eula, work / 'eula.txt')
        (work / 'server.properties').write_text(
            'server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nenable-query=false\nenable-rcon=false\n'
            'view-distance=2\nsimulation-distance=2\nlevel-type=minecraft:flat\ngenerate-structures=false\n'
            'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n')
        command = [str(java), '-Xms256m', '-Xmx1G', '-Djava.awt.headless=true', '-cp', os.pathsep.join(str(p) for p in jars),
                   'net.fabricmc.loader.impl.launch.knot.KnotServer', '--nogui']
        started = threading.Event()
        process = subprocess.Popen(command, cwd=work, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, start_new_session=True)
        def collect():
            with (report / 'server.log').open('w') as output:
                for line in process.stdout:
                    output.write(line)
                    output.flush()
                    if 'Done (' in line and 'For help' in line:
                        started.set()
        reader = threading.Thread(target=collect)
        reader.start()
        deadline = time.monotonic() + args.timeout
        stopped = False
        while process.poll() is None and time.monotonic() < deadline:
            if started.is_set() and not stopped and not args.probe_mod:
                process.stdin.write('stop\n')
                process.stdin.flush()
                stopped = True
            time.sleep(0.1)
        timed_out = process.poll() is None
        if timed_out:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
        process.wait()
        reader.join(timeout=10)
        log = (report / 'server.log').read_text(errors='replace')
        reproduced = 'FactoryRedirectWrapperMixinTransformer' in log and 'ClassCastException' in log
        passed = (reproduced and not started.is_set()) if args.expect_redirect_crash else (
            started.is_set() and process.returncode == 0 and not timed_out and 'MixinTransformerError' not in log)
        if args.probe_mod:
            passed = passed and 'RAILS_INDUSTRY_ALL_PASSED' in log and 'RAILS_INDUSTRY_FAILED' not in log
        checks = [line.split('RAILS_INDUSTRY_OK_', 1)[1].strip() for line in log.splitlines() if 'RAILS_INDUSTRY_OK_' in line]
        result = {'passed': passed, 'checks': checks, 'loader': args.loader, 'addon': addon_meta['version'], 'java': str(java),
                  'expect_redirect_crash': args.expect_redirect_crash, 'redirect_crash_reproduced': reproduced,
                  'server_started': started.is_set(), 'exit_code': process.returncode, 'timed_out': timed_out,
                  'addon_sha256': hashlib.sha256(args.addon_jar.read_bytes()).hexdigest()}
        (report / 'summary.json').write_text(json.dumps(result, indent=2) + '\n')
        print(('PASS' if passed else 'FAIL') + ': ' + str(report / 'summary.json'), flush=True)
        return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())
