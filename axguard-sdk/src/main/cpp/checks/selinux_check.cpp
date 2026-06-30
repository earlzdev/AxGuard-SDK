#include <jni.h>
#include "../common/syscall_io.h"

// SELinux enforcing mode via a direct read of the selinuxfs node.
// Returns 1 enforcing, 0 permissive, -1 not observable (caller falls back).
extern "C" JNIEXPORT jint JNICALL
Java_com_axguard_sdk_internal_checks_SelinuxCheck_nativeEnforce(
        JNIEnv *, jobject) {
    char buf[8];
    if (axguard::raw_read_file("/sys/fs/selinux/enforce", buf, sizeof(buf)) <= 0) return -1;
    if (buf[0] == '1') return 1;
    if (buf[0] == '0') return 0;
    return -1;
}
