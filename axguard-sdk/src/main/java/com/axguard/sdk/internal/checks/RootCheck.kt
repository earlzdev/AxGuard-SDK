package com.axguard.sdk.internal.checks

import android.os.Build
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.NativeLibrary
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.obfs.ObfuscatedStrings
import com.axguard.sdk.internal.models.checks.RootThreatImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.SystemPropertiesUtil
import com.axguard.sdk.internal.utils.toErrorKind
import java.io.File

internal class RootCheck : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.ROOT

    override fun run(): SecurityCheckResult {
        // Property-derived signals: cheap and effectively infallible, so they are
        // not counted as evidence probes for the Secure/Unavailable branch.
        val testKeysDetected = Build.TAGS?.contains(ObfuscatedStrings.TEST_KEYS_MARKER) == true
        val adbRootProp = SystemPropertiesUtil.get("service.adb.root") == "1"
        val insecureBuildProps = SystemPropertiesUtil.get("ro.debuggable") == "1" ||
            SystemPropertiesUtil.get("ro.secure") == "0"

        // Native probe first: gathered via direct syscalls, out of reach of hooks
        // on the Java File/IO the fallback uses. If it ran, trust its bits fully.
        if (NativeLibrary.loaded) {
            val bits = runCatching { nativeProbe() }.getOrNull()
            if (bits != null && bits >= 0) {
                return buildResult(
                    suBinaryFound = bits and FLAG_SU_BINARY != 0,
                    suCommandAvailable = bits and FLAG_SU_COMMAND != 0,
                    systemPartitionRw = bits and FLAG_SYSTEM_RW != 0,
                    magiskDetected = bits and FLAG_MAGISK != 0,
                    kernelSuDetected = bits and FLAG_KERNEL_SU != 0,
                    testKeysDetected = testKeysDetected,
                    adbRootProp = adbRootProp,
                    insecureBuildProps = insecureBuildProps,
                    evidenceSucceeded = true,
                    firstError = null,
                )
            }
        }

        // Kotlin fallback: independent probes so one failure can't taint the rest.
        val suPathResult = runCatching { ObfuscatedStrings.SU_PATHS.any { File(it).exists() } }
        val suOnPathResult = runCatching { checkSuOnPath() }
        // SELinux denies stat on /data/adb to apps on most modern devices, so the
        // path probes usually read false even on Magisk; the mount scan fires there.
        val magiskFilesResult = runCatching {
            ObfuscatedStrings.MAGISK_PATHS.any { File(it).exists() }
        }
        val kernelSuResult = runCatching {
            ObfuscatedStrings.KSU_NODES.any { File(it).exists() }
        }
        val mountsResult = runCatching {
            val mounts = File("/proc/mounts").readLines()
            isSystemMountedRw(mounts) to hasMagiskMounts(mounts)
        }

        val evidenceProbes = listOf(
            suPathResult, suOnPathResult, magiskFilesResult, kernelSuResult, mountsResult,
        )
        val evidenceSucceeded = evidenceProbes.any { it.isSuccess }
        val firstError = evidenceProbes.firstNotNullOfOrNull { it.exceptionOrNull() }

        val (systemPartitionRw, magiskFromMounts) = mountsResult.getOrDefault(false to false)

        return buildResult(
            suBinaryFound = suPathResult.getOrDefault(false),
            suCommandAvailable = suOnPathResult.getOrDefault(false),
            systemPartitionRw = systemPartitionRw,
            magiskDetected = magiskFilesResult.getOrDefault(false) || magiskFromMounts,
            kernelSuDetected = kernelSuResult.getOrDefault(false),
            testKeysDetected = testKeysDetected,
            adbRootProp = adbRootProp,
            insecureBuildProps = insecureBuildProps,
            evidenceSucceeded = evidenceSucceeded,
            firstError = firstError,
        )
    }

    private fun buildResult(
        suBinaryFound: Boolean,
        suCommandAvailable: Boolean,
        systemPartitionRw: Boolean,
        magiskDetected: Boolean,
        kernelSuDetected: Boolean,
        testKeysDetected: Boolean,
        adbRootProp: Boolean,
        insecureBuildProps: Boolean,
        evidenceSucceeded: Boolean,
        firstError: Throwable?,
    ): SecurityCheckResult {
        // insecureBuildProps is deliberately excluded: userdebug/eng firmware is
        // debug-friendly but not rooted, and must not fire this threat alone.
        val anyFired = suBinaryFound || testKeysDetected || adbRootProp ||
            suCommandAvailable || systemPartitionRw || magiskDetected || kernelSuDetected

        return when {
            anyFired -> RootThreatImpl(
                suBinaryFound = suBinaryFound,
                testKeysDetected = testKeysDetected,
                adbRootProp = adbRootProp,
                insecureBuildProps = insecureBuildProps,
                suCommandAvailable = suCommandAvailable,
                systemPartitionRw = systemPartitionRw,
                magiskDetected = magiskDetected,
                kernelSuDetected = kernelSuDetected,
            )
            // Fail closed: with no successful probe we can't claim absence of root.
            !evidenceSucceeded -> UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl((firstError ?: IllegalStateException("no probe succeeded")).toErrorKind()),
            )
            else -> SecureImpl(checkId = id)
        }
    }

    private external fun nativeProbe(): Int

    private fun checkSuOnPath(): Boolean {
        val path = System.getenv("PATH") ?: return false
        // Bare "su" is an unavoidable literal in the bytecode; the longer sibling
        // paths are obfuscated in ObfuscatedStrings.
        val binary = "su"
        return path.split(":").any { dir ->
            dir.isNotEmpty() && File(dir, binary).exists()
        }
    }

    companion object {

        // Native probe bit layout — must stay aligned with cpp/checks/root_check.cpp.
        private const val FLAG_SU_BINARY: Int = 0x1
        private const val FLAG_SU_COMMAND: Int = 0x2
        private const val FLAG_SYSTEM_RW: Int = 0x4
        private const val FLAG_MAGISK: Int = 0x8
        private const val FLAG_KERNEL_SU: Int = 0x10

        internal fun isSystemMountedRw(procMountsLines: List<String>): Boolean {
            return procMountsLines.any { line ->
                val fields = line.split(" ")
                if (fields.size < 4) return@any false
                val mountPoint = fields[1]
                if (mountPoint != "/system" && mountPoint != "/") return@any false
                fields[3].split(",").any { it == "rw" }
            }
        }

        internal fun hasMagiskMounts(procMountsLines: List<String>): Boolean {
            val marker = ObfuscatedStrings.MAGISK_MOUNT_SUBSTR
            return procMountsLines.any { line ->
                val fields = line.split(" ")
                if (fields.size < 2) return@any false
                fields[0].contains(marker) || fields[1].contains(marker)
            }
        }
    }
}
