package com.axguard.sdk.internal.checks

import android.content.Context
import android.provider.Settings
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.DeveloperOptionsThreatImpl
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind

internal class DeveloperOptionsCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.DEVELOPER_OPTIONS

    override fun run(): SecurityCheckResult {
        var error: Throwable? = null
        var developerOptionsEnabled = false
        var adbEnabled = false

        try {
            developerOptionsEnabled = Settings.Global.getInt(
                /* cr = */ context.contentResolver,
                /* name = */ Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                /* def = */ 0,
            ) == 1
            // adb_enabled persists independently of the Developer Options master
            // toggle (e.g. flipped via shell), so it is read unconditionally.
            adbEnabled = Settings.Global.getInt(
                /* cr = */ context.contentResolver,
                /* name = */ Settings.Global.ADB_ENABLED,
                /* def = */ 0,
            ) == 1
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to check developer options", e)
            error = e
        }

        return when {
            developerOptionsEnabled || adbEnabled -> DeveloperOptionsThreatImpl(
                developerOptionsEnabled = developerOptionsEnabled,
                adbEnabled = adbEnabled,
            )
            error != null -> UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(error.toErrorKind()),
            )
            else -> SecureImpl(checkId = id)
        }
    }

    companion object {
        private const val TAG = "DeveloperOptionsCheck"
    }
}
