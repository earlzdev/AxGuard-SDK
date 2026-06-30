package com.axguard.sdk.internal.checks

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable.Reason
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.EncryptionThreatImpl
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind

internal class EncryptionCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.ENCRYPTION

    override fun run(): SecurityCheckResult {
        val status = try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.storageEncryptionStatus
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to get encryption state", e)
            return UnavailableImpl(id, ErrorReasonImpl(e.toErrorKind()))
        }

        // ACTIVE_DEFAULT_KEY is encrypted but with a key bound to no credential —
        // reported as a threat, not as full encryption.
        val defaultKeyEncryption = status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        val storageEncryptionInactive = status == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE

        val encryptionMeaningful = when (status) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> true
            else -> false
        }
        if (!encryptionMeaningful && !storageEncryptionInactive && !defaultKeyEncryption) {
            return UnavailableImpl(id, Reason.Unsupported)
        }

        // Credential-encrypted storage derives no protection without a lock screen.
        val noSecureLockScreen = !isDeviceSecure()

        return if (storageEncryptionInactive || defaultKeyEncryption || noSecureLockScreen) {
            EncryptionThreatImpl(
                storageEncryptionInactive = storageEncryptionInactive,
                defaultKeyEncryption = defaultKeyEncryption,
                noSecureLockScreen = noSecureLockScreen,
            )
        } else {
            SecureImpl(checkId = id)
        }
    }

    private fun isDeviceSecure(): Boolean {
        return try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.isDeviceSecure
        } catch (e: Exception) {
            AxLog.w(TAG, "Failed to read lock screen state", e)
            // Fail toward "secure" so an error never fabricates a threat.
            true
        }
    }

    companion object {
        private const val TAG = "EncryptionCheck"
    }
}
