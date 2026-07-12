// Native probe entry points, bound by RegisterNatives (jni_onload.cpp).
#pragma once

#include <jni.h>

namespace axg {

jboolean integrity_check_fp(JNIEnv *env, jobject thiz, jbyteArray expectedObfuscated,
                            jstring actualFingerprint);
jint root_probe(JNIEnv *env, jobject thiz);
jint hook_probe(JNIEnv *env, jobject thiz);
jint debugger_probe(JNIEnv *env, jobject thiz);
jint selinux_enforce(JNIEnv *env, jobject thiz);
jstring native_prop_get(JNIEnv *env, jobject thiz, jstring key);

} // namespace axg
