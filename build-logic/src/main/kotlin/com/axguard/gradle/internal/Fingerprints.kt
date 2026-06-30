package com.axguard.gradle.internal

import com.axguard.gradle.config.AxGuardBuild
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64

/**
 * Canonicalizes a fingerprint for comparison: removes colon separators and any
 * whitespace, then lowercases. So `"AA:BB CC"` and `"aabbcc"` normalize equally.
 *
 * @param raw the fingerprint as supplied (hex, with optional colons/whitespace).
 * @return the compact lowercase-hex form, with no separators.
 */
internal fun normalizeFingerprint(raw: String): String {
    return raw.filter { it != ':' && !it.isWhitespace() }.lowercase()
}

/**
 * XOR-obfuscates [fingerprint] and Base64-wraps it for transport in the manifest
 * meta-data. Uses the shared per-byte key schedule (`(key + i) & 0xFF`); the SDK
 * reverses it in native, so no plaintext fingerprint reaches the app's Java layer.
 *
 * @param fingerprint the normalized fingerprint to encode.
 * @param key the XOR base key; defaults to [AxGuardBuild.OBFS_KEY], the value the
 *   SDK decodes with.
 * @return the obfuscated bytes, Base64-encoded without line wrapping.
 */
internal fun obfuscateToBase64(fingerprint: String, key: Int = AxGuardBuild.OBFS_KEY): String {
    val bytes = fingerprint.toByteArray(Charsets.UTF_8)
    val out = ByteArray(bytes.size) { i ->
        (bytes[i].toInt() xor ((key + i) and 0xFF)).toByte()
    }
    return Base64.getEncoder().encodeToString(out)
}

/**
 * Computes the SHA-256 certificate fingerprint(s) held in a keystore.
 *
 * Tries the `PKCS12` type first, then legacy `JKS`, so both modern
 * (`.keystore`/`.p12`) and older (`.jks`) stores work. Certificates are read
 * with only the store password — the per-key password is not needed. The
 * function never throws: any failure (missing file, wrong password, unreadable
 * entry) results in an empty list.
 *
 * @param storeFile the keystore file to read.
 * @param storePassword the password protecting [storeFile].
 * @return the lowercase-hex SHA-256 fingerprint of every certificate in the
 *   store, or an empty list if none could be read.
 */
internal fun keystoreFingerprints(storeFile: File, storePassword: String): List<String> {
    for (type in listOf("PKCS12", "JKS")) {
        try {
            val keyStore = KeyStore.getInstance(type)
            storeFile.inputStream().use { keyStore.load(it, storePassword.toCharArray()) }
            val aliases = keyStore.aliases().toList()
            val digest = MessageDigest.getInstance("SHA-256")
            val fingerprints = aliases.mapNotNull { alias ->
                keyStore.getCertificate(alias)?.let { cert ->
                    digest.digest(cert.encoded).joinToString("") { "%02x".format(it) }
                }
            }
            if (fingerprints.isNotEmpty()) return fingerprints
        } catch (_: Exception) {
            // Wrong keystore type / bad password: try the next type.
        }
    }
    return emptyList()
}
