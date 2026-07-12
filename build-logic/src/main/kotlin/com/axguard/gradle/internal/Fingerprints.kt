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
 * Obfuscates [fingerprint] with the shared keystream scheme and Base64-wraps it for
 * the manifest meta-data; the SDK reverses it in native (integrity_check.cpp). Must
 * stay byte-identical to cpp/obfs.h, the Kotlin `Obfs` object, and tools/gen_obfs.py
 * (uint32 math reproduced with masked `Int`s). [key] defaults to [AxGuardBuild.OBFS_KEY].
 */
internal fun obfuscateToBase64(fingerprint: String, key: Int = AxGuardBuild.OBFS_KEY): String {
    val bytes = fingerprint.toByteArray(Charsets.UTF_8)

    var seed = key xor 0x9E3779B9.toInt()
    seed *= 0x85EBCA6B.toInt()
    seed = seed xor (seed ushr 13)
    seed = seed or 1

    var state = seed
    var prev = seed and 0xFF
    val out = ByteArray(bytes.size)
    for (i in bytes.indices) {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        val k = (state xor (state ushr 8) xor (state ushr 16) xor (state ushr 24)) and 0xFF
        val c = (bytes[i].toInt() and 0xFF) xor k xor (i and 0xFF) xor prev
        out[i] = c.toByte()
        prev = c and 0xFF
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
