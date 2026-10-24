"""Synthesized sounds for CyberFarm, drawn in code like the textures.

Needs numpy and soundfile (only to write .ogg files). Run from the project root:
    uv run --with numpy --with soundfile python tools/soundgen.py
"""
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parent.parent
SOUNDS = ROOT / "src/main/resources/assets/cyberfarm/sounds"
RATE = 22050


def drone_fly(length=2.0, seed=7):
    """The flight of a quadcopter: four rotors a few Hz apart, whose beating gives the wobbling
    whine of a real drone, over the rush of air through the blades.

    Every part repeats a whole number of times in `length` (frequencies are multiples of
    1 / length), so the sound loops with no seam.
    """
    rng = np.random.default_rng(seed)
    count = int(RATE * length)
    t = np.arange(count) / RATE

    whine = np.zeros(count)
    for base in (252.0, 258.5, 265.0, 271.5):
        for harmonic in range(1, 9):
            whine += np.sin(2 * np.pi * base * harmonic * t + rng.uniform(0, 2 * np.pi)) / harmonic ** 0.9
    whine /= np.abs(whine).max()

    # Air: noise between 300 Hz and 5 kHz, softer as it gets higher, built straight in the
    # frequency domain so it is periodic too
    freqs = np.fft.rfftfreq(count, 1 / RATE)
    band = (freqs > 300) & (freqs < 5000)
    spectrum = np.zeros(len(freqs), dtype=complex)
    spectrum[band] = (rng.normal(size=band.sum()) + 1j * rng.normal(size=band.sum())) / np.sqrt(freqs[band] / 300)
    air = np.fft.irfft(spectrum, count)
    air /= np.abs(air).max()

    sound = 0.75 * whine + 0.35 * air
    # A slight throb as the rotors keep the drone balanced
    sound *= 1.0 + 0.08 * np.sin(2 * np.pi * 4.0 * t)
    return 0.9 * sound / np.abs(sound).max()


def periodic_noise(count, low, high, rng, tilt=0.5):
    """Noise between `low` and `high` Hz, softer as it gets higher, built in the frequency domain so it
    repeats exactly every `count` samples (a seamless loop)."""
    freqs = np.fft.rfftfreq(count, 1 / RATE)
    band = (freqs > low) & (freqs < high)
    spectrum = np.zeros(len(freqs), dtype=complex)
    spectrum[band] = (rng.normal(size=band.sum()) + 1j * rng.normal(size=band.sum())) / (freqs[band] / low) ** tilt
    noise = np.fft.irfft(spectrum, count)
    return noise / np.abs(noise).max()


def envelope(count, attack, release):
    """Fades in over `attack` seconds and out over the last `release` seconds."""
    env = np.ones(count)
    a, r = int(RATE * attack), int(RATE * release)
    env[:a] = np.linspace(0, 1, a)
    env[count - r:] *= np.linspace(1, 0, r)
    return env


def sweep(start, end, count, harmonics, rng):
    """A tone gliding from `start` to `end` Hz, with `harmonics` overtones (a buzzy motor)."""
    freq = np.geomspace(start, end, count)
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    tone = sum(np.sin(h * phase + rng.uniform(0, 2 * np.pi)) / h for h in range(1, harmonics + 1))
    return tone / np.abs(tone).max()


def mech_servo(length=0.9, seed=11):
    """A heavy servo: a motor whining up to speed and settling, with the ripple of its gears."""
    rng = np.random.default_rng(seed)
    count = int(RATE * length)
    t = np.arange(count) / RATE
    rise = np.concatenate([np.geomspace(110, 260, count // 3), np.full(count - count // 3, 260.0)])
    rise *= 1.0 - 0.15 * np.clip((t - length * 0.75) / (length * 0.25), 0, 1)
    phase = 2 * np.pi * np.cumsum(rise) / RATE
    motor = sum(np.sin(h * phase) / h ** 0.8 for h in range(1, 10))
    motor /= np.abs(motor).max()
    gears = 1.0 + 0.35 * np.sign(np.sin(2 * np.pi * 38 * t))
    hiss = periodic_noise(count, 800, 6000, rng)
    sound = (0.8 * motor * gears + 0.12 * hiss) * envelope(count, 0.04, 0.18)
    return 0.9 * sound / np.abs(sound).max()


def mech_jet(length=2.0, seed=13):
    """The jet's thrust, a seamless loop: the roar of the exhaust over a deep rumble and the
    turbine's whine. Every part is periodic in `length`."""
    rng = np.random.default_rng(seed)
    count = int(RATE * length)
    t = np.arange(count) / RATE
    roar = periodic_noise(count, 60, 4000, rng, tilt=0.7)
    rumble = sum(np.sin(2 * np.pi * 47.0 * h * t + rng.uniform(0, 2 * np.pi)) / h for h in range(1, 5))
    rumble /= np.abs(rumble).max()
    whine = np.sin(2 * np.pi * 1180.0 * t) + 0.5 * np.sin(2 * np.pi * 1770.5 * t)
    sound = 0.8 * roar + 0.35 * rumble + 0.06 * whine
    sound *= 1.0 + 0.1 * np.sin(2 * np.pi * 6.0 * t)
    return 0.9 * sound / np.abs(sound).max()


def mech_drill(length=1.0, seed=17):
    """The drill spinning, a seamless loop: a motor hum and the grinding chatter of the bit."""
    rng = np.random.default_rng(seed)
    count = int(RATE * length)
    t = np.arange(count) / RATE
    motor = sum(np.sin(2 * np.pi * 96.0 * h * t + rng.uniform(0, 2 * np.pi)) / h ** 0.7 for h in range(1, 14))
    motor /= np.abs(motor).max()
    grind = periodic_noise(count, 1200, 7000, rng, tilt=0.3) * (0.6 + 0.4 * np.sin(2 * np.pi * 32.0 * t))
    sound = 0.7 * motor + 0.35 * grind
    return 0.85 * sound / np.abs(sound).max()


def mech_blade(on, length=0.7, seed=19):
    """The energy blade: a hum gliding up as it comes out, or down and away as it goes back."""
    rng = np.random.default_rng(seed)
    count = int(RATE * length)
    start, end = (70, 210) if on else (210, 55)
    hum = sweep(start, end, count, 6, rng)
    crackle = periodic_noise(count, 2000, 9000, rng) * (rng.random(count) < 0.04)
    env = envelope(count, 0.03, 0.25 if on else 0.5)
    if not on:
        env *= np.linspace(1, 0.3, count)
    sound = (hum + 0.6 * crackle) * env
    return 0.85 * sound / np.abs(sound).max()


def mech_boot(seed=23):
    """The cockpit waking up: four blips climbing, then a soft chord."""
    count_blip = int(RATE * 0.11)
    parts = []
    for freq in (523.25, 659.25, 783.99, 1046.5):
        t = np.arange(count_blip) / RATE
        blip = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(4 * np.pi * freq * t)
        parts.append(blip * envelope(count_blip, 0.005, 0.05))
        parts.append(np.zeros(int(RATE * 0.03)))
    count = int(RATE * 0.7)
    t = np.arange(count) / RATE
    chord = sum(np.sin(2 * np.pi * f * t) for f in (523.25, 783.99, 1046.5)) / 3
    parts.append(chord * envelope(count, 0.01, 0.6) * 0.8)
    sound = np.concatenate(parts)
    return 0.6 * sound / np.abs(sound).max()


def write(name, samples):
    SOUNDS.mkdir(parents=True, exist_ok=True)
    sf.write(SOUNDS / f"{name}.ogg", samples.astype(np.float32), RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    write("drone_fly", drone_fly())
    write("mech_servo", mech_servo())
    write("mech_jet", mech_jet())
    write("mech_drill", mech_drill())
    write("mech_blade_on", mech_blade(True))
    write("mech_blade_off", mech_blade(False))
    write("mech_boot", mech_boot())
    print("sounds written")
