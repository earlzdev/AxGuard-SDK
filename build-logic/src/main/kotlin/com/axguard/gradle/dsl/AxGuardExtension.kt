package com.axguard.gradle.dsl

import org.gradle.api.provider.Property

/**
 * DSL for configuring the AxGuard SDK from the consumer's app module.
 *
 * The fingerprint is taken from [certFingerprint] if set in the DSL, otherwise
 * from the `axguard.certFingerprint` Gradle property (`gradle.properties` or
 * `-P` — wired as the property's convention). Obtain the value once with
 * `keytool -list -v -keystore <store>` (the SHA-256 line), or the
 * `axguardCertFingerprint` task.
 *
 * ```
 * axguard {
 *     certFingerprint.set("AA:BB:…")
 * }
 * ```
 */
abstract class AxGuardExtension {

    /**
     * SHA-256 signing-certificate fingerprint (hex, colons/whitespace optional).
     */
    abstract val certFingerprint: Property<String>
}
