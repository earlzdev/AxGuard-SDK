package com.axguard.sdk.api.models.threats

/**
 * At-rest storage protection is weak or absent.
 */
public interface EncryptionThreat : SecurityCheckResult.ThreatDetected {

    /**
     * Storage encryption is inactive.
     */
    public val storageEncryptionInactive: Boolean

    /**
     * Encrypted with a default key not bound to any user credential.
     */
    public val defaultKeyEncryption: Boolean

    /**
     * No secure lock screen, so credential-encrypted storage derives no protection.
     */
    public val noSecureLockScreen: Boolean
}
