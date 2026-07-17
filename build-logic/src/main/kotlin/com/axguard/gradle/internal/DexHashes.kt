package com.axguard.gradle.internal

import java.io.File
import java.security.MessageDigest

/**
 * Streams a file through SHA-256 and returns the lowercase-hex digest.
 */
internal fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * Combined dex hash: SHA-256 each file → lowercase hex, sort the hex strings
 * lexicographically, concatenate, SHA-256 the ASCII concatenation → hex.
 *
 * Deliberately independent of file names and order — packaging renames dex
 * files (`classes.dex`, `classes2.dex`, …) and enumerates them in
 * build-dependent order. Must stay byte-identical with the SDK's
 * `DexIntegrityCheck.computeCombinedDexHashUncached`.
 */
internal fun combinedDexHash(dexFiles: Collection<File>): String {
    val perFile = dexFiles.map { sha256Hex(it) }.sorted()
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(perFile.joinToString("").toByteArray(Charsets.US_ASCII))
    return digest.joinToString("") { "%02x".format(it) }
}
