#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "../obfs.h"
#include "../common/syscall_io.h"

// Root detection. Returns a bitset: bit0 su binary at a known path, bit1 su on
// PATH, bit2 /system (or /) mounted rw, bit3 Magisk artifact, bit4 KernelSU node.
// test-keys / adb-root / insecure props stay in Kotlin.
static const char *su_path(int i) {
    switch (i) {
        case 0:  return OBFS("/system/app/Superuser.apk");
        case 1:  return OBFS("/sbin/su");
        case 2:  return OBFS("/system/bin/su");
        case 3:  return OBFS("/system/xbin/su");
        case 4:  return OBFS("/data/local/xbin/su");
        case 5:  return OBFS("/data/local/bin/su");
        case 6:  return OBFS("/system/sd/xbin/su");
        case 7:  return OBFS("/system/bin/failsafe/su");
        case 8:  return OBFS("/data/local/su");
        case 9:  return OBFS("/su/bin/su");
        case 10: return OBFS("/system/xbin/daemonsu");
        case 11: return OBFS("/sbin/.su");
        case 12: return OBFS("/data/adb/ksu/bin/ksud");
        default: return nullptr;
    }
}

static bool su_on_path() {
    const char *path = getenv("PATH");
    if (path == nullptr) return false;
    char buf[64];
    const char *start = path;
    for (const char *p = path;; ++p) {
        if (*p == ':' || *p == '\0') {
            size_t dir_len = (size_t) (p - start);
            // "<dir>/su" must fit; overlong PATH entries are skipped, not truncated.
            if (dir_len > 0 && dir_len + 4 < sizeof(buf)) {
                memcpy(buf, start, dir_len);
                buf[dir_len] = '/';
                buf[dir_len + 1] = 's';
                buf[dir_len + 2] = 'u';
                buf[dir_len + 3] = '\0';
                if (axguard::raw_exists(buf)) return true;
            }
            if (*p == '\0') break;
            start = p + 1;
        }
    }
    return false;
}

namespace {
struct mount_ctx {
    bool system_rw;
    bool magisk;
};
} // namespace

static bool mounts_line_cb(const char *line, void *c) {
    mount_ctx *ctx = (mount_ctx *) c;

    char tmp[1024];
    strncpy(tmp, line, sizeof(tmp) - 1);
    tmp[sizeof(tmp) - 1] = '\0';

    char *save = nullptr;
    char *device = strtok_r(tmp, " ", &save);
    char *mount = strtok_r(nullptr, " ", &save);
    strtok_r(nullptr, " ", &save);
    char *opts = strtok_r(nullptr, " ", &save);

    if (device == nullptr || mount == nullptr) return false;

    // Sources/mountpoints in /proc/mounts are kernel-controlled, so the magisk
    // substring here can't be planted by app-writable data.
    const char *magisk_marker = OBFS("magisk");
    if (strstr(device, magisk_marker) != nullptr || strstr(mount, magisk_marker) != nullptr) {
        ctx->magisk = true;
    }

    if (opts != nullptr && (strcmp(mount, "/system") == 0 || strcmp(mount, "/") == 0)) {
        char *osave = nullptr;
        for (char *tok = strtok_r(opts, ",", &osave); tok != nullptr;
             tok = strtok_r(nullptr, ",", &osave)) {
            if (strcmp(tok, "rw") == 0) {
                ctx->system_rw = true;
                break;
            }
        }
    }
    return false;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_axguard_sdk_internal_checks_RootCheck_nativeProbe(
        JNIEnv *, jobject) {
    int flags = 0;

    for (int i = 0; ; ++i) {
        const char *p = su_path(i);
        if (p == nullptr) break;
        if (axguard::raw_exists(p)) { flags |= 0x1; break; }
    }
    if (su_on_path()) flags |= 0x2;

    mount_ctx mctx = {false, false};
    axguard::scan_lines("/proc/mounts", mounts_line_cb, &mctx);
    if (mctx.system_rw) flags |= 0x4;

    if (mctx.magisk || axguard::raw_exists(OBFS("/data/adb/magisk/")) || axguard::raw_exists(OBFS("/data/adb/magisk.db"))) {
        flags |= 0x8;
    }
    if (axguard::raw_exists(OBFS("/proc/sys/kernel/ksu")) || axguard::raw_exists(OBFS("/sys/kernel/ksu"))) {
        flags |= 0x10;
    }
    return flags;
}
