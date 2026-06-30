package com.axguard.sdk.api.models.threats

/**
 * ADB is, or may be, reachable over the network.
 */
public interface AdbOverNetworkThreat : SecurityCheckResult.ThreatDetected {

    /**
     * An adb TCP port property is set.
     */
    public val tcpPortSet: Boolean

    /**
     * Wireless debugging is enabled.
     */
    public val wifiAdbEnabled: Boolean

    /**
     * The adb port accepts a loopback connection.
     */
    public val portReachable: Boolean

    /**
     * The configured adb TCP port, if known.
     */
    public val tcpPort: String?
}
