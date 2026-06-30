#include "syscall_io.h"

#include <sys/syscall.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <poll.h>
#include <errno.h>
#include <string.h>
#include <stdint.h>
#include <time.h>

namespace axguard {

int raw_open(const char *path) {
    return (int) syscall(SYS_openat, AT_FDCWD, path, O_RDONLY | O_CLOEXEC, 0);
}

bool raw_exists(const char *path) {
    return syscall(SYS_faccessat, AT_FDCWD, path, F_OK) == 0;
}

ssize_t raw_read_file(const char *path, char *buf, size_t cap) {
    if (cap == 0) return -1;
    int fd = raw_open(path);
    if (fd < 0) return -1;
    ssize_t total = 0;
    while ((size_t) total < cap - 1) {
        ssize_t n = syscall(SYS_read, fd, buf + total, cap - 1 - total);
        if (n <= 0) break;
        total += n;
    }
    syscall(SYS_close, fd);
    buf[total] = '\0';
    return total;
}

bool scan_lines(const char *path, line_cb cb, void *ctx) {
    int fd = raw_open(path);
    if (fd < 0) return false;

    char chunk[4096];
    char line[4096 + 256];
    size_t line_len = 0;
    bool overflowed = false;
    bool stop = false;
    ssize_t n;
    while (!stop && (n = syscall(SYS_read, fd, chunk, sizeof(chunk))) > 0) {
        for (ssize_t i = 0; i < n; ++i) {
            char c = chunk[i];
            if (c == '\n') {
                if (!overflowed) {
                    line[line_len] = '\0';
                    if (cb(line, ctx)) { stop = true; break; }
                }
                line_len = 0;
                overflowed = false;
            } else if (!overflowed) {
                if (line_len < sizeof(line) - 1) {
                    line[line_len++] = c;
                } else {
                    overflowed = true;
                }
            }
        }
    }
    if (!stop && line_len > 0 && !overflowed) {
        line[line_len] = '\0';
        cb(line, ctx);
    }
    syscall(SYS_close, fd);
    return true;
}

// Socket path via direct syscalls: the libc PLT path is exactly what a runtime
// hooking framework intercepts; the kernel path a GOT/PLT hook can't reach.
static int sys_socket(int domain, int type, int protocol) {
    return (int) syscall(SYS_socket, domain, type, protocol);
}
static int sys_connect(int fd, const sockaddr *addr, socklen_t alen) {
    return (int) syscall(SYS_connect, fd, addr, alen);
}
static int sys_getsockopt(int fd, int level, int optname, void *optval, socklen_t *optlen) {
    return (int) syscall(SYS_getsockopt, fd, level, optname, optval, optlen);
}
static int sys_close(int fd) {
    return (int) syscall(SYS_close, fd);
}
// arm64 has no SYS_poll; SYS_ppoll exists on every Android ABI.
static int sys_ppoll_ms(pollfd *fds, nfds_t n, int timeout_ms) {
    timespec ts;
    ts.tv_sec = timeout_ms / 1000;
    ts.tv_nsec = (long) (timeout_ms % 1000) * 1000000L;
    return (int) syscall(SYS_ppoll, fds, n, &ts, nullptr, 0);
}

bool port_open(int port, int timeout_ms) {
    int fd = sys_socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (fd < 0) return false;

    sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons((uint16_t) port);
    // Constant instead of inet_pton("127.0.0.1"): drops the libc call and the
    // "127.0.0.1" string from .rodata.
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    bool open = false;
    int r = sys_connect(fd, (sockaddr *) &addr, sizeof(addr));
    if (r == 0) {
        open = true;
    } else if (errno == EINPROGRESS) {
        pollfd pfd;
        pfd.fd = fd;
        pfd.events = POLLOUT;
        pfd.revents = 0;
        if (sys_ppoll_ms(&pfd, 1, timeout_ms) > 0 && (pfd.revents & POLLOUT)) {
            int err = 0;
            socklen_t len = sizeof(err);
            if (sys_getsockopt(fd, SOL_SOCKET, SO_ERROR, &err, &len) == 0 && err == 0) {
                open = true;
            }
        }
    }
    sys_close(fd);
    return open;
}

} // namespace axguard
