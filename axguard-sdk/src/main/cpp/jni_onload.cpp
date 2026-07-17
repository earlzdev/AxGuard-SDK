// Binds hidden probe functions at load time so JNI_OnLoad is the sole exported symbol.
#include <jni.h>

#include "common/jni_targets.h"

namespace {

// Signatures must match the Kotlin `external fun` declarations.
const JNINativeMethod kAppIntegrity[] = {
    {"nativeCheckFingerprint", "([BLjava/lang/String;)Z",
     reinterpret_cast<void *>(&axg::integrity_check_fp)},
};
const JNINativeMethod kDexIntegrity[] = {
    {"nativeCheckHash", "([BLjava/lang/String;)Z",
     reinterpret_cast<void *>(&axg::integrity_check_fp)},
};
const JNINativeMethod kRoot[] = {
    {"nativeProbe", "()I", reinterpret_cast<void *>(&axg::root_probe)},
};
const JNINativeMethod kHook[] = {
    {"nativeProbe", "()I", reinterpret_cast<void *>(&axg::hook_probe)},
};
const JNINativeMethod kDebugger[] = {
    {"nativeProbe", "()I", reinterpret_cast<void *>(&axg::debugger_probe)},
};
const JNINativeMethod kSelinux[] = {
    {"nativeEnforce", "()I", reinterpret_cast<void *>(&axg::selinux_enforce)},
};
const JNINativeMethod kNativeProps[] = {
    {"nativeGet", "(Ljava/lang/String;)Ljava/lang/String;",
     reinterpret_cast<void *>(&axg::native_prop_get)},
};

// Missing class or failed registration is skipped, not fatal: only that one
// check loses its probe instead of the whole library failing to load.
void register_class(JNIEnv *env, const char *name, const JNINativeMethod *methods, jint count) {
    jclass clazz = env->FindClass(name);
    if (clazz == nullptr) {
        env->ExceptionClear();
        return;
    }
    env->RegisterNatives(clazz, methods, count);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
    env->DeleteLocalRef(clazz);
}

} // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    register_class(env, "com/axguard/sdk/internal/checks/AppIntegrityCheck", kAppIntegrity, 1);
    register_class(env, "com/axguard/sdk/internal/checks/DexIntegrityCheck", kDexIntegrity, 1);
    register_class(env, "com/axguard/sdk/internal/checks/RootCheck", kRoot, 1);
    register_class(env, "com/axguard/sdk/internal/checks/HookCheck", kHook, 1);
    register_class(env, "com/axguard/sdk/internal/checks/DebuggerCheck", kDebugger, 1);
    register_class(env, "com/axguard/sdk/internal/checks/SelinuxCheck", kSelinux, 1);
    register_class(env, "com/axguard/sdk/internal/utils/NativeProps", kNativeProps, 1);

    return JNI_VERSION_1_6;
}
