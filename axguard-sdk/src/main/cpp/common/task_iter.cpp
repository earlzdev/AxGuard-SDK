#include "task_iter.h"

#include <sys/syscall.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>

namespace axguard {

// Kernel-ABI directory entry (stable since 2.6.10); read via SYS_getdents64 so
// the task sweep never touches libc/Java IO a hook could intercept.
namespace {
struct axg_dirent64 {
    uint64_t d_ino;
    int64_t  d_off;
    uint16_t d_reclen;
    uint8_t  d_type;
    char     d_name[];
};
} // namespace

bool for_each_task(task_cb cb, void *ctx) {
    int fd = (int) syscall(SYS_openat, AT_FDCWD, "/proc/self/task",
                           O_RDONLY | O_DIRECTORY | O_CLOEXEC, 0);
    if (fd < 0) return false;

    char buf[8192];
    long nread;
    bool stop = false;
    while (!stop && (nread = syscall(SYS_getdents64, fd, buf, sizeof(buf))) > 0) {
        for (long bpos = 0; bpos < nread; ) {
            auto *d = reinterpret_cast<axg_dirent64 *>(buf + bpos);
            if (d->d_name[0] != '.') {
                if (cb(d->d_name, ctx)) { stop = true; break; }
            }
            bpos += d->d_reclen;
        }
    }
    syscall(SYS_close, fd);
    return true;
}

} // namespace axguard
