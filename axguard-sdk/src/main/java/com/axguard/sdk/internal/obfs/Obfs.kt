package com.axguard.sdk.internal.obfs

import com.axguard.sdk.BuildConfig

/** Keystream decoder; must stay byte-identical to cpp/obfs.h and gen_obfs.py. */
internal object Obfs {

    private val KEY: Int = BuildConfig.OBFS_KEY

    private fun seed(): Int {
        var s = KEY xor 0x9E3779B9.toInt()
        s *= 0x85EBCA6B.toInt()
        s = s xor (s ushr 13)
        return s or 1
    }

    fun decode(encoded: ByteArray): String {
        var state = seed()
        var prev = seed() and 0xFF
        val out = ByteArray(encoded.size)
        for (i in encoded.indices) {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            val k = (state xor (state ushr 8) xor (state ushr 16) xor (state ushr 24)) and 0xFF
            val c = encoded[i].toInt() and 0xFF
            out[i] = (c xor k xor (i and 0xFF) xor prev).toByte()
            prev = c
        }
        return String(out, Charsets.UTF_8)
    }
}
