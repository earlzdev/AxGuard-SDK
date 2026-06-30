#pragma once

#include <sys/types.h>
#include <stddef.h>

namespace axguard {

int raw_open(const char *path);

bool raw_exists(const char *path);

ssize_t raw_read_file(const char *path, char *buf, size_t cap);

typedef bool (*line_cb)(const char *line, void *ctx);
bool scan_lines(const char *path, line_cb cb, void *ctx);

bool port_open(int port, int timeout_ms);

} // namespace axguard
