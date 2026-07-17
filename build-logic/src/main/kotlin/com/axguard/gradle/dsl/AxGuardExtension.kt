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

    /**
     * Enables build-time hash injection for the DEX-integrity check
     * (`SecurityCheckId.DEX_INTEGRITY`). Defaults to the `axguard.dexIntegrity`
     * Gradle property, else `false`.
     *
     * The check fails CLOSED — it reports a threat whenever it runs without an
     * injected hash — so enable this wherever the check is in use. Relies on
     * internal AGP artifact types: on an incompatible AGP version the hash is
     * not injected (a warning is logged) and the check reports a threat. Not
     * supported with dynamic feature modules. App stores must deliver the dex
     * files byte-identical (true for Play today); verify via internal app
     * sharing before rolling out.
     */
    abstract val dexIntegrity: Property<Boolean>
}
