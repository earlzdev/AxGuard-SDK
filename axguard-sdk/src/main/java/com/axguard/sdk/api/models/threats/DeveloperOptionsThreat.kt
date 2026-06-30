package com.axguard.sdk.api.models.threats

/**
 * Developer-facing settings are enabled.
 */
public interface DeveloperOptionsThreat : SecurityCheckResult.ThreatDetected {

    /**
     * Developer Options is on.
     */
    public val developerOptionsEnabled: Boolean

    /**
     * USB debugging (adb) is on.
     */
    public val adbEnabled: Boolean
}
