package com.earldev.axguard.demo.domain.catalog

import com.axguard.sdk.api.models.SecurityCheckId
import com.earldev.axguard.demo.domain.model.CheckCategory
import com.earldev.axguard.demo.domain.model.CheckMetadata

/**
 * Single source of truth for the human-facing description of every check the SDK exposes.
 * Keyed by [SecurityCheckId]; kept separate from any run's outcome so the copy can be
 * reviewed, localized, and tested in isolation.
 */
object CheckCatalog {

    private val entries: Map<Int, CheckMetadata> = listOf(
        CheckMetadata(
            id = SecurityCheckId.ROOT,
            title = "Root Access",
            category = CheckCategory.DEVICE_INTEGRITY,
            tagline = "Privilege escalation & superuser tools",
            whatItChecks = "Looks for su binaries, Magisk and KernelSU, test-keys builds, " +
                "writable system partitions, and other traces of a rooted device.",
            whyItMatters = "Root grants unrestricted access to your app's process and " +
                "sandbox, letting an attacker read secrets, patch code, and bypass every " +
                "other on-device defense.",
            recommendation = "Treat rooted devices as untrusted: gate high-risk actions, " +
                "step up authentication, or block sensitive flows entirely.",
        ),
        CheckMetadata(
            id = SecurityCheckId.DEBUGGER,
            title = "Debugger Attached",
            category = CheckCategory.TAMPERING,
            tagline = "JDWP & native tracers",
            whatItChecks = "Detects a connected Java (JDWP) debugger, a native tracer " +
                "(non-zero TracerPid), threads stuck in tracing-stop, and a debuggable manifest.",
            whyItMatters = "A live debugger lets an attacker single-step your code, dump " +
                "memory, and alter control flow at runtime to defeat security logic.",
            recommendation = "Refuse to run sensitive logic while a debugger is attached and " +
                "ship release builds with the debuggable flag disabled.",
        ),
        CheckMetadata(
            id = SecurityCheckId.HOOK,
            title = "Hooking Framework",
            category = CheckCategory.TAMPERING,
            tagline = "Frida, Xposed & instrumentation",
            whatItChecks = "Scans for hooking libraries in memory maps, a loadable Xposed " +
                "bridge, on-disk frida-server, open frida ports, and frida worker threads.",
            whyItMatters = "Instrumentation frameworks rewrite functions on the fly, so an " +
                "attacker can intercept calls, forge return values, and lift keys from memory.",
            recommendation = "Abort security-critical operations when instrumentation is " +
                "present and consider additional runtime integrity attestation.",
        ),
        CheckMetadata(
            id = SecurityCheckId.SELINUX,
            title = "SELinux Enforcing",
            category = CheckCategory.DEVICE_INTEGRITY,
            tagline = "Mandatory access control",
            whatItChecks = "Confirms SELinux is in enforcing mode rather than permissive.",
            whyItMatters = "SELinux confines every process to a policy. In permissive mode " +
                "those confinements are only logged, widening the blast radius of any exploit.",
            recommendation = "Consider a permissive device tampered; raise your trust bar " +
                "before allowing sensitive operations.",
        ),
        CheckMetadata(
            id = SecurityCheckId.VERIFIED_BOOT,
            title = "Verified Boot",
            category = CheckCategory.DEVICE_INTEGRITY,
            tagline = "Boot chain integrity",
            whatItChecks = "Reads the Verified Boot state via TEE key attestation and flags " +
                "any non-green state (yellow, orange, or red).",
            whyItMatters = "Verified Boot cryptographically guarantees the OS hasn't been " +
                "modified. A non-green state means the boot chain is unlocked or altered.",
            recommendation = "Only green fully assures device integrity; treat other states " +
                "as an unlocked or modified device.",
        ),
        CheckMetadata(
            id = SecurityCheckId.EMULATOR,
            title = "Emulator",
            category = CheckCategory.DEVICE_INTEGRITY,
            tagline = "Virtualized runtime environment",
            whatItChecks = "Weighs strong signals (emulator hardware, QEMU nodes, goldfish " +
                "TTY) and weak ones (generic build props, missing sensors) to spot emulators.",
            whyItMatters = "Emulators give attackers snapshots, full introspection, and easy " +
                "automation — an ideal lab for reverse engineering and abuse at scale.",
            recommendation = "Restrict production access on emulators, or require extra " +
                "verification before allowing sensitive actions.",
        ),
        CheckMetadata(
            id = SecurityCheckId.ENCRYPTION,
            title = "Storage Encryption",
            category = CheckCategory.CONFIGURATION,
            tagline = "At-rest data protection",
            whatItChecks = "Verifies storage encryption is active, not backed by a default " +
                "key, and that a secure lock screen protects credential-encrypted storage.",
            whyItMatters = "Without strong at-rest encryption, anyone with physical access " +
                "to the device or a backup can recover the data your app stores.",
            recommendation = "Encourage users to set a secure lock screen and avoid persisting " +
                "sensitive data where encryption guarantees are weak.",
        ),
        CheckMetadata(
            id = SecurityCheckId.APP_INTEGRITY,
            title = "App Integrity",
            category = CheckCategory.TAMPERING,
            tagline = "Signing certificate verification",
            whatItChecks = "Compares the APK's current signing certificate against the " +
                "fingerprint baked in at build time.",
            whyItMatters = "A mismatch means the app was re-signed — a hallmark of " +
                "repackaged, trojanized, or cracked builds distributed outside the store.",
            recommendation = "Refuse to operate when the signer doesn't match; the app may " +
                "have been repackaged with malicious code.",
        ),
        CheckMetadata(
            id = SecurityCheckId.ADB_OVER_NETWORK,
            title = "ADB over Network",
            category = CheckCategory.NETWORK,
            tagline = "Wireless debugging exposure",
            whatItChecks = "Detects an adb TCP port property, wireless debugging, and whether " +
                "the adb port accepts a loopback connection.",
            whyItMatters = "Network-reachable ADB lets anyone on the network install apps, " +
                "read logs, and run shell commands on the device.",
            recommendation = "Warn the user to disable wireless debugging before continuing " +
                "with sensitive operations.",
        ),
        CheckMetadata(
            id = SecurityCheckId.DEVELOPER_OPTIONS,
            title = "Developer Options",
            category = CheckCategory.CONFIGURATION,
            tagline = "Developer settings & USB debugging",
            whatItChecks = "Reads whether Developer Options and USB debugging (adb) are on.",
            whyItMatters = "These settings unlock debugging, mock locations, and other " +
                "developer tooling that eases tampering and data extraction.",
            recommendation = "Consider elevating scrutiny when developer settings are on, " +
                "especially alongside other risk signals.",
        ),
        CheckMetadata(
            id = SecurityCheckId.USER_CA,
            title = "User CA Certificates",
            category = CheckCategory.NETWORK,
            tagline = "TLS interception trust anchors",
            whatItChecks = "Enumerates user-installed CA certificates alongside the system " +
                "CA count for context.",
            whyItMatters = "A user-installed CA can let a proxy transparently decrypt TLS " +
                "traffic — the foundation of a man-in-the-middle attack.",
            recommendation = "Pin certificates for sensitive traffic so a rogue user CA " +
                "cannot intercept it.",
        ),
        CheckMetadata(
            id = SecurityCheckId.VPN,
            title = "VPN Active",
            category = CheckCategory.NETWORK,
            tagline = "Active VPN transport",
            whatItChecks = "Counts active networks carrying a VPN transport.",
            whyItMatters = "A VPN reroutes all traffic through a third party. While often " +
                "benign, it can also front traffic interception or geo-spoofing.",
            recommendation = "Treat as contextual signal — combine with other indicators " +
                "rather than blocking on VPN presence alone.",
        ),
        CheckMetadata(
            id = SecurityCheckId.PROXY,
            title = "HTTP Proxy",
            category = CheckCategory.NETWORK,
            tagline = "Configured HTTP / PAC proxy",
            whatItChecks = "Inspects the default HTTP proxy, PAC script, per-network proxies, " +
                "and JVM proxy system properties.",
            whyItMatters = "A configured proxy can capture and modify HTTP traffic, a common " +
                "setup for intercepting or tampering with API calls.",
            recommendation = "Pin certificates and validate responses so a proxy cannot read " +
                "or alter sensitive traffic.",
        ),
    ).associateBy { it.id }

    /** Every check's metadata, in canonical [SecurityCheckId] order. */
    val all: List<CheckMetadata> = entries.values.sortedBy { it.id }

    /** Metadata for [checkId], or a safe generic fallback for an unknown id. */
    operator fun get(checkId: Int): CheckMetadata = entries[checkId] ?: CheckMetadata(
        id = checkId,
        title = "Check #$checkId",
        category = CheckCategory.DEVICE_INTEGRITY,
        tagline = "Security check",
        whatItChecks = "A security check reported by the AxGuard SDK.",
        whyItMatters = "This check contributes to the overall device trust assessment.",
        recommendation = "Review the reported signals to decide how to respond.",
    )
}
