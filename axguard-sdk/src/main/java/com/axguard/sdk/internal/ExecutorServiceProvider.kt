package com.axguard.sdk.internal

import com.axguard.sdk.internal.utils.availableProcessors
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared worker pool for the checks. Bounded to availableProcessors*2 so
 * concurrent callers can't explode the thread count; unbounded queue because
 * invokeAll submits every task up front. allowCoreThreadTimeOut reaps idle
 * workers after 60 s, so the SDK carries no live-thread cost between runs.
 * Daemon threads so a check stuck in an uninterruptible native call can't keep
 * the process alive.
 */
internal object ExecutorServiceProvider {

    val executor: ExecutorService by lazy {
        val poolSize = (availableProcessors * 2).coerceAtLeast(2)
        ThreadPoolExecutor(
            /* corePoolSize = */ poolSize,
            /* maximumPoolSize = */ poolSize,
            /* keepAliveTime = */ 60L,
            /* unit = */ TimeUnit.SECONDS,
            /* workQueue = */ LinkedBlockingQueue(),
            /* threadFactory = */ DaemonThreadFactory(),
        ).apply {
            allowCoreThreadTimeOut(true)
        }
    }

    private class DaemonThreadFactory : ThreadFactory {
        private val counter = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            val t = Thread(
                /* target = */ r,
                /* name = */ "axguard-check-${counter.incrementAndGet()}",
            )
            t.isDaemon = true

            return t
        }
    }
}
