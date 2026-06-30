package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.VpnThreat

internal data class VpnThreatImpl(
    override val activeNetworkCount: Int,
) : VpnThreat {
    override val checkId: Int get() = SecurityCheckId.VPN
}
