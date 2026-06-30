package com.axguard.sdk.internal.checks

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckErrorKind
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable.Reason
import com.axguard.sdk.internal.NativeLibrary
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.internal.models.checks.AppIntegrityThreatImpl
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import java.security.MessageDigest

internal class AppIntegrityCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.APP_INTEGRITY

    override fun run(): SecurityCheckResult {
        if (!libraryLoaded) {
            return UnavailableImpl(
                checkId = id,
                reason = Reason.Unsupported,
            )
        }

        // The expected fingerprint is injected by the AxGuard Gradle plugin at the
        // consumer's build (obfuscated). Absent means integrity checking wasn't
        // configured, so the check is Unsupported rather than a failure.
        val expectedObfuscated = readExpectedFingerprint()
            ?: return UnavailableImpl(
                checkId = id,
                reason = Reason.Unsupported,
            )

        val fingerprints: List<String> = computeSigningFingerprints()
        if (fingerprints.isEmpty()) {
            return UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(SecurityCheckErrorKind.IO),
            )
        }

        // Legacy multi-signer APKs pass when any current signer matches. Signing
        // history is deliberately not consulted: key rotation requires updating
        // the fingerprint configured in the plugin DSL.
        val match = try {
            fingerprints.any { nativeCheckFingerprint(expectedObfuscated, it) }
        } catch (e: UnsatisfiedLinkError) {
            return UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(SecurityCheckErrorKind.INTERNAL),
            )
        }

        return if (match) {
            SecureImpl(checkId = id)
        } else {
            AppIntegrityThreatImpl(actualFingerprints = fingerprints)
        }
    }

    // Reads the obfuscated expected fingerprint from the manifest <meta-data> the
    // AxGuard Gradle plugin injected into the consumer app. Base64 is only
    // transport; the value stays XOR-obfuscated and is decoded in native, so no
    // plaintext fingerprint ever exists in the Java layer.
    private fun readExpectedFingerprint(): ByteArray? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
            val encoded = appInfo.metaData?.getString(META_NAME)
            if (encoded.isNullOrEmpty()) return null
            Base64.decode(encoded, Base64.NO_WRAP).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            AxLog.w(TAG, "Failed to read expected fingerprint", e)
            null
        }
    }

    private fun computeSigningFingerprints(): List<String> {
        // Cached: signing can't change without a reinstall (which kills the process).
        // An empty list means failure and is never cached, so a transient
        // PackageManager error is retried next run.
        cachedFingerprints?.let { return it }
        return synchronized(fingerprintCacheLock) {
            cachedFingerprints?.let { return@synchronized it }
            val computed = computeSigningFingerprintsUncached()
            if (computed.isNotEmpty()) cachedFingerprints = computed
            computed
        }
    }

    private fun computeSigningFingerprintsUncached(): List<String> {
        return try {
            val certs: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    /* packageName = */ context.packageName,
                    /* flags = */ PackageManager.GET_SIGNING_CERTIFICATES,
                )
                info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    /* packageName = */ context.packageName,
                    /* flags = */ PackageManager.GET_SIGNATURES,
                )
                @Suppress("DEPRECATION")
                info.signatures?.map { it.toByteArray() }.orEmpty()
            }
            certs.map { sha256Hex(it) }
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to retrieve signing certificate", e)
            emptyList()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private external fun nativeCheckFingerprint(
        expectedObfuscated: ByteArray,
        actualFingerprint: String,
    ): Boolean

    companion object {
        private const val TAG = "AppIntegrityCheck"

        // Manifest meta-data key the AxGuard Gradle plugin writes into the consumer
        // app, carrying the obfuscated expected fingerprint. Must match the
        // plugin's AxGuardConfigurationTask.META_NAME.
        private const val META_NAME = "com.axguard.sdk.EXPECTED_FP"

        val libraryLoaded: Boolean get() = NativeLibrary.loaded

        @Volatile private var cachedFingerprints: List<String>? = null
        private val fingerprintCacheLock = Any()
    }
}
