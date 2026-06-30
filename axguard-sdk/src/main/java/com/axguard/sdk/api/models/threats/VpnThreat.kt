package com.axguard.sdk.api.models.threats

/**
 * A VPN transport is active.
 */
public interface VpnThreat : SecurityCheckResult.ThreatDetected {

    /**
     * Number of active networks carrying a VPN transport.
     */
    public val activeNetworkCount: Int
}
