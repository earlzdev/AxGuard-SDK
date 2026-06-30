package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.ProxyThreat

internal data class ProxyThreatImpl(
    override val http: ProxyThreat.HttpProxy?,
    override val pac: ProxyThreat.PacProxy?,
    override val perNetworkProxyPresent: Boolean,
    override val proxySystemPropertySet: Boolean,
) : ProxyThreat {
    override val checkId: Int get() = SecurityCheckId.PROXY
}

internal data class HttpProxyImpl(
    override val host: String,
    override val port: Int,
) : ProxyThreat.HttpProxy

internal data class PacProxyImpl(
    override val scriptUrl: String,
) : ProxyThreat.PacProxy
