package com.axguard.sdk.api.models.threats

/**
 * User-installed CA certificates are present, enabling TLS interception.
 */
public interface UserCaThreat : SecurityCheckResult.ThreatDetected {

    /**
     * Subject DNs of the user-installed CAs. May embed identifying data.
     */
    public val subjects: List<String>

    /**
     * Number of system CAs, for context.
     */
    public val systemCaCount: Int
}
