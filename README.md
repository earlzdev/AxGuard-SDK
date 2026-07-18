# AxGuard SDK

**On-device Android threat & tamper detection.** One call runs 14 security checks — root, hooking, debuggers, emulators, Verified Boot, signing-certificate and DEX-code integrity, and more.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.earlzdev/axguard-sdk)](https://central.sonatype.com/artifact/io.github.earlzdev/axguard-sdk)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![minSdk](https://img.shields.io/badge/minSdk-26-green)](#requirements)

## Demo

https://github.com/user-attachments/assets/5425366f-444e-44ba-a604-d2d2b3c5a4ec

*The [`demo-app`](https://github.com/earlzdev/AxGuard-SDK/tree/main/demo-app) module runs every check and renders the live report.*

## Features

| Check | `SecurityCheckId` | Detects |
|-------|-------------------|---------|
| Root | `ROOT` | `su` binaries, test-keys, Magisk, KernelSU, RW system partition |
| Debugger | `DEBUGGER` | Attached debugger or native `ptrace` tracer |
| Hook | `HOOK` | Frida, Xposed and other instrumentation frameworks |
| SELinux | `SELINUX` | SELinux not in enforcing mode |
| Verified Boot | `VERIFIED_BOOT` | Boot state via TEE key attestation |
| Emulator | `EMULATOR` | Emulator / virtualized environment |
| Encryption | `ENCRYPTION` | At-rest storage encryption disabled |
| App Integrity | `APP_INTEGRITY` | APK signing-certificate mismatch (repackaging) |
| DEX Integrity | `DEX_INTEGRITY` | `classes*.dex` code tampering vs. the build-time hash |
| ADB over network | `ADB_OVER_NETWORK` | ADB reachable over TCP/IP |
| Developer Options | `DEVELOPER_OPTIONS` | Developer options / USB debugging enabled |
| User CA | `USER_CA` | User-installed CA certificates (MITM risk) |
| VPN | `VPN` | Active VPN tunnel |
| Proxy | `PROXY` | HTTP proxy configured |

- **Hook-resistant.** Root, debugger, hook and SELinux probes run in a hardened `.so` and issue direct syscalls to bypass PLT and Java-layer hooks.
- **Fail-closed.** A blocked or hooked probe reports `Unavailable`, never `Secure`.
- **On-device only.** Every verdict is computed locally; AxGuard sends nothing anywhere.

## Requirements

| | |
|---|---|
| `minSdk` | 26 (Android 8.0) |
| `compileSdk` | 34 |
| JVM target | 11 |

## Installation

Apply the Gradle plugin — it pulls in the runtime AAR for you and powers the App Integrity and DEX Integrity checks by baking your signing-certificate fingerprint and `classes*.dex` hash into the build.

```kotlin
// app/build.gradle.kts
plugins {
    id("io.github.earlzdev.axguard") version "0.1.3"
}

axguard {
    // SHA-256 signing-certificate fingerprint (hex; colons/whitespace optional).
    certFingerprint.set("AA:BB:CC:…")

    // Enable the DEX Integrity check: bakes the classes*.dex hash into the build.
    dexIntegrity.set(true)
}
```

Make sure Maven Central is in your plugin repositories (`settings.gradle.kts`):

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

**Getting the fingerprint.** The plugin adds an `axguardCertFingerprint` task that prints the SHA-256 you paste into `certFingerprint`. It uses the JDK's KeyStore API — no `keytool` needed. Point it at your keystore:

```bash
./gradlew :app:axguardCertFingerprint \
    -Paxguard.keystore=/path/to/release.jks \
    -Paxguard.storepass=<store-password>
```

```text
> Task :app:axguardCertFingerprint
axguard: release.jks SHA-256 = AA:BB:CC:DD:…:99
```

Copy the `SHA-256` value into the `axguard { }` block, or set it as the `axguard.certFingerprint` Gradle property (`gradle.properties` or `-P`) instead.

> **Using Play App Signing?** The APK users install is re-signed with Google's key, whose private key isn't on your machine, so no keystore read gives the right value. Copy the SHA-256 from **Play Console → App integrity → App signing** and use that as `certFingerprint`.

**DEX Integrity.** Setting `dexIntegrity = true` bakes the hash of the compiled `classes*.dex` into the APK, and the `DEX_INTEGRITY` check re-hashes them at runtime to catch code tampering. Both the App Integrity and DEX Integrity checks **fail closed**: they report a threat if the plugin didn't inject their baseline (fingerprint / hash), so enable them wherever the corresponding check runs. The DEX check relies on internal AGP artifacts and is not supported for dynamic-feature apps or an incompatible AGP version — verify a real install (including via Play internal app sharing) before rollout.

## Quick start

`runChecks` is blocking — **call it off the main thread.**

```kotlin
import com.axguard.sdk.api.AxGuardSdk
import com.axguard.sdk.api.models.SecurityCheckConfig

val report = AxGuardSdk.getInstance().runChecks(
    SecurityCheckConfig(context) // empty checkIds → run all 14 checks
)

report.results.forEach { result ->
    Log.d("AxGuard", "check ${result.checkId} -> $result")
}
```

## Usage

### Configuring a run

```kotlin
import com.axguard.sdk.api.models.SecurityCheckConfig
import com.axguard.sdk.api.models.SecurityCheckId

val config = SecurityCheckConfig(
    context   = context,
    checkIds  = setOf(SecurityCheckId.ROOT, SecurityCheckId.HOOK, SecurityCheckId.APP_INTEGRITY),
    timeoutMs = 5_000L,          // shared wall-clock budget; default 10_000 ms
)
```

- **`checkIds`** — the checks to run; an empty set (the default) runs all of them.
- **`timeoutMs`** — one wall-clock budget shared across every check. On expiry, checks still running surface as `Unavailable(Timeout)`. Must be positive.

### Handling results

Every result is one of three cases from the sealed `SecurityCheckResult`. Threats expose per-check detail through their `ThreatDetected` subtype:

```kotlin
import com.axguard.sdk.api.models.threats.SecurityCheckResult.*
import com.axguard.sdk.api.models.threats.RootThreat

when (val result = report.results.first()) {
    is Secure          -> { /* check ran, no threat found */ }

    is RootThreat      -> if (result.magiskDetected || result.suBinaryFound) blockSensitiveFlow()

    is ThreatDetected  -> { /* some other threat; branch on its concrete type */ }

    is Unavailable     -> when (result.reason) {
        is Unavailable.Reason.Timeout     -> { /* didn't finish in budget */ }
        is Unavailable.Reason.Unsupported -> { /* not observable on this device */ }
        is Unavailable.Reason.Error       -> { /* probe failed; inspect reason.kind */ }
    }
}
```

`report.results` holds one entry per requested check, ordered by `SecurityCheckId` ascending. Treat `Unavailable` as a **weak, suspicious** signal — a run that is mostly `Unavailable` may itself indicate tampering.

## Security model

AxGuard is one layer of **defense in depth**. It raises the cost of running your app on a rooted, hooked, emulated, or repackaged device and surfaces high-signal tampering indicators as structured results.

It is **not a root of trust**: every check runs inside your process on hardware the attacker may control, so a determined adversary with a custom ROM, kernel, or patched app can defeat any on-device check. For decisions that matter, pair these signals with server-side verification (e.g. [Play Integrity](https://developer.android.com/google/play/integrity)) and treat AxGuard as corroborating evidence. AxGuard reports risk; how you react — block, degrade, log, step-up auth — is your call.

## Contributing

Contributions are welcome — feel free to [open an issue](https://github.com/earlzdev/AxGuard-SDK/issues) or a pull request. Bug reports, new detection ideas, extra device coverage, and bypass reports are all appreciated; for anything non-trivial, opening an issue first to discuss the approach keeps things smooth. The [`demo-app`](https://github.com/earlzdev/AxGuard-SDK/tree/main/demo-app) module is the easiest way to exercise a check end-to-end.

## License

Licensed under the [Apache License 2.0](LICENSE). Copyright 2026 Ilya Saushin.
