package com.axguard.sdk.internal.checks

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.NativeLibrary
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.TaskDirUnobservableException
import com.axguard.sdk.internal.models.checks.DebuggerThreatImpl
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind
import java.io.File

internal class DebuggerCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.DEBUGGER

    override fun run(): SecurityCheckResult {
        val jdwpConnected = Debug.isDebuggerConnected()
        val waitingForDebugger = Debug.waitingForDebugger()
        val appDebuggable =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        // Native per-thread scan first: reads TracerPid and thread state via direct
        // syscalls, so a hook on the Java IO the fallback uses can't filter it.
        // -1 means /proc/self/task couldn't be listed.
        val nativeBits: Int? = if (NativeLibrary.loaded) {
            runCatching { nativeProbe() }.getOrNull()?.takeIf { it >= 0 }
        } else {
            null
        }

        val fallbackScan: Result<Pair<Boolean, Boolean>>? = if (nativeBits == null) {
            runCatching {
                // ptrace attaches per thread, so a process-level TracerPid read
                // misses a tracer on a single worker thread. Sweep every task.
                val tasks = File("/proc/self/task").listFiles().orEmpty()
                // Empty listing (SELinux denial, missing procfs) isn't an error —
                // the probe just isn't observable; route to Unsupported, not Error.
                if (tasks.isEmpty()) throw TaskDirUnobservableException()
                var attached = false
                var traced = false
                for (task in tasks) {
                    if (!attached && tracerPidOf(task) != 0) attached = true
                    if (!traced && isInTracingStop(task)) traced = true
                    if (attached && traced) break
                }
                attached to traced
            }
        } else {
            null
        }

        val nativeAttached = (nativeBits?.and(FLAG_NATIVE_ATTACHED) ?: 0) != 0 ||
            fallbackScan?.getOrNull()?.first == true
        val anyThreadTraced = (nativeBits?.and(FLAG_THREAD_TRACED) ?: 0) != 0 ||
            fallbackScan?.getOrNull()?.second == true

        val evidenceSucceeded = nativeBits != null || fallbackScan?.isSuccess == true
        val evidenceError = fallbackScan?.exceptionOrNull()
        val evidenceUnsupported = evidenceError === TaskDirUnobservableException()

        return when {
            // appDebuggable is a build-time manifest flag, not a runtime attack
            // signal, so it never fires the threat alone — only corroborates.
            jdwpConnected || waitingForDebugger || nativeAttached || anyThreadTraced ->
                DebuggerThreatImpl(
                    jdwpConnected = jdwpConnected,
                    waitingForDebugger = waitingForDebugger,
                    nativeAttached = nativeAttached,
                    anyThreadTraced = anyThreadTraced,
                    appDebuggable = appDebuggable,
                )
            // Fail closed: without tracer evidence, absence of a tracer isn't observable.
            !evidenceSucceeded && evidenceUnsupported -> UnavailableImpl(
                checkId = id,
                reason = SecurityCheckResult.Unavailable.Reason.Unsupported,
            )
            !evidenceSucceeded -> UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(
                    (evidenceError ?: IllegalStateException("no probe succeeded")).toErrorKind(),
                ),
            )
            else -> SecureImpl(checkId = id)
        }
    }

    private fun tracerPidOf(taskDir: File): Int {
        return try {
            File(taskDir, "status").useLines { lines ->
                lines.firstOrNull { it.startsWith("TracerPid:") }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 0
            }
        } catch (_: Exception) {
            // Threads exit between listing and reading; expected churn, not a tracer.
            0
        }
    }

    private fun isInTracingStop(taskDir: File): Boolean {
        return try {
            val stat = File(taskDir, "stat").readText()
            // comm (field 2) may contain spaces and ')', so the state char is the
            // first token after the LAST ')'. Only 't' is tracing stop; 'T' is a
            // plain SIGSTOP and must not count.
            stat.substringAfterLast(')').trim().firstOrNull() == 't'
        } catch (_: Exception) {
            false
        }
    }

    private external fun nativeProbe(): Int

    companion object {
        // Native probe bit layout — must stay aligned with cpp/checks/debugger_check.cpp.
        private const val FLAG_NATIVE_ATTACHED: Int = 0x1
        private const val FLAG_THREAD_TRACED: Int = 0x2
    }
}
