package com.axguard.sdk.internal.checks

import android.annotation.SuppressLint
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable.Reason
import com.axguard.sdk.internal.NativeLibrary
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.SelinuxThreatImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind
import java.io.File
import java.io.FileNotFoundException

internal class SelinuxCheck : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.SELINUX

    @SuppressLint("PrivateApi")
    override fun run(): SecurityCheckResult {
        // Native selinuxfs read first: direct syscall, no reflection to hook.
        // -1 means not observable -> fall through.
        if (NativeLibrary.loaded) {
            try {
                when (nativeEnforce()) {
                    1 -> return SecureImpl(id)
                    0 -> return SelinuxThreatImpl
                }
            } catch (e: UnsatisfiedLinkError) {
                AxLog.w(TAG, "Native SELinux probe unavailable", e)
            }
        }

        // Platform accessor next: portable across OEMs that relocate selinuxfs.
        try {
            val clazz = Class.forName("android.os.SELinux")
            val enforced = clazz.getMethod("isSELinuxEnforced").invoke(null) as? Boolean
            if (enforced == true) return SecureImpl(id)
        } catch (e: Exception) {
            AxLog.w(TAG, "SELinux reflection failed, falling back to sysfs", e)
        }

        return try {
            when (File(SELINUX_ENFORCE_PATH).readText().trim()) {
                "1" -> SecureImpl(id)
                "0" -> SelinuxThreatImpl
                else -> UnavailableImpl(id, Reason.Unsupported)
            }
        } catch (_: FileNotFoundException) {
            // Node absent or read denied by policy (EACCES also surfaces here):
            // the mode is not observable on this device.
            UnavailableImpl(
                checkId = id,
                reason = Reason.Unsupported,
            )
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to read SELinux enforce node", e)
            UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(e.toErrorKind()),
            )
        }
    }

    private external fun nativeEnforce(): Int

    companion object {
        private const val TAG = "SelinuxCheck"
        private const val SELINUX_ENFORCE_PATH = "/sys/fs/selinux/enforce"
    }
}
