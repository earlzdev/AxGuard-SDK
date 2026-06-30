package com.axguard.sdk.internal.checks

import android.os.NetworkOnMainThreadException
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.NativeLibrary
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.TaskDirUnobservableException
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.obfs.ObfuscatedStrings
import com.axguard.sdk.internal.models.checks.HookThreatImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket

internal class HookCheck : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.HOOK

    override fun run(): SecurityCheckResult {
        // Always in Kotlin: the Xposed-bridge probe needs the app classloader,
        // which native code cannot reach.
        val xposedBridge = xposedBridgeLoaded()

        // Native probe first: maps/thread/port scans via direct syscalls rather
        // than the Java IO a hooking framework can intercept.
        val nativeBits: Int? = if (NativeLibrary.loaded) {
            runCatching { nativeProbe() }.getOrNull()?.takeIf { it >= 0 }
        } else {
            null
        }

        if (nativeBits != null) {
            return decide(
                suspiciousLibrary = nativeBits and FLAG_SUSPICIOUS_LIBRARY != 0,
                fridaThreadDetected = nativeBits and FLAG_FRIDA_THREAD != 0,
                fridaPortOpen = nativeBits and FLAG_FRIDA_PORT != 0,
                fridaServerOnDisk = nativeBits and FLAG_FRIDA_SERVER != 0,
                xposedBridge = xposedBridge,
                evidenceSucceeded = true,
                firstError = null,
            )
        }

        // Kotlin fallback: independent probes so one failure can't taint the rest.
        val libsResult = runCatching { suspiciousLibraryInMaps() }
        val threadResult = runCatching { fridaThreadPresent() }
        val portResult = runCatching { checkFridaPort() }
        // SELinux denies stat on /data/local/tmp on most modern devices; kept for
        // older/permissive ones only.
        val serverResult = runCatching { ObfuscatedStrings.FRIDA_SERVER_PATHS.any { File(it).exists() } }

        val evidenceProbes = listOf(libsResult, threadResult, portResult, serverResult)
        val evidenceSucceeded = evidenceProbes.any { it.isSuccess }
        val firstError = evidenceProbes.firstNotNullOfOrNull { it.exceptionOrNull() }
        val anyUnsupported = evidenceProbes.any { it.exceptionOrNull() === TaskDirUnobservableException() }

        return decide(
            suspiciousLibrary = libsResult.getOrDefault(false),
            fridaThreadDetected = threadResult.getOrDefault(false),
            fridaPortOpen = portResult.getOrDefault(false),
            fridaServerOnDisk = serverResult.getOrDefault(false),
            xposedBridge = xposedBridge,
            evidenceSucceeded = evidenceSucceeded,
            firstError = firstError,
            anyUnsupported = anyUnsupported,
        )
    }

    private fun decide(
        suspiciousLibrary: Boolean,
        fridaThreadDetected: Boolean,
        fridaPortOpen: Boolean,
        fridaServerOnDisk: Boolean,
        xposedBridge: Boolean,
        evidenceSucceeded: Boolean,
        firstError: Throwable?,
        anyUnsupported: Boolean = false,
    ): SecurityCheckResult = when {
        suspiciousLibrary || xposedBridge || fridaServerOnDisk || fridaPortOpen || fridaThreadDetected ->
            HookThreatImpl(
                suspiciousLibraryInMaps = suspiciousLibrary,
                xposedBridgeLoaded = xposedBridge,
                fridaServerOnDisk = fridaServerOnDisk,
                standardFridaPortOpen = fridaPortOpen,
                fridaThreadDetected = fridaThreadDetected,
            )
        // Fail closed: if hook evidence couldn't be gathered, absence isn't observable.
        !evidenceSucceeded && anyUnsupported -> UnavailableImpl(
            checkId = id,
            reason = SecurityCheckResult.Unavailable.Reason.Unsupported,
        )
        !evidenceSucceeded -> UnavailableImpl(
            checkId = id,
            reason = ErrorReasonImpl(
                (firstError ?: IllegalStateException("no probe succeeded")).toErrorKind(),
            ),
        )
        else -> SecureImpl(checkId = id)
    }

    private fun suspiciousLibraryInMaps(): Boolean {
        BufferedReader(FileReader("/proc/self/maps")).use { reader ->
            return reader.lineSequence().any { line ->
                // Match the filename component only: substring-matching the whole
                // line false-positives (libspine.so contains "pine", etc.).
                val slash = line.lastIndexOf('/')
                if (slash < 0) return@any false
                val filename = line.substring(slash + 1).lowercase()
                ObfuscatedStrings.SUSPICIOUS_FILENAME_PREFIXES.any { filename.startsWith(it) }
            }
        }
    }

    private fun fridaThreadPresent(): Boolean {
        val tasks = File("/proc/self/task").listFiles().orEmpty()
        if (tasks.isEmpty()) throw TaskDirUnobservableException()
        return tasks.any { task ->
            val name = try {
                File(task, "comm").readText().trim()
            } catch (_: Exception) {
                // Thread exited between listing and reading; expected churn.
                return@any false
            }
            // "gmain" is deliberately excluded: GLib-based apps have a legit thread there.
            name in ObfuscatedStrings.FRIDA_THREAD_EXACT || name.startsWith(ObfuscatedStrings.FRIDA_THREAD_PREFIX)
        }
    }

    private fun xposedBridgeLoaded(): Boolean {
        for (className in ObfuscatedStrings.XPOSED_CLASSES) {
            try {
                // initialize = false: detect presence without running the
                // framework's static initializers (heavy and observable).
                Class.forName(className, false, HookCheck::class.java.classLoader)
                return true
            } catch (_: ClassNotFoundException) {
                // not present
            } catch (e: Throwable) {
                // Present but broken class can throw LinkageError; a failed probe
                // must not escape the check.
                AxLog.e(TAG, "Unexpected error checking $className", e)
            }
        }
        return false
    }

    private fun checkFridaPort(): Boolean {
        for (port in FRIDA_PORTS) {
            try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT_MS) }
                return true
            } catch (e: NetworkOnMainThreadException) {
                throw e
            } catch (_: Exception) {
                // port not open, try next
            }
        }
        return false
    }

    private external fun nativeProbe(): Int

    companion object {
        private const val TAG = "HookCheck"

        private const val CONNECT_TIMEOUT_MS = 200

        // Native probe bit layout — must stay aligned with cpp/checks/hook_check.cpp.
        private const val FLAG_SUSPICIOUS_LIBRARY: Int = 0x1
        private const val FLAG_FRIDA_THREAD: Int = 0x2
        private const val FLAG_FRIDA_PORT: Int = 0x4
        private const val FLAG_FRIDA_SERVER: Int = 0x8

        private val FRIDA_PORTS = listOf(27042, 27043)
    }
}
