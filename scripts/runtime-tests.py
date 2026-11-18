#!/usr/bin/env python3
"""Run isolated Fabric integration fixtures and fail if their success marker is missing."""
import argparse
import datetime
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SUITES = {
    'pastoral': ('PastoralRuntimeChecks', 'PASTORAL'),
    'railstrains': ('TrainRuntimeChecks', 'RAIL_TRAINS'),
    'railsindustry': ('RailsIndustryRuntimeChecks', 'RAILS_INDUSTRY'),
    'railstations': ('RailStationsRuntimeChecks', 'RAIL_STATIONS'),
    'rails': ('RailsRuntimeChecks', 'RAILS'),
    'remote': ('RemoteRuntimeChecks', 'REMOTE'),
    'pump': ('PumpRuntimeChecks', 'PUMP'),
    'quantum': ('QuantumChestRuntimeChecks', 'QUANTUM_CHEST'),
    'compactor': ('CompactorRuntimeChecks', 'COMPACTOR'),
    'newcontent': ('NewContentRuntimeChecks', 'NEWCONTENT'),
    'tech': ('TechCompatibilityRuntimeChecks', 'TECHCOMPAT'),
    'progression': ('ProgressionRuntimeChecks', 'PROGRESSION'),
    'followup': ('AuditFollowupRuntimeChecks', 'FOLLOWUP'),
    'fluidpersistence': ('FluidPersistenceRuntimeChecks', 'FLUIDPERSIST'),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('suites', nargs='*', metavar='SUITE', help='pastoral, remote, pump, quantum, compactor, newcontent, progression, followup, fluidpersistence; tech, rails, railstations, railsindustry and railstrains are optional')
    parser.add_argument('--timeout', type=int, default=240, help='Maximum seconds per server')
    parser.add_argument('--techreborn-jar', type=Path, default=ROOT / 'build/compatibility/techreborn/TechReborn-5.11.19.jar')
    parser.add_argument('--export-world', type=Path, help='Copy a successful single-suite world to this new directory')
    args = parser.parse_args()
    if args.timeout < 30:
        parser.error("--timeout must be at least 30 seconds")
    suites = args.suites or [suite for suite in SUITES if suite not in ('tech', 'rails', 'railstations', 'railsindustry', 'railstrains')]
    if any(suite not in SUITES for suite in suites):
        parser.error("Unknown suite; choose pastoral, remote, pump, quantum, compactor, newcontent, progression, followup, fluidpersistence, rails, railstations, railsindustry, railstrains or tech")
    if args.export_world and (len(suites) != 1 or args.export_world.exists()):
        parser.error('--export-world requires one suite and a new output directory')
    if 'tech' in suites and not args.techreborn_jar.is_file():
        parser.error('Tech suite requires an official Tech Reborn 1.21 jar; pass --techreborn-jar')
    eula = ROOT / 'run/eula.txt'
    if not eula.exists() or 'eula=true' not in eula.read_text().lower():
        parser.error('Requires the existing project EULA acceptance in run/eula.txt; no acceptance is created.')
    if 'progression' in suites:
        subprocess.run([sys.executable, str(ROOT / 'scripts/audit-progression.py')], cwd=ROOT, check=True)
    compile_tasks = ['testClasses']
    if any(suite.startswith('rails') for suite in suites):
        compile_tasks += [':addons:mythic_rails:testClasses', ':addons:mythic_rails:jar']
    subprocess.run([str(ROOT / 'gradlew'), *compile_tasks, '--offline', '--no-daemon'], cwd=ROOT, check=True)
    report = ROOT / 'build/reports/runtime-tests' / datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    report.mkdir(parents=True)
    results = []
    for suite in suites:
        class_name, marker = SUITES[suite]
        out = report / suite
        out.mkdir()
        with tempfile.TemporaryDirectory(prefix='obsidium-runtime-') as temporary:
            work = Path(temporary)
            shutil.copy2(eula, work / 'eula.txt')
            (work / 'server.properties').write_text(
                'server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n'
                'enable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\n'
                'level-type=minecraft:flat\nlevel-seed=obsidium-runtime-tests\ngenerate-structures=false\nmax-tick-time=60000\ngamemode=creative\n'
                'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n')
            mods = work / 'mods'
            mods.mkdir()
            if suite == 'tech':
                shutil.copy2(args.techreborn_jar, mods / args.techreborn_jar.name)
            if suite.startswith('rails'):
                addon = ROOT / 'addons/mythic_rails'
                addon_version = json.loads((addon / 'build/resources/main/fabric.mod.json').read_text())['version']
                dev_jar = addon / 'build/devlibs' / ('obsidium-mythic-rails-' + addon_version + '-dev.jar')
                if not dev_jar.is_file():
                    raise RuntimeError('Missing current addon dev JAR: ' + str(dev_jar))
                shutil.copy2(dev_jar, mods / dev_jar.name)
            classes = (ROOT / 'addons/mythic_rails' if suite.startswith('rails') else ROOT) / 'build/classes/java/test'
            fixture_classes = sorted((classes / 'net/valentin/obsidium').glob(class_name + '*.class'))
            if not fixture_classes:
                raise RuntimeError('Missing compiled fixture: ' + class_name)
            with zipfile.ZipFile(mods / 'runtime-checks.jar', 'w') as jar:
                jar.writestr('fabric.mod.json', json.dumps({
                    'schemaVersion': 1, 'id': 'obsidium_runtime_checks', 'version': '1.0.0',
                    'environment': 'server', 'entrypoints': {'main': ['net.valentin.obsidium.' + class_name]},
                    'depends': {'obsidium': '*', 'fabricloader': '*', 'fabric-api': '*'},
                }))
                for compiled in fixture_classes:
                    jar.write(compiled, compiled.relative_to(classes))
            # Temporary init script keeps the test mod out of production source sets and jars.
            init_text = """gradle.beforeProject { p ->
 if (p.path == ':') p.plugins.withId('net.fabricmc.fabric-loom-remap') {
  p.loom.runs { integrationTest { server(); runDir('%s'); programArg('--nogui') } }
  p.tasks.matching { it.name == 'runIntegrationTest' }.configureEach { timeout = java.time.Duration.ofSeconds(%d) }
 }
}
""" % (str(work).replace('\\', '\\\\').replace("'", "\\'"), args.timeout - 15)
            init = work / 'integration.gradle'
            init.write_text(init_text)
            (out / 'integration.gradle.txt').write_text(init_text)
            command = [str(ROOT / 'gradlew'), 'runIntegrationTest', '--offline', '--no-daemon',
                       '--no-configuration-cache', '--init-script', str(init)]
            print('Running ' + suite + ' → ' + str(out), flush=True)
            timed_out = False
            for launch in range(2 if suite in ('railstations', 'railstrains', 'pastoral') else 1):
                with (out / 'gradle.log').open('w' if launch == 0 else 'a') as log:
                    process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                                               start_new_session=True)
                    try:
                        code = process.wait(timeout=args.timeout)
                    except subprocess.TimeoutExpired:
                        # Kill only this isolated test process group, including its server child.
                        import os
                        import signal
                        os.killpg(process.pid, signal.SIGTERM)
                        try:
                            process.wait(timeout=10)
                        except subprocess.TimeoutExpired:
                            os.killpg(process.pid, signal.SIGKILL)
                            process.wait()
                        code = process.returncode
                        timed_out = True
                if suite in ('railstations', 'railstrains', 'pastoral') and launch == 0:
                    first_log = (out / 'gradle.log').read_text(errors='replace')
                    if code != 0 or timed_out or marker + '_RESTART_READY' not in first_log or marker + '_FAILED' in first_log:
                        break
                    if (work / 'logs/latest.log').exists():
                        shutil.copy2(work / 'logs/latest.log', out / 'server-before-restart.log')
                    print('Restarting the same test world', flush=True)
            native_log = work / 'logs/latest.log'
            if native_log.exists():
                shutil.copy2(native_log, out / 'server.log')
            saved_data = work / 'world/data/obsidium_quantum_link_network.dat'
            if saved_data.exists():
                shutil.copy2(saved_data, out / saved_data.name)
            shutil.copy2(work / 'server.properties', out / 'server.properties')
            log_text = (out / 'gradle.log').read_text(errors='replace')
            checks = [line.split(marker + '_OK_', 1)[1].strip()
                      for line in log_text.splitlines() if marker + '_OK_' in line]
            passed = code == 0 and not timed_out and marker + '_ALL_PASSED' in log_text and marker + '_FAILED' not in log_text and not any('Parsing error loading recipe ' + mod + ':' in log_text for mod in ('obsidium', 'mythic_rails'))
            if passed and args.export_world:
                shutil.copytree(work / 'world', args.export_world)
            results.append({'suite': suite, 'passed': passed, 'exit_code': code,
                            'timed_out': timed_out, 'checks': checks})
            (report / 'summary.json').write_text(json.dumps(results, indent=2) + '\n')
            print(f'{suite}: {"PASS" if passed else "FAIL"} ({len(checks)} checks)', flush=True)
    print('Report: ' + str(report), flush=True)
    return 0 if all(result['passed'] for result in results) else 1


if __name__ == '__main__':
    raise SystemExit(main())
