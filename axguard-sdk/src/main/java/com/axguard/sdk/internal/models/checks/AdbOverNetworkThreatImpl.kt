package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.AdbOverNetworkThreat

internal data class AdbOverNetworkThreatImpl(
    override val tcpPortSet: Boolean,
    override val wifiAdbEnabled: Boolean,
    override val portReachable: Boolean,
    override val tcpPort: String?,
) : AdbOverNetworkThreat {

    override val checkId: Int
        get() = SecurityCheckId.ADB_OVER_NETWORK
}
