#include <jni.h>
#include <stdio.h>
#include <string.h>

#include "../obfs.h"
#include "../common/syscall_io.h"
#include "../common/task_iter.h"
#include "../common/str_util.h"

// Hook framework detection. Returns a bitset: bit0 suspicious library basename in
// /proc/self/maps, bit1 a frida worker thread, bit2 a standard frida port open,
// bit3 a frida server on disk. The Xposed-bridge probe stays in Kotlin (it needs
// the app classloader) and is OR-ed in there.
//
// Prefixes matched against the basename only, so libspine.so / "exposed" don't false-hit.
static const char *sus_prefix(int i) {
    switch (i) {
        case 0:  return OBFS("frida-agent");
        case 1:  return OBFS("frida-gadget");
        case 2:  return OBFS("libfrida");
        case 3:  return OBFS("libgadget");
        case 4:  return OBFS("libxposed");
        case 5:  return OBFS("xposedbridge");
        case 6:  return OBFS("liblspd");
        case 7:  return OBFS("lspd");
        case 8:  return OBFS("libriru");
        case 9:  return OBFS("libzygisk");
        case 10: return OBFS("zygisk");
        case 11: return OBFS("libsandhook");
        case 12: return OBFS("sandhook");
        case 13: return OBFS("libyahfa");
        case 14: return OBFS("yahfa");
        case 15: return OBFS("libpine");
        default: return nullptr;
    }
}

static bool maps_line_cb(const char *line, void *ctx) {
    const char *slash = strrchr(line, '/');
    if (slash == nullptr) return false;
    const char *base = slash + 1;
    for (int i = 0; ; ++i) {
        const char *prefix = sus_prefix(i);
        if (prefix == nullptr) break;
        if (axguard::starts_with_ci(base, prefix)) {
            *(bool *) ctx = true;
            return true;
        }
    }
    return false;
}

static bool frida_task_cb(const char *tid, void *ctx_) {
    char path[320];
    char comm[64];
    snprintf(path, sizeof(path), "/proc/self/task/%s/comm", tid);
    if (axguard::raw_read_file(path, comm, sizeof(comm)) <= 0) return false;
    size_t len = strlen(comm);
    while (len > 0 && (comm[len - 1] == '\n' || comm[len - 1] == '\r')) comm[--len] = '\0';
    // "gmain" is deliberately not matched: GLib apps have a legit thread there.
    if (strcmp(comm, OBFS("gum-js-loop")) == 0 ||
        strcmp(comm, OBFS("pool-frida")) == 0 ||
        strncmp(comm, OBFS("frida"), 5) == 0) {
        *(bool *) ctx_ = true;
        return true;
    }
    return false;
}

static bool frida_thread_present() {
    bool found = false;
    axguard::for_each_task(frida_task_cb, &found);
    return found;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_axguard_sdk_internal_checks_HookCheck_nativeProbe(
        JNIEnv *, jobject) {
    int flags = 0;

    bool suspicious = false;
    axguard::scan_lines("/proc/self/maps", maps_line_cb, &suspicious);
    if (suspicious) flags |= 0x1;

    if (frida_thread_present()) flags |= 0x2;

    if (axguard::port_open(27042, 200) || axguard::port_open(27043, 200)) flags |= 0x4;

    // Kept for older/permissive devices; SELinux denies stat here on most.
    if (axguard::raw_exists(OBFS("/data/local/tmp/frida-server")) ||
        axguard::raw_exists(OBFS("/data/local/tmp/re.frida.server"))) {
        flags |= 0x8;
    }
    return flags;
}
