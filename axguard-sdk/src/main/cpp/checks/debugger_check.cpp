#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../common/syscall_io.h"
#include "../common/task_iter.h"

// Debugger: per-thread TracerPid and tracing-stop scan. Returns a bitset, or -1
// if /proc/self/task couldn't be listed. ptrace is per-thread, so we sweep every
// tid rather than trusting the process-level entry.
static bool debugger_task_cb(const char *tid, void *ctx_) {
    int *flags = (int *) ctx_;
    char path[320];
    char buf[4096];

    if ((*flags & 0x1) == 0) {
        snprintf(path, sizeof(path), "/proc/self/task/%s/status", tid);
        if (axguard::raw_read_file(path, buf, sizeof(buf)) > 0) {
            const char *p = strstr(buf, "TracerPid:");
            if (p != nullptr && atoi(p + 10) != 0) *flags |= 0x1;
        }
    }
    if ((*flags & 0x2) == 0) {
        snprintf(path, sizeof(path), "/proc/self/task/%s/stat", tid);
        if (axguard::raw_read_file(path, buf, sizeof(buf)) > 0) {
            // State char is the first non-space after the LAST ')' (comm may hold
            // ')'). Only 't' is tracing stop; 'T' is a plain SIGSTOP, not a tracer.
            const char *p = strrchr(buf, ')');
            if (p != nullptr) {
                ++p;
                while (*p == ' ') ++p;
                if (*p == 't') *flags |= 0x2;
            }
        }
    }
    return (*flags & 0x3) == 0x3;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_axguard_sdk_internal_checks_DebuggerCheck_nativeProbe(
        JNIEnv *, jobject) {
    int flags = 0;
    if (!axguard::for_each_task(debugger_task_cb, &flags)) return -1;
    return flags;
}
