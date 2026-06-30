package com.axguard.sdk.internal

import android.content.Context
import com.axguard.sdk.api.AxGuardSdk
import com.axguard.sdk.api.models.AxGuardReport
import com.axguard.sdk.api.models.SecurityCheckConfig
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal object AxGuardSdkImpl : AxGuardSdk {

    @Throws(InterruptedException::class)
    override fun runChecks(
        config: SecurityCheckConfig,
    ): AxGuardReport {
        val context: Context = config.context.applicationContext
        val factory = SecurityChecksFactory()

        val securityChecks: List<SecurityCheck> = factory.createChecks(
            context = context,
            checkIds = config.checkIds,
        )

        return AxGuardReport(
            results = executeAll(
                securityChecks = securityChecks,
                timeoutMs = config.timeoutMs,
            ).sortedBy { it.checkId },
        )
    }

    internal fun executeAll(
        securityChecks: List<SecurityCheck>,
        timeoutMs: Long,
    ): List<SecurityCheckResult> {
        val tasks: List<Callable<SecurityCheckResult>> = securityChecks.map { check ->
            Callable { runSafely(check) }
        }

        // invokeAll blocks until every task finishes or the timeout expires, then
        // cancels anything still running — one deadline for the whole report.
        val futures: List<Future<SecurityCheckResult>> =
            ExecutorServiceProvider.executor.invokeAll(tasks, timeoutMs, TimeUnit.MILLISECONDS)

        return futures.zip(securityChecks) { future, check ->
            resolve(check, future)
        }
    }

    // Any uncaught throwable from a check becomes Unavailable rather than failing
    // the whole report.
    private fun runSafely(check: SecurityCheck): SecurityCheckResult {
        return try {
            check.run()
        } catch (t: Throwable) {
            UnavailableImpl(
                checkId = check.id,
                reason = ErrorReasonImpl(t.toErrorKind()),
            )
        }
    }

    private fun resolve(
        check: SecurityCheck,
        future: Future<SecurityCheckResult>,
    ): SecurityCheckResult {
        if (future.isCancelled) {
            return UnavailableImpl(
                checkId = check.id,
                reason = SecurityCheckResult.Unavailable.Reason.Timeout,
            )
        }
        return runCatching { future.get() }.getOrElse { e: Throwable ->
            val cause: Throwable = (e as? ExecutionException)?.cause ?: e
            UnavailableImpl(
                checkId = check.id,
                reason = ErrorReasonImpl(cause.toErrorKind()),
            )
        }
    }
}
