package com.axguard.sdk.internal.obfs

import com.axguard.sdk.BuildConfig

/**
 * XOR decoder mirroring the native OBFS() macro in cpp/obfs.h. Encoded byte
 * arrays sit in the DEX in place of the plaintext detection strings, defeating
 * `strings classes.dex`. A runtime-instrumentation attacker still sees the
 * decoded value in memory — this raises the bar against static analysis only.
 */
internal object Obfs {

    private val KEY: Int = BuildConfig.OBFS_KEY

    fun decode(encoded: ByteArray): String {
        val out = ByteArray(encoded.size)
        for (i in encoded.indices) {
            out[i] = (encoded[i].toInt() xor ((KEY + i) and 0xFF)).toByte()
        }
        return String(out, Charsets.UTF_8)
    }
}
