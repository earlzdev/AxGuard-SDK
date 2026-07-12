#!/usr/bin/env python3
"""Regenerate the obfuscated literals in obfs/ObfuscatedStrings.kt.

Keystream cipher shared byte-for-byte with Obfs.decode, cpp/obfs.h,
integrity_check.cpp, and Fingerprints.obfuscateToBase64. KEY must match
AxGuardBuild.OBFS_KEY.

Usage:  python3 tools/gen_obfs.py "string one" "string two" ...
        (prints a `bytes(...), // "…"` line per argument)
"""
import sys

KEY = 0x6D7A3F15  # keep in sync with AxGuardBuild.OBFS_KEY
M32 = 0xFFFFFFFF


def seed(key):
    s = (key ^ 0x9E3779B9) & M32
    s = (s * 0x85EBCA6B) & M32
    s ^= (s >> 13)
    s &= M32
    return s | 1


def encode(text):
    data = text.encode("utf-8")
    st = seed(KEY)
    prev = seed(KEY) & 0xFF
    out = []
    for i, p in enumerate(data):
        st ^= (st << 13) & M32; st &= M32
        st ^= (st >> 17)
        st ^= (st << 5) & M32; st &= M32
        k = (st ^ (st >> 8) ^ (st >> 16) ^ (st >> 24)) & 0xFF
        c = (p ^ k ^ (i & 0xFF) ^ prev) & 0xFF
        out.append(c)
        prev = c
    return out


def decode(data):  # inverse, for self-check
    st = seed(KEY)
    prev = seed(KEY) & 0xFF
    out = bytearray()
    for i, c in enumerate(data):
        st ^= (st << 13) & M32; st &= M32
        st ^= (st >> 17)
        st ^= (st << 5) & M32; st &= M32
        k = (st ^ (st >> 8) ^ (st >> 16) ^ (st >> 24)) & 0xFF
        out.append((c ^ k ^ (i & 0xFF) ^ prev) & 0xFF)
        prev = c
    return bytes(out)


def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


def main(args):
    if not args:
        print(__doc__)
        return 1
    for text in args:
        enc = encode(text)
        assert decode(enc).decode("utf-8") == text, text
        body = ", ".join("0x%02X" % b for b in enc)
        print('bytes(%s), // "%s"' % (body, esc(text)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
