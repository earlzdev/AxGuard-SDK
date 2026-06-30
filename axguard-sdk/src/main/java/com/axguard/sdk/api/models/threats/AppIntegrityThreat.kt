package com.axguard.sdk.api.models.threats

/**
 * The APK signing certificate does not match the expected fingerprint (possible repackaging).
 */
public interface AppIntegrityThreat : SecurityCheckResult.ThreatDetected {

    /**
     * SHA-256 hex fingerprints of the current signer(s).
     */
    public val actualFingerprints: List<String>
}
