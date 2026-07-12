// Keystream obfuscation: OBFS("…") enciphers string literals into .rodata so they
// never show in `strings`. Must stay byte-identical to Obfs.kt and gen_obfs.py.
#pragma once

#include <cstddef>
#include <cstdint>

#ifndef AXGUARD_OBFS_KEY
#define AXGUARD_OBFS_KEY 0x6D7A3F15
#endif

namespace axg {

constexpr uint32_t obfs_seed(uint32_t key) {
    uint32_t s = key ^ 0x9E3779B9u;
    s *= 0x85EBCA6Bu;
    s ^= s >> 13;
    return s | 1u;
}

constexpr uint32_t obfs_step(uint32_t state) {
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    return state;
}

constexpr uint8_t obfs_fold(uint32_t x) {
    return static_cast<uint8_t>((x ^ (x >> 8) ^ (x >> 16) ^ (x >> 24)) & 0xFF);
}

inline void obfs_decode(const uint8_t *in, char *out, std::size_t n) {
    uint32_t seed = obfs_seed(static_cast<uint32_t>(AXGUARD_OBFS_KEY));
    uint32_t state = seed;
    uint8_t prev = static_cast<uint8_t>(seed & 0xFF);
    for (std::size_t i = 0; i < n; ++i) {
        state = obfs_step(state);
        uint8_t k = obfs_fold(state);
        uint8_t c = static_cast<uint8_t>(in[i]);
        out[i] = static_cast<char>(c ^ k ^ static_cast<uint8_t>(i & 0xFF) ^ prev);
        prev = c;
    }
}

template<std::size_t N>
struct ObfsString {
    uint8_t data[N];
    constexpr ObfsString(const char (&s)[N]) : data{} {
        uint32_t seed = obfs_seed(static_cast<uint32_t>(AXGUARD_OBFS_KEY));
        uint32_t state = seed;
        uint8_t prev = static_cast<uint8_t>(seed & 0xFF);
        for (std::size_t i = 0; i < N; ++i) {
            state = obfs_step(state);
            uint8_t k = obfs_fold(state);
            uint8_t c = static_cast<uint8_t>(
                    static_cast<uint8_t>(s[i]) ^ k ^ static_cast<uint8_t>(i & 0xFF) ^ prev);
            data[i] = c;
            prev = c;
        }
    }
    void decode(char *out) const { obfs_decode(data, out, N); }
};

} // namespace axg

// Decodes into a per-call-site thread_local scratch buffer. Each OBFS(...)
// expansion is a unique lambda type, so distinct call sites never share a buffer.
#define OBFS(lit) \
    ([]() -> const char* { \
        constexpr ::axg::ObfsString<sizeof(lit)> _o(lit); \
        static thread_local char _buf[sizeof(lit)]; \
        _o.decode(_buf); \
        return _buf; \
    }())
