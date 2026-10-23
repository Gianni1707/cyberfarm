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


def write(name, samples):
    SOUNDS.mkdir(parents=True, exist_ok=True)
    sf.write(SOUNDS / f"{name}.ogg", samples.astype(np.float32), RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    write("drone_fly", drone_fly())
    print("sounds written")
