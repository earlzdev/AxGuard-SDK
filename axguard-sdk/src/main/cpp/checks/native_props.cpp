#include <jni.h>
#include <sys/system_properties.h>

// __system_property_get is the real accessor libc exposes: no reflection, no
// Binder round-trip, and harder to intercept than the Java SystemProperties path.
extern "C" JNIEXPORT jstring JNICALL
Java_com_axguard_sdk_internal_utils_NativeProps_nativeGet(
        JNIEnv *env, jobject, jstring j_key) {
    const char *key = env->GetStringUTFChars(j_key, nullptr);
    if (key == nullptr) return nullptr;

    char value[PROP_VALUE_MAX];
    int len = __system_property_get(key, value);
    env->ReleaseStringUTFChars(j_key, key);

    if (len <= 0) return nullptr;
    return env->NewStringUTF(value);
}
