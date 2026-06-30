package com.axguard.gradle.tasks

import com.axguard.gradle.internal.keystoreFingerprints
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Prints the SHA-256 fingerprint of a signing certificate, ready to paste into
 * `axguard { certFingerprint.set(…) }`. Uses the JDK's KeyStore API — no external
 * `keytool` needed. Point it at a keystore explicitly:
 *
 *   `-Paxguard.keystore=<path> -Paxguard.storepass=<pw>`
 *
 * IMPORTANT: for apps distributed with **Play App Signing**, the certificate that
 * signs the installed APK is Google's app-signing key — its private key is not on
 * your machine, so no keystore read can produce it. Copy that SHA-256 from
 * Play Console → App integrity → App signing and set it as `certFingerprint`.
 *
 * Run: `./gradlew axguardCertFingerprint -Paxguard.keystore=<path> -Paxguard.storepass=<pw>`
 */
abstract class AxGuardCertFingerprintTask : DefaultTask() {

    /**
     * `-Paxguard.keystore` — keystore path to read.
     */
    @get:Internal
    abstract val keystorePath: Property<String>

    /**
     * `-Paxguard.storepass` — store password for the keystore.
     */
    @get:Internal
    abstract val storePassword: Property<String>

    init {
        group = "axguard"
        description = "Prints SHA-256 signing-certificate fingerprints to paste into certFingerprint."
    }

    @TaskAction
    fun printFingerprints() {
        val keystore = keystorePath.orNull
        if (keystore == null) {
            logger.lifecycle(
                "axguard: pass -Paxguard.keystore=<path> -Paxguard.storepass=<pw> to read a keystore."
            )
            return
        }
        printKeystore(File(keystore))
        logger.lifecycle(
            "axguard: for a Play-signed release, use the SHA-256 from " +
                "Play Console → App integrity → App signing instead of a local keystore."
        )
    }

    private fun printKeystore(file: File) {
        if (!file.exists()) {
            logger.lifecycle("axguard: keystore not found at ${file.path}.")
            return
        }
        val password = storePassword.orNull
        if (password == null) {
            logger.lifecycle("axguard: pass -Paxguard.storepass=<password> to read ${file.path}.")
            return
        }
        val fingerprints = keystoreFingerprints(file, password)
        if (fingerprints.isEmpty()) {
            logger.lifecycle("axguard: could not read a certificate from ${file.path}.")
        } else {
            fingerprints.forEach { fp -> logger.lifecycle("axguard: ${file.name} SHA-256 = $fp") }
        }
    }
}
