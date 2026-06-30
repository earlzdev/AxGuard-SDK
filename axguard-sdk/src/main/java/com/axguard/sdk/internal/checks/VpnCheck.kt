package com.axguard.sdk.internal.checks

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.models.checks.VpnThreatImpl
import com.axguard.sdk.internal.utils.toErrorKind

internal class VpnCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.VPN

    override fun run(): SecurityCheckResult {
        val activeCount = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            cm.allNetworks.count { network ->
                cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to check VPN status", e)
            return UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(e.toErrorKind()),
            )
        }

        if (activeCount == 0) {
            return SecureImpl(checkId = id)
        }
        return VpnThreatImpl(activeNetworkCount = activeCount)
    }

    companion object {
        private const val TAG = "VpnCheck"
    }
}
