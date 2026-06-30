#pragma once

namespace axguard {

typedef bool (*task_cb)(const char *tid, void *ctx);

bool for_each_task(task_cb cb, void *ctx);

} // namespace axguard
