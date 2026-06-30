package com.axguard.sdk.internal.checks

import android.content.Context
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.net.Uri
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.ProxyThreat
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.HttpProxyImpl
import com.axguard.sdk.internal.models.checks.PacProxyImpl
import com.axguard.sdk.internal.models.checks.ProxyThreatImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.toErrorKind

internal class ProxyCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.PROXY

    override fun run(): SecurityCheckResult {
        var error: Throwable? = null
        var http: ProxyThreat.HttpProxy? = null
        var pac: ProxyThreat.PacProxy? = null
        var perNetwork = false

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            cm.defaultProxy?.let { defaultProxy ->
                when {
                    defaultProxy.hasPacScript() -> {
                        pac = PacProxyImpl(defaultProxy.pacFileUrl.toString())
                    }
                    !defaultProxy.host.isNullOrEmpty() -> {
                        http = HttpProxyImpl(
                            host = defaultProxy.host!!,
                            port = defaultProxy.port,
                        )
                    }
                }
            }

            @Suppress("DEPRECATION")
            cm.allNetworks.forEach { network ->
                try {
                    val proxy = cm.getLinkProperties(network)?.httpProxy
                    if (proxy != null && (!proxy.host.isNullOrEmpty() || proxy.hasPacScript())) {
                        perNetwork = true
                    }
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to check proxy", e)
            error = e
        }

        // Injected code can set these directly to redirect HttpURLConnection/OkHttp
        // traffic without any system-level proxy existing.
        val proxySystemPropertySet =
            !System.getProperty("http.proxyHost").isNullOrEmpty() ||
                !System.getProperty("https.proxyHost").isNullOrEmpty()

        return when {
            http != null || pac != null || perNetwork || proxySystemPropertySet -> ProxyThreatImpl(
                http = http,
                pac = pac,
                perNetworkProxyPresent = perNetwork,
                proxySystemPropertySet = proxySystemPropertySet,
            )
            error != null -> UnavailableImpl(
                checkId = id,
                reason = ErrorReasonImpl(error.toErrorKind())
            )
            else -> SecureImpl(checkId = id)
        }
    }

    // getPacFileUrl() reports Uri.EMPTY (not null) when no PAC script is set, so a
    // plain host:port proxy must not be classified as a PAC proxy.
    private fun ProxyInfo.hasPacScript(): Boolean {
        return pacFileUrl != null && pacFileUrl != Uri.EMPTY
    }

    companion object {
        private const val TAG = "ProxyCheck"
    }
}
