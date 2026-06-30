package com.axguard.sdk.internal.utils

internal object AttestationDerParser {

    internal enum class VerifiedBootState { VERIFIED, SELF_SIGNED, UNVERIFIED, FAILED }

    private const val CLASS_UNIVERSAL = 0
    private const val CLASS_CONTEXT = 2
    private const val TAG_OCTET_STRING = 0x04
    private const val TAG_ENUMERATED = 0x0A
    private const val TAG_SEQUENCE = 0x10

    private const val TAG_ROOT_OF_TRUST = 704

    private const val KEY_DESCRIPTION_TEE_ENFORCED_INDEX = 7

    fun parseVerifiedBootState(extensionValue: ByteArray): VerifiedBootState? {
        return try {
            val outer = readTlv(extensionValue, 0, extensionValue.size) ?: return null
            if (outer.tagClass != CLASS_UNIVERSAL || outer.tagNumber != TAG_OCTET_STRING) return null

            val keyDescription =
                readTlv(extensionValue, outer.contentStart, outer.contentEnd) ?: return null
            if (keyDescription.tagNumber != TAG_SEQUENCE) return null

            val fields = readChildren(extensionValue, keyDescription)
            val teeEnforced = fields.getOrNull(KEY_DESCRIPTION_TEE_ENFORCED_INDEX) ?: return null
            if (teeEnforced.tagNumber != TAG_SEQUENCE) return null

            // Only the TEE-enforced RootOfTrust counts; the softwareEnforced list
            // is trivially forgeable.
            val rootOfTrustField = readChildren(extensionValue, teeEnforced)
                .firstOrNull { it.tagClass == CLASS_CONTEXT && it.tagNumber == TAG_ROOT_OF_TRUST }
                ?: return null

            val rootOfTrust =
                readTlv(extensionValue, rootOfTrustField.contentStart, rootOfTrustField.contentEnd)
                    ?: return null
            if (rootOfTrust.tagNumber != TAG_SEQUENCE) return null

            val stateField = readChildren(extensionValue, rootOfTrust).getOrNull(2) ?: return null
            if (stateField.tagNumber != TAG_ENUMERATED) return null
            if (stateField.contentEnd - stateField.contentStart != 1) return null

            VerifiedBootState.entries.getOrNull(extensionValue[stateField.contentStart].toInt())
        } catch (e: Exception) {
            null
        }
    }

    private class Tlv(
        val tagClass: Int,
        val tagNumber: Int,
        val contentStart: Int,
        val contentEnd: Int,
    )

    private fun readTlv(data: ByteArray, start: Int, limit: Int): Tlv? {
        var pos = start
        if (pos >= limit) return null

        val first = data[pos].toInt() and 0xFF
        pos++
        val tagClass = first ushr 6
        var tagNumber = first and 0x1F
        if (tagNumber == 0x1F) {
            tagNumber = 0
            var more = true
            while (more) {
                if (pos >= limit) return null
                val b = data[pos].toInt() and 0xFF
                pos++
                tagNumber = (tagNumber shl 7) or (b and 0x7F)
                more = b and 0x80 != 0
                if (tagNumber > 0xFFFF) return null
            }
        }

        if (pos >= limit) return null
        val lenByte = data[pos].toInt() and 0xFF
        pos++
        val length: Int
        if (lenByte < 0x80) {
            length = lenByte
        } else {
            val numBytes = lenByte and 0x7F
            if (numBytes == 0 || numBytes > 4) return null
            var acc = 0L
            repeat(numBytes) {
                if (pos >= limit) return null
                acc = (acc shl 8) or (data[pos].toInt() and 0xFF).toLong()
                pos++
            }
            if (acc > Int.MAX_VALUE) return null
            length = acc.toInt()
        }

        val contentEnd = pos + length
        if (contentEnd > limit || contentEnd < pos) return null
        return Tlv(tagClass, tagNumber, pos, contentEnd)
    }

    private fun readChildren(data: ByteArray, parent: Tlv): List<Tlv> {
        val children = mutableListOf<Tlv>()
        var pos = parent.contentStart
        while (pos < parent.contentEnd) {
            val child = readTlv(data, pos, parent.contentEnd) ?: break
            children.add(child)
            pos = child.contentEnd
        }
        return children
    }
}
