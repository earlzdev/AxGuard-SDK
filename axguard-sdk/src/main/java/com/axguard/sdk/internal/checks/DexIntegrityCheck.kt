package com.axguard.sdk.internal.checks

import android.content.Context
import android.util.Base64
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.DexIntegrityThreat
import com.axguard.sdk.api.models.threats.SecurityCheckErrorKind
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable.Reason
import com.axguard.sdk.internal.NativeLibrary
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.internal.models.checks.DexIntegrityThreatImpl
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import java.security.MessageDigest
import java.util.zip.ZipFile

internal class DexIntegrityCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.DEX_INTEGRITY

    override fun run(): SecurityCheckResult {
        if (!libraryLoaded) {
            return UnavailableImpl(
                checkId = id,
                reason = Reason.Unsupported,
            )
        }

        val expectedObfuscated = readExpectedHash()
            ?: return DexIntegrityThreatImpl(DexIntegrityThreat.Reason.BaselineMissing)

        val match = try {
            nativeCheckHash(expectedObfuscated, computeCombinedDexHash())
        } catch (_: UnsatisfiedLinkError) {
            return UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(SecurityCheckErrorKind.INTERNAL),
            )
        }

        return if (match) {
            SecureImpl(checkId = id)
        } else {
            DexIntegrityThreatImpl(DexIntegrityThreat.Reason.HashMismatch)
        }
    }

    private fun readExpectedHash(): ByteArray? {
        return try {
            val encoded = context.assets.open(ASSET_PATH).use { it.readBytes() }
                .toString(Charsets.UTF_8)
                .trim()
            if (encoded.isEmpty()) return null
            Base64.decode(encoded, Base64.NO_WRAP).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            AxLog.w(TAG, "Failed to read expected dex hash", e)
            null
        }
    }

    private fun computeCombinedDexHash(): String {
        cachedHash?.let { return it }
        return synchronized(hashCacheLock) {
            cachedHash ?: computeCombinedDexHashUncached().also { cachedHash = it }
        }
    }

    private fun computeCombinedDexHashUncached(): String {
        val perFile = mutableListOf<String>()
        ZipFile(context.applicationInfo.sourceDir).use { zip ->
            for (entry in zip.entries()) {
                if (!DEX_ENTRY.matches(entry.name)) continue
                val digest = MessageDigest.getInstance("SHA-256")
                zip.getInputStream(entry).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                perFile += digest.digest().toHex()
            }
        }
        perFile.sort()
        return MessageDigest.getInstance("SHA-256")
            .digest(perFile.joinToString("").toByteArray(Charsets.US_ASCII))
            .toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private external fun nativeCheckHash(
        expectedObfuscated: ByteArray,
        actualHash: String,
    ): Boolean

    companion object {

        val libraryLoaded: Boolean
            get() = NativeLibrary.loaded

        private val hashCacheLock = Any()

        private const val TAG = "DexIntegrityCheck"

        private const val ASSET_PATH = "axg/dx.bin"

        private const val BUFFER_SIZE = 64 * 1024

        private val DEX_ENTRY = Regex("classes\\d*\\.dex")

        @Volatile
        private var cachedHash: String? = null
    }
}
