package com.earldev.axguard.demo.domain.model

import com.axguard.sdk.api.models.threats.AdbOverNetworkThreat
import com.axguard.sdk.api.models.threats.AppIntegrityThreat
import com.axguard.sdk.api.models.threats.DebuggerThreat
import com.axguard.sdk.api.models.threats.DeveloperOptionsThreat
import com.axguard.sdk.api.models.threats.EmulatorThreat
import com.axguard.sdk.api.models.threats.EncryptionThreat
import com.axguard.sdk.api.models.threats.HookThreat
import com.axguard.sdk.api.models.threats.ProxyThreat
import com.axguard.sdk.api.models.threats.RootThreat
import com.axguard.sdk.api.models.threats.SecurityCheckErrorKind
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.api.models.threats.UserCaThreat
import com.axguard.sdk.api.models.threats.VerifiedBootThreat
import com.axguard.sdk.api.models.threats.VpnThreat
import com.earldev.axguard.demo.domain.catalog.CheckCatalog

/**
 * Translates the SDK's [SecurityCheckResult] into the UI-facing [SecurityCheck], merging
 * in the static [CheckCatalog] copy and unpacking each threat's concrete signals.
 */
internal fun SecurityCheckResult.toSecurityCheck(): SecurityCheck = SecurityCheck(
    id = checkId,
    metadata = CheckCatalog[checkId],
    status = toStatus(),
    statusHeadline = toHeadline(),
    signals = toSignals(),
    evidence = toEvidence(),
)

private fun SecurityCheckResult.toStatus(): CheckStatus = when (this) {
    is SecurityCheckResult.Secure -> CheckStatus.SECURE
    is SecurityCheckResult.ThreatDetected -> CheckStatus.THREAT
    is SecurityCheckResult.Unavailable -> when (reason) {
        SecurityCheckResult.Unavailable.Reason.Timeout -> CheckStatus.TIMEOUT
        SecurityCheckResult.Unavailable.Reason.Unsupported -> CheckStatus.UNSUPPORTED
        is SecurityCheckResult.Unavailable.Reason.Error -> CheckStatus.ERROR
        else -> CheckStatus.ERROR
    }
    else -> CheckStatus.ERROR
}

private fun SecurityCheckResult.toHeadline(): String = when (this) {
    is SecurityCheckResult.Secure -> "No threat detected"
    is SecurityCheckResult.ThreatDetected -> "Threat detected"
    is SecurityCheckResult.Unavailable -> when (val r = reason) {
        SecurityCheckResult.Unavailable.Reason.Timeout ->
            "Timed out before a verdict was reached"
        SecurityCheckResult.Unavailable.Reason.Unsupported ->
            "Not observable on this device"
        is SecurityCheckResult.Unavailable.Reason.Error ->
            "Probe failed (${r.kind.errorKindLabel()})"
        else -> "Unavailable"
    }
    else -> "Unavailable"
}

private fun SecurityCheckResult.toEvidence(): List<Evidence> = when (this) {
    is AppIntegrityThreat -> actualFingerprints.mapIndexed { i, fp ->
        Evidence(if (actualFingerprints.size == 1) "Actual signer" else "Signer ${i + 1}", fp)
    }
    is AdbOverNetworkThreat -> buildList {
        tcpPort?.let { add(Evidence("ADB TCP port", it)) }
    }
    is UserCaThreat -> buildList {
        add(Evidence("System CAs", systemCaCount.toString()))
        subjects.forEach { add(Evidence("User CA", it)) }
    }
    is VpnThreat -> listOf(Evidence("Active VPN networks", activeNetworkCount.toString()))
    is ProxyThreat -> buildList {
        http?.let { add(Evidence("HTTP proxy", "${it.host}:${it.port}")) }
        pac?.let { add(Evidence("PAC script", it.scriptUrl)) }
    }
    is VerifiedBootThreat -> listOf(Evidence("Boot state", state.label()))
    else -> emptyList()
}

private fun SecurityCheckResult.toSignals(): List<CheckSignal> = when (this) {
    is RootThreat -> listOf(
        CheckSignal("su binary found", suBinaryFound),
        CheckSignal("Test-keys build", testKeysDetected),
        CheckSignal("adb root property", adbRootProp),
        CheckSignal("Insecure build props", insecureBuildProps),
        CheckSignal("su command available", suCommandAvailable),
        CheckSignal("System partition writable", systemPartitionRw),
        CheckSignal("Magisk detected", magiskDetected),
        CheckSignal("KernelSU detected", kernelSuDetected),
    )
    is DebuggerThreat -> listOf(
        CheckSignal("JDWP debugger connected", jdwpConnected),
        CheckSignal("Waiting for debugger", waitingForDebugger),
        CheckSignal("Native tracer attached", nativeAttached),
        CheckSignal("Thread in tracing-stop", anyThreadTraced),
        CheckSignal("App is debuggable", appDebuggable),
    )
    is HookThreat -> listOf(
        CheckSignal("Hook library in memory maps", suspiciousLibraryInMaps),
        CheckSignal("Xposed bridge loaded", xposedBridgeLoaded),
        CheckSignal("frida-server on disk", fridaServerOnDisk),
        CheckSignal("Standard frida port open", standardFridaPortOpen),
        CheckSignal("frida thread detected", fridaThreadDetected),
    )
    is EmulatorThreat -> listOf(
        CheckSignal("Emulator hardware", emulatorHardware),
        CheckSignal("QEMU device file", qemuDeviceFile),
        CheckSignal("Goldfish TTY driver", goldfishTtyDriver),
        CheckSignal("Genymotion manufacturer", genymotionManufacturer),
        CheckSignal("Emulator model", emulatorModel),
        CheckSignal("QEMU properties", qemuProperties),
        CheckSignal("Generic fingerprint", genericFingerprint),
        CheckSignal("Generic brand", genericBrand),
        CheckSignal("Emulator product", emulatorProduct),
        CheckSignal("Zero battery temperature", zeroBatteryTemp),
        CheckSignal("No accelerometer", noAccelerometer),
    )
    is EncryptionThreat -> listOf(
        CheckSignal("Storage encryption inactive", storageEncryptionInactive),
        CheckSignal("Default-key encryption", defaultKeyEncryption),
        CheckSignal("No secure lock screen", noSecureLockScreen),
    )
    is AdbOverNetworkThreat -> listOf(
        CheckSignal("TCP port property set", tcpPortSet),
        CheckSignal("Wireless debugging enabled", wifiAdbEnabled),
        CheckSignal("ADB port reachable", portReachable),
    )
    is DeveloperOptionsThreat -> listOf(
        CheckSignal("Developer Options enabled", developerOptionsEnabled),
        CheckSignal("USB debugging enabled", adbEnabled),
    )
    is ProxyThreat -> listOf(
        CheckSignal("Default HTTP proxy", http != null),
        CheckSignal("PAC proxy", pac != null),
        CheckSignal("Per-network proxy", perNetworkProxyPresent),
        CheckSignal("Proxy system property", proxySystemPropertySet),
    )
    else -> emptyList()
}

private fun VerifiedBootThreat.State.label(): String = when (this) {
    VerifiedBootThreat.State.Yellow -> "Yellow — verified with a user key"
    VerifiedBootThreat.State.Orange -> "Orange — Verified Boot disabled"
    VerifiedBootThreat.State.Red -> "Red — boot verification failed"
    else -> "Non-green"
}

private fun Int.errorKindLabel(): String = when (this) {
    SecurityCheckErrorKind.IO -> "I/O"
    SecurityCheckErrorKind.SECURITY -> "security"
    SecurityCheckErrorKind.INTERRUPTED -> "interrupted"
    SecurityCheckErrorKind.INTERNAL -> "internal"
    else -> "unknown"
}
