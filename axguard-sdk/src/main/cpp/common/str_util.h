#pragma once

#include <stddef.h>

namespace axguard {

inline bool starts_with_ci(const char *s, const char *prefix) {
    for (size_t k = 0; prefix[k] != '\0'; ++k) {
        char c = s[k];
        if (c == '\0') return false;
        if (c >= 'A' && c <= 'Z') c = (char) (c - 'A' + 'a');
        if (c != prefix[k]) return false;
    }
    return true;
}

} // namespace axguard
