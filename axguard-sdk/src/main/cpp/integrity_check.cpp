#include <jni.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>

#include "obfs.h"
#include "common/jni_targets.h"

// A SHA-256 fingerprint is 64 hex chars; leave headroom for sloppy input.
#define FINGERPRINT_BUF_SIZE 129

// Volatile write loop so the optimizer can't elide the zeroing (explicit_bzero
// is only in bionic from API 30).
static void secure_bzero(void *p, size_t n) {
    volatile uint8_t *v = static_cast<volatile uint8_t *>(p);
    for (size_t i = 0; i < n; ++i) v[i] = 0;
}

// Constant-time compare: once lengths match, the OR-accumulator avoids an
// early-return on first mismatch, closing the (weak, in-process) timing channel.
static bool ct_equal(const char *a, const char *b) {
    size_t la = strlen(a);
    size_t lb = strlen(b);
    if (la != lb) return false;
    uint8_t acc = 0;
    for (size_t i = 0; i < la; ++i) {
        acc |= static_cast<uint8_t>(a[i] ^ b[i]);
    }
    return acc == 0;
}

static bool normalize_fingerprint(const char* in, char* out, size_t out_size) {
    size_t j = 0;
    for (size_t i = 0; in[i] != '\0'; ++i) {
        char c = in[i];
        if (c == ':' || c == ' ' || c == '\t' || c == '\r' || c == '\n') continue;
        if (c >= 'A' && c <= 'Z') c = (char) (c - 'A' + 'a');
        if (j + 1 >= out_size) return false;
        out[j++] = c;
    }
    out[j] = '\0';
    return true;
}

// Decode, normalize, and constant-time compare stay native so the plaintext
// fingerprint never reaches Java. Bound via RegisterNatives (jni_onload.cpp).
jboolean axg::integrity_check_fp(
        JNIEnv* env, jobject /* this */, jbyteArray expectedObfuscated, jstring actualFingerprint) {

    jsize n = env->GetArrayLength(expectedObfuscated);
    // Reject empty or oversized input: it can't be a SHA-256 fingerprint, and the
    // bound keeps the decode within expected_plain.
    if (n <= 0 || n >= FINGERPRINT_BUF_SIZE) return JNI_FALSE;

    jbyte* obf = env->GetByteArrayElements(expectedObfuscated, nullptr);
    if (obf == nullptr) return JNI_FALSE;

    char expected_plain[FINGERPRINT_BUF_SIZE];
    axg::obfs_decode(reinterpret_cast<const uint8_t*>(obf), expected_plain, static_cast<size_t>(n));
    expected_plain[n] = '\0';
    env->ReleaseByteArrayElements(expectedObfuscated, obf, JNI_ABORT);

    const char* actual = env->GetStringUTFChars(actualFingerprint, nullptr);
    if (actual == nullptr) {
        secure_bzero(expected_plain, sizeof(expected_plain));
        return JNI_FALSE;
    }

    char expected_norm[FINGERPRINT_BUF_SIZE];
    char actual_norm[FINGERPRINT_BUF_SIZE];
    bool match = normalize_fingerprint(expected_plain, expected_norm, sizeof(expected_norm))
            && normalize_fingerprint(actual, actual_norm, sizeof(actual_norm))
            && ct_equal(expected_norm, actual_norm);

    env->ReleaseStringUTFChars(actualFingerprint, actual);

    // Zeroize every buffer that transiently held the plaintext fingerprint.
    secure_bzero(expected_plain, sizeof(expected_plain));
    secure_bzero(expected_norm, sizeof(expected_norm));
    secure_bzero(actual_norm, sizeof(actual_norm));

    // Intentionally silent on mismatch: a log line would let a repackaged app
    // detect via logcat that its tampering was caught.
    return match ? JNI_TRUE : JNI_FALSE;
}
