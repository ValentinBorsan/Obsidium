#!/usr/bin/env python3
"""
Obsidium Machine SFX Generator
==============================

Procedurally synthesizes ORIGINAL machine sound effects for the Obsidium mod
(no vanilla Minecraft sounds, no downloaded samples -> 100% free / royalty-free).

Pipeline:
  numpy/scipy synthesis  ->  mono OGG Vorbis (via `soundfile`)  ->  assets/obsidium/sounds/machine/*.ogg
                          ->  assets/obsidium/sounds.json
                          ->  src/main/java/net/valentin/obsidium/sound/ModSounds.java

Mono is REQUIRED: Minecraft only applies 3D positional attenuation to mono sounds.

Requirements (all free):
  pip install numpy scipy soundfile

Usage:
  python3 tools/sfx/generate_machine_sounds.py              # generate everything
  python3 tools/sfx/generate_machine_sounds.py --list       # list available sounds
  python3 tools/sfx/generate_machine_sounds.py --only forge_strike steam_hiss
  python3 tools/sfx/generate_machine_sounds.py --wav-preview  # also dump .wav files to tools/sfx/preview/

Every sound is deterministic (seeded), so re-running produces identical files.
To tweak a sound, edit its recipe function below and re-run.
"""

import argparse
import json
import os
import sys

import numpy as np
from scipy import signal

try:
    import soundfile as sf
except ImportError:  # pragma: no cover
    sys.exit("Missing dependency: pip install soundfile  (bundles libsndfile with OGG Vorbis support)")

SR = 44100
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MOD_ID = "obsidium"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MOD_ID)
SOUND_DIR = os.path.join(ASSETS, "sounds", "machine")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "net", "valentin", MOD_ID, "sound", "ModSounds.java")
PREVIEW_DIR = os.path.join(ROOT, "tools", "sfx", "preview")


# =====================================================================
#  DSP toolkit
# =====================================================================

def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def rng(seed):
    return np.random.default_rng(seed)


def white(dur, seed=0):
    return rng(seed).uniform(-1.0, 1.0, int(dur * SR))


def brown(dur, seed=0):
    x = np.cumsum(rng(seed).normal(0, 1, int(dur * SR)))
    x = signal.lfilter([1], [1, -0.995], np.diff(x, prepend=0))  # leaky integrator
    return normalize(x)


def sine(freq, dur, phase=0.0):
    """freq may be a scalar or an array (for sweeps)."""
    n = int(dur * SR)
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    return np.sin(2 * np.pi * np.cumsum(f) / SR + phase)


def saw(freq, dur):
    n = int(dur * SR)
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    ph = np.cumsum(f) / SR
    return 2.0 * (ph - np.floor(ph + 0.5))


def square(freq, dur, duty=0.5):
    n = int(dur * SR)
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    ph = (np.cumsum(f) / SR) % 1.0
    return np.where(ph < duty, 1.0, -1.0)


def lowpass(x, cutoff, order=2):
    b, a = signal.butter(order, min(cutoff, SR / 2 - 100) / (SR / 2), "low")
    return signal.lfilter(b, a, x)


def highpass(x, cutoff, order=2):
    b, a = signal.butter(order, max(cutoff, 10) / (SR / 2), "high")
    return signal.lfilter(b, a, x)


def bandpass(x, lo, hi, order=2):
    b, a = signal.butter(order, [max(lo, 10) / (SR / 2), min(hi, SR / 2 - 100) / (SR / 2)], "band")
    return signal.lfilter(b, a, x)


def sweep_lowpass(x, f_start, f_end, blocks=64):
    """Time-varying lowpass (processed in blocks with continuous filter state)."""
    out = np.zeros_like(x)
    n = len(x)
    edges = np.linspace(0, n, blocks + 1).astype(int)
    zi = None
    for i in range(blocks):
        f = f_start + (f_end - f_start) * (i / max(1, blocks - 1))
        b, a = signal.butter(2, np.clip(f, 30, SR / 2 - 200) / (SR / 2), "low")
        if zi is None:
            zi = signal.lfilter_zi(b, a) * 0
        seg, zi = signal.lfilter(b, a, x[edges[i]:edges[i + 1]], zi=zi)
        out[edges[i]:edges[i + 1]] = seg
    return out


def env_adsr(dur, a=0.01, d=0.1, s=0.7, r=0.2):
    n = int(dur * SR)
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    ns = max(0, n - na - nd - nr)
    e = np.concatenate([
        np.linspace(0, 1, max(1, na), endpoint=False),
        np.linspace(1, s, max(1, nd), endpoint=False),
        np.full(ns, s),
        np.linspace(s, 0, max(1, nr)),
    ])
    return fit(e, n)


def env_exp(dur, decay):
    """Exponential decay envelope; decay = time constant in seconds."""
    return np.exp(-t_axis(dur) / decay)


def env_attack_exp(dur, attack, decay):
    t = t_axis(dur)
    return (1 - np.exp(-t / max(attack, 1e-4))) * np.exp(-t / decay)


def fade(x, fin=0.005, fout=0.02):
    x = x.copy()
    ni, no = int(fin * SR), int(fout * SR)
    if ni > 0:
        x[:ni] *= np.linspace(0, 1, ni)
    if no > 0:
        x[-no:] *= np.linspace(1, 0, no)
    return x


def loopable(x, xfade=0.15):
    """Crossfade the tail into the head so the sound loops seamlessly."""
    n = int(xfade * SR)
    head, body, tail = x[:n], x[n:-n] if n else x, x[-n:]
    w = np.linspace(0, 1, n)
    blended = tail * (1 - w) + head * w
    return np.concatenate([body, blended])


def fit(x, n):
    if len(x) >= n:
        return x[:n]
    return np.pad(x, (0, n - len(x)))


def mix(*parts):
    n = max(len(p) for p in parts)
    return sum(fit(p, n) for p in parts)


def place(base, part, at):
    """Add `part` into `base` starting at time `at` (seconds)."""
    i = int(at * SR)
    end = min(len(base), i + len(part))
    if i < len(base):
        base[i:end] += part[:end - i]
    return base


def normalize(x, peak=0.9):
    m = np.max(np.abs(x)) if len(x) else 0
    return x * (peak / m) if m > 0 else x


def soft_clip(x, drive=1.5):
    return np.tanh(x * drive) / np.tanh(drive)


def reverb(x, room=0.35, damp=3500, wet=0.25, seed=7):
    """Cheap convolution reverb using a decaying noise impulse response."""
    ir_len = room * 1.6
    ir = white(ir_len, seed) * env_exp(ir_len, room * 0.35)
    ir = lowpass(ir, damp)
    ir[0] = 0
    ir = ir / (np.sum(np.abs(ir)) + 1e-9) * 6
    tail = signal.fftconvolve(x, ir)[: len(x) + len(ir)]
    dry = np.pad(x, (0, len(tail) - len(x)))
    return dry * (1 - wet) + tail * wet


def metal_hit(dur, base, seed=0, decay=0.35, brightness=1.0):
    """Inharmonic modal synthesis -> metallic clang (anvil / plate)."""
    r = rng(seed)
    ratios = [1.0, 2.76, 5.40, 8.93, 13.34, 3.17, 6.92]
    out = np.zeros(int(dur * SR))
    for k, ratio in enumerate(ratios):
        f = base * ratio * (1 + r.uniform(-0.01, 0.01))
        if f > SR / 2.2:
            continue
        amp = (1.0 / (k + 1) ** (0.9 / brightness)) * r.uniform(0.7, 1.0)
        out += amp * sine(f, dur, r.uniform(0, 6.28)) * env_exp(dur, decay / (1 + k * 0.35))
    click = highpass(white(0.012, seed + 1), 2500) * env_exp(0.012, 0.003)
    return place(out, click * 1.5, 0)


def bubbles(dur, rate, seed=0, fmin=180, fmax=900, size=0.04):
    """Liquid bubble pops (rising-pitch blips)."""
    r = rng(seed)
    out = np.zeros(int(dur * SR))
    count = int(dur * rate)
    for _ in range(count):
        at = r.uniform(0, dur - size)
        f0 = r.uniform(fmin, fmax)
        d = size * r.uniform(0.6, 1.4)
        f = np.linspace(f0, f0 * r.uniform(1.6, 2.6), int(d * SR))
        blip = sine(f, d) * env_attack_exp(d, 0.002, d * 0.3) * r.uniform(0.3, 1.0)
        place(out, blip, at)
    return out


def crackle(dur, rate, seed=0):
    r = rng(seed)
    out = np.zeros(int(dur * SR))
    for _ in range(int(dur * rate)):
        at = r.uniform(0, dur - 0.01)
        d = r.uniform(0.002, 0.008)
        place(out, highpass(white(d, int(r.integers(1000000000))), 1500) * env_exp(d, d * 0.3) * r.uniform(0.2, 1.0), at)
    return out


# =====================================================================
#  Sound recipes  (name -> function(variant_index) -> np.ndarray)
#  Each returns a mono float array in [-1, 1].
# =====================================================================

RECIPES = {}
META = {}


def sound(name, variants=1, subtitle=None, loop=False):
    def deco(fn):
        RECIPES[name] = fn
        META[name] = {"variants": variants, "subtitle": subtitle or name.replace("_", " ").title(), "loop": loop}
        return fn
    return deco


# ---------- Generators ----------

@sound("generator_hum", subtitle="Volcanite Generator hums", loop=True)
def generator_hum(v):
    d = 2.0
    t = t_axis(d)
    wobble = 1 + 0.004 * np.sin(2 * np.pi * 0.5 * t)  # 0.5 Hz -> 1 full cycle per loop
    drone = 0.55 * sine(55 * wobble, d) + 0.30 * sine(110 * wobble, d) + 0.12 * saw(165 * wobble, d)
    drone = lowpass(drone, 600)
    rumble = 0.25 * lowpass(brown(d, 11), 140)
    lava = 0.12 * bubbles(d, 6, seed=12, fmin=120, fmax=380, size=0.06)
    return loopable(soft_clip(mix(drone, rumble, lava), 1.2))


@sound("steam_generator_run", subtitle="Steam Turbine spins", loop=True)
def steam_generator_run(v):
    d = 2.0
    t = t_axis(d)
    # Turbine whine: blade-pass harmonic stack with slight flutter
    f = 420 * (1 + 0.01 * np.sin(2 * np.pi * 1.0 * t))
    whine = 0.22 * sine(f, d) + 0.10 * sine(f * 2, d) + 0.05 * sine(f * 3.01, d)
    whine *= 0.8 + 0.2 * np.sin(2 * np.pi * 14 * t)  # blade chop
    hiss = 0.35 * bandpass(white(d, 21), 2500, 9000) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * t))
    body = 0.40 * lowpass(saw(48, d) + 0.5 * sine(96, d), 300)
    return loopable(normalize(mix(whine, hiss, body), 0.85))


@sound("turbine_spinup", subtitle="Turbine spins up")
def turbine_spinup(v):
    d = 2.2
    t = t_axis(d)
    curve = 1 - np.exp(-t / 0.7)
    f = 60 + 360 * curve
    whine = 0.3 * sine(f, d) + 0.12 * sine(f * 2, d)
    chop = 0.7 + 0.3 * np.sin(2 * np.pi * np.cumsum(2 + 12 * curve) / SR)
    hiss = 0.25 * sweep_lowpass(white(d, 31), 400, 8000) * curve
    return fade(normalize(mix(whine * chop, hiss) * env_adsr(d, 0.05, 0.2, 1.0, 0.25)))


@sound("turbine_spindown", subtitle="Turbine winds down")
def turbine_spindown(v):
    return fade(turbine_spinup(v)[::-1].copy())


@sound("steam_hiss", variants=3, subtitle="Steam hisses")
def steam_hiss(v):
    d = 0.9 + 0.2 * v
    n = bandpass(white(d, 40 + v), 1800 + 400 * v, 10000)
    e = env_attack_exp(d, 0.02, 0.35 + 0.1 * v)
    return fade(normalize(n * e, 0.8))


@sound("geothermal_bubble", variants=3, subtitle="Magma bubbles")
def geothermal_bubble(v):
    d = 1.4
    glops = bubbles(d, 9, seed=50 + v, fmin=70, fmax=260, size=0.11)
    rumble = 0.4 * lowpass(brown(d, 55 + v), 120)
    sizzle = 0.15 * crackle(d, 40, seed=57 + v)
    return fade(normalize(reverb(mix(glops, rumble, sizzle), room=0.3, wet=0.2)))


@sound("supreme_eruption", subtitle="Tectonic Eruption!")
def supreme_eruption(v):
    d = 3.2
    t = t_axis(d)
    boom = lowpass(brown(d, 60), 180) * env_attack_exp(d, 0.01, 0.9) * 1.4
    sub = sine(38 * (1 - 0.3 * t / d), d) * env_attack_exp(d, 0.005, 0.7)
    roar = sweep_lowpass(white(d, 61), 5000, 600) * env_attack_exp(d, 0.08, 1.4) * 0.6
    debris = crackle(d, 120, seed=62) * env_exp(d, 1.2) * 0.5
    return fade(normalize(soft_clip(reverb(mix(boom, sub, roar, debris), room=0.8, wet=0.35), 2.0)))


@sound("supreme_vent", subtitle="Volcanic generator vents")
def supreme_vent(v):
    d = 2.5
    hiss = bandpass(white(d, 70), 900, 7000) * env_adsr(d, 0.15, 0.4, 0.6, 1.2)
    ping = 0.08 * metal_hit(d, 700, seed=71, decay=0.5) * env_exp(d, 0.4)
    tick = sum(place(np.zeros(int(d * SR)), metal_hit(0.15, 2600, seed=72 + i, decay=0.04) * 0.12, 0.6 + i * 0.45) for i in range(4))
    return fade(normalize(mix(hiss, ping, tick)))


# ---------- Magma Forge ----------

@sound("forge_strike", variants=4, subtitle="Hammer strikes")
def forge_strike(v):
    d = 1.1
    base = [196, 207, 185, 220][v]
    clang = metal_hit(d, base, seed=100 + v, decay=0.45, brightness=1.2)
    thud = 0.6 * lowpass(white(0.08, 110 + v), 300) * env_exp(0.08, 0.02)
    return fade(normalize(reverb(soft_clip(mix(clang, thud), 1.6), room=0.35, wet=0.22)))


@sound("forge_perfect_strike", variants=2, subtitle="Perfect strike!")
def forge_perfect_strike(v):
    d = 1.6
    clang = metal_hit(d, 233 + 10 * v, seed=120 + v, decay=0.55, brightness=1.6)
    chime = 0.25 * (sine(1568, d) + 0.6 * sine(2349, d)) * env_attack_exp(d, 0.005, 0.5)
    sizzle = 0.2 * bandpass(white(d, 125 + v), 3000, 9000) * env_attack_exp(d, 0.05, 0.3)
    return fade(normalize(reverb(mix(clang, chime, sizzle), room=0.45, wet=0.3)))


@sound("forge_complete", subtitle="Forging complete")
def forge_complete(v):
    d = 2.0
    thump = 0.9 * sine(np.linspace(110, 45, int(d * SR)), d) * env_exp(d, 0.18)
    quench = 0.45 * bandpass(white(d, 130), 1500, 8000) * env_attack_exp(d, 0.03, 0.6)
    shimmer = 0.18 * sum(sine(f, d) * env_attack_exp(d, 0.02 + i * 0.05, 0.7) for i, f in enumerate([523, 659, 784, 1047]))
    return fade(normalize(reverb(mix(thump, quench, shimmer), room=0.5, wet=0.3)))


@sound("forge_fail", subtitle="Forge fizzles")
def forge_fail(v):
    d = 0.7
    fizz = bandpass(white(d, 140), 600, 4000) * env_exp(d, 0.18)
    buzz = 0.3 * lowpass(square(np.linspace(140, 90, int(d * SR)), d), 900) * env_exp(d, 0.2)
    return fade(normalize(mix(fizz, buzz), 0.75))


@sound("forge_ambient", subtitle="Forge roars", loop=True)
def forge_ambient(v):
    d = 2.0
    t = t_axis(d)
    roar = lowpass(brown(d, 150), 500) * (0.75 + 0.25 * np.sin(2 * np.pi * 1.0 * t))
    flame = 0.35 * bandpass(white(d, 151), 300, 2200) * (0.8 + 0.2 * np.sin(2 * np.pi * 2.5 * t))
    pops = 0.3 * crackle(d, 25, seed=152)
    return loopable(normalize(mix(roar, flame, pops), 0.8))


# ---------- Multiblocks ----------

@sound("multiblock_form", subtitle="Multiblock assembled")
def multiblock_form(v):
    d = 2.4
    n = int(d * SR)
    sweep = 0.35 * sine(np.geomspace(80, 880, n), d) * env_adsr(d, 0.05, 0.3, 0.6, 1.2)
    clunks = np.zeros(n)
    for i in range(3):
        place(clunks, metal_hit(0.6, 110 + 30 * i, seed=200 + i, decay=0.15) * 0.6, 0.05 + i * 0.22)
    chord = 0.22 * sum(sine(f, d) * env_attack_exp(d, 0.6, 0.9) for f in [261.6, 329.6, 392.0, 523.3])
    return fade(normalize(reverb(mix(sweep, clunks, chord), room=0.6, wet=0.3)))


@sound("multiblock_break", subtitle="Multiblock disassembled")
def multiblock_break(v):
    d = 1.6
    n = int(d * SR)
    sweep = 0.4 * saw(np.geomspace(600, 60, n), d) * env_exp(d, 0.5)
    sweep = lowpass(sweep, 1500)
    clank = metal_hit(d, 98, seed=210, decay=0.3) * 0.7
    return fade(normalize(reverb(mix(sweep, clank), room=0.5, wet=0.25)))


# ---------- Energy ----------

@sound("energy_core_hum", subtitle="Energy Core thrums", loop=True)
def energy_core_hum(v):
    d = 2.0
    t = t_axis(d)
    mains = 0.45 * sine(60, d) + 0.25 * sine(120, d) + 0.12 * sine(180, d)
    buzz = 0.10 * lowpass(square(120, d, 0.3), 2500)
    pulse = 0.5 + 0.5 * np.sin(2 * np.pi * 1.0 * t)
    shimmer = 0.08 * sine(880 + 6 * np.sin(2 * np.pi * 2.0 * t), d) * pulse
    return loopable(normalize(mix(mains, buzz, shimmer), 0.7))


@sound("energy_charge", variants=2, subtitle="Energy surges")
def energy_charge(v):
    d = 0.6
    n = int(d * SR)
    zap = square(np.geomspace(200 + 100 * v, 2400, n), d, 0.25) * env_attack_exp(d, 0.01, 0.2)
    zap = bandpass(zap, 300, 6000)
    sparks = 0.4 * crackle(d, 160, seed=300 + v)
    return fade(normalize(mix(zap * 0.6, sparks), 0.75))


@sound("energy_discharge", subtitle="Energy discharges")
def energy_discharge(v):
    return fade(energy_charge(0)[::-1].copy())


# ---------- Processing machines ----------

@sound("grinder_run", subtitle="Grinder grinds", loop=True)
def grinder_run(v):
    d = 2.0
    t = t_axis(d)
    motor = 0.35 * lowpass(saw(90 + 2 * np.sin(2 * np.pi * 0.5 * t), d), 700)
    grit = bandpass(white(d, 400), 800, 5000) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 6 * t)))
    rocks = 0.5 * crackle(d, 90, seed=401)
    return loopable(normalize(mix(motor, grit * 0.5, rocks), 0.8))


@sound("furnace_roar", subtitle="Furnace burns", loop=True)
def furnace_roar(v):
    d = 2.0
    t = t_axis(d)
    roar = lowpass(brown(d, 500), 350) * (0.8 + 0.2 * np.sin(2 * np.pi * 1.5 * t))
    flicker = 0.3 * bandpass(white(d, 501), 400, 1800)
    pops = 0.25 * crackle(d, 15, seed=502)
    return loopable(normalize(mix(roar, flicker, pops), 0.7))


@sound("distillation_bubble", subtitle="Distillation chamber bubbles", loop=True)
def distillation_bubble(v):
    d = 2.0
    boil = bubbles(d, 28, seed=600, fmin=400, fmax=1400, size=0.035)
    big = 0.6 * bubbles(d, 4, seed=601, fmin=150, fmax=300, size=0.09)
    drip = sum(place(np.zeros(int(d * SR)), sine(np.linspace(1800, 2600, int(0.05 * SR)), 0.05) * env_exp(0.05, 0.012) * 0.4, at) for at in (0.4, 1.3))
    glass = 0.05 * sine(3150, d) * env_exp(d, 0.4)
    return loopable(normalize(reverb(mix(boil, big, drip, glass), room=0.25, wet=0.25), 0.75))


@sound("distillation_complete", subtitle="Distillate ready")
def distillation_complete(v):
    d = 1.4
    notes = [(0.0, 784), (0.12, 988), (0.24, 1319)]
    out = np.zeros(int(d * SR))
    for at, f in notes:
        place(out, (sine(f, 1.0) + 0.3 * sine(f * 2.01, 1.0)) * env_attack_exp(1.0, 0.004, 0.25) * 0.5, at)
    place(out, bubbles(0.5, 20, seed=610, fmin=500, fmax=1500, size=0.03) * 0.5, 0.0)
    return fade(normalize(reverb(out, room=0.4, wet=0.3)))


@sound("pump_run", subtitle="Pump churns", loop=True)
def pump_run(v):
    d = 2.0
    t = t_axis(d)
    stroke = (0.5 + 0.5 * np.sin(2 * np.pi * 2.0 * t)) ** 3
    piston = 0.6 * lowpass(brown(d, 700), 250) * stroke
    gurgle = 0.3 * bubbles(d, 10, seed=701, fmin=150, fmax=500, size=0.05)
    motor = 0.2 * lowpass(saw(75, d), 500)
    return loopable(normalize(mix(piston, gurgle, motor), 0.75))


# ---------- Small UI / interaction ----------

@sound("machine_click", variants=3, subtitle="Machine clicks")
def machine_click(v):
    d = 0.12
    return fade(normalize(metal_hit(d, 1800 + 300 * v, seed=800 + v, decay=0.02) + 0.5 * highpass(white(d, 810 + v), 3000) * env_exp(d, 0.004), 0.6), 0.0005, 0.01)


@sound("machine_toggle_on", subtitle="Machine powers on")
def machine_toggle_on(v):
    d = 0.5
    blip = sine(np.geomspace(300, 900, int(d * SR)), d) * env_attack_exp(d, 0.005, 0.12)
    return fade(normalize(mix(blip * 0.6, machine_click(0) * 0.8), 0.7))


@sound("machine_toggle_off", subtitle="Machine powers off")
def machine_toggle_off(v):
    d = 0.5
    blip = sine(np.geomspace(900, 250, int(d * SR)), d) * env_attack_exp(d, 0.005, 0.12)
    return fade(normalize(mix(blip * 0.6, machine_click(1) * 0.8), 0.7))


@sound("wrench_ratchet", variants=2, subtitle="Wrench ratchets")
def wrench_ratchet(v):
    d = 0.45
    out = np.zeros(int(d * SR))
    for i in range(5 + v):
        place(out, metal_hit(0.06, 2200 + 150 * (i % 2), seed=900 + i + 10 * v, decay=0.012), i * 0.065)
    return fade(normalize(out, 0.6))


@sound("pipe_connect", variants=2, subtitle="Pipe connects")
def pipe_connect(v):
    d = 0.35
    return fade(normalize(reverb(metal_hit(d, 620 + 90 * v, seed=950 + v, decay=0.08), room=0.15, wet=0.2), 0.6))


# =====================================================================
#  Export
# =====================================================================

def file_names(name):
    n = META[name]["variants"]
    return [name] if n == 1 else ["%s_%d" % (name, i + 1) for i in range(n)]


def render(name):
    out = []
    for i, fname in enumerate(file_names(name)):
        x = RECIPES[name](i)
        x = np.nan_to_num(np.asarray(x, dtype=np.float64))
        x = normalize(x, 0.89)
        out.append((fname, x.astype(np.float32)))
    return out


def write_sounds_json(names):
    data = {}
    if os.path.exists(SOUNDS_JSON):
        with open(SOUNDS_JSON, "r", encoding="utf-8") as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError:
                data = {}
    for name in names:
        entry = {
            "category": "block",
            "subtitle": "subtitles.%s.machine.%s" % (MOD_ID, name),
            "sounds": [
                {"name": "%s:machine/%s" % (MOD_ID, fn), "stream": False}
                for fn in file_names(name)
            ],
        }
        data["machine.%s" % name] = entry
    with open(SOUNDS_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def write_lang_subtitles(names):
    lang = os.path.join(ASSETS, "lang", "en_us.json")
    if not os.path.exists(lang):
        return
    with open(lang, "r", encoding="utf-8") as f:
        data = json.load(f)
    for name in names:
        data["subtitles.%s.machine.%s" % (MOD_ID, name)] = META[name]["subtitle"]
    with open(lang, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def write_java():
    names = sorted(RECIPES.keys())
    lines = []
    for n in names:
        lines.append("    public static final SoundEvent %s = register(\"machine.%s\");" % (n.upper(), n))
    java = """package net.valentin.%(mod)s.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.valentin.%(mod)s.ObsidiumMod;

/**
 * AUTO-GENERATED by tools/sfx/generate_machine_sounds.py -- do not edit by hand.
 * Original procedurally synthesized machine sounds (no vanilla Minecraft audio).
 */
public final class ModSounds {
%(fields)s

    private ModSounds() {}

    private static SoundEvent register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Call once from ObsidiumMod#onInitialize to force class loading / registration. */
    public static void init() {
        ObsidiumMod.LOGGER.info("Registered {} Obsidium machine sounds", %(count)d);
    }
}
""" % {"mod": MOD_ID, "fields": "\n".join(lines), "count": len(names)}
    os.makedirs(os.path.dirname(JAVA_OUT), exist_ok=True)
    with open(JAVA_OUT, "w", encoding="utf-8") as f:
        f.write(java)


def main():
    ap = argparse.ArgumentParser(description="Generate original Obsidium machine SFX")
    ap.add_argument("--only", nargs="*", help="generate only these sound names")
    ap.add_argument("--list", action="store_true", help="list available sounds and exit")
    ap.add_argument("--wav-preview", action="store_true", help="also write .wav copies to tools/sfx/preview/")
    ap.add_argument("--quality", type=float, default=0.6, help="OGG Vorbis quality 0..1 (default 0.6)")
    args = ap.parse_args()

    if args.list:
        for n in sorted(RECIPES):
            m = META[n]
            print("%-24s variants=%d loop=%-5s  %s" % (n, m["variants"], m["loop"], m["subtitle"]))
        return

    names = args.only if args.only else sorted(RECIPES)
    unknown = [n for n in names if n not in RECIPES]
    if unknown:
        sys.exit("Unknown sound(s): %s  (use --list)" % ", ".join(unknown))

    os.makedirs(SOUND_DIR, exist_ok=True)
    if args.wav_preview:
        os.makedirs(PREVIEW_DIR, exist_ok=True)

    total = 0
    for name in names:
        for fname, x in render(name):
            path = os.path.join(SOUND_DIR, fname + ".ogg")
            sf.write(path, x, SR, format="OGG", subtype="VORBIS", compression_level=1.0 - args.quality)
            if args.wav_preview:
                sf.write(os.path.join(PREVIEW_DIR, fname + ".wav"), x, SR, subtype="PCM_16")
            total += 1
            print("  ✔ %-28s %5.2fs" % (fname + ".ogg", len(x) / SR))

    write_sounds_json(names)
    write_lang_subtitles(names)
    write_java()
    print("\nGenerated %d files for %d sound events." % (total, len(names)))
    print("  sounds  -> %s" % os.path.relpath(SOUND_DIR, ROOT))
    print("  json    -> %s" % os.path.relpath(SOUNDS_JSON, ROOT))
    print("  java    -> %s" % os.path.relpath(JAVA_OUT, ROOT))


if __name__ == "__main__":
    main()
