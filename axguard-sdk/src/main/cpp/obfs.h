// XOR-obfuscated string literals for hostile-artifact names. Each OBFS("…")
// compiles the plaintext into .rodata already XOR'd, so it never appears in
// `strings` output of a shipped release. Raises the bar against static analysis
// only — a dynamic-instrumentation attacker still sees the decoded strings.
#pragma once

#include <cstddef>
#include <cstdint>

#ifndef AXGUARD_OBFS_KEY
#define AXGUARD_OBFS_KEY 0x5A
#endif

namespace axg {

template<std::size_t N>
struct ObfsString {
    uint8_t data[N];
    constexpr ObfsString(const char (&s)[N]) : data{} {
        for (std::size_t i = 0; i < N; ++i) {
            data[i] = static_cast<uint8_t>(s[i]) ^
                      static_cast<uint8_t>((AXGUARD_OBFS_KEY + i) & 0xFF);
        }
    }
    void decode(char *out) const {
        for (std::size_t i = 0; i < N; ++i) {
            out[i] = static_cast<char>(data[i] ^
                                       static_cast<uint8_t>((AXGUARD_OBFS_KEY + i) & 0xFF));
        }
    }
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
