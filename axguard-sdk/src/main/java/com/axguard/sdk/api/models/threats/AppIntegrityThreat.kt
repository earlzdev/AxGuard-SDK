package com.axguard.sdk.api.models.threats

/**
 * The APK signing certificate failed integrity verification (possible repackaging).
 */
public interface AppIntegrityThreat : SecurityCheckResult.ThreatDetected {

    /**
     * Why verification failed.
     */
    public val reason: Reason

    /**
     * The reason an app-integrity check reports a threat.
     */
    public sealed interface Reason {

        /**
         * No expected fingerprint was found in the APK — the build-time baseline
         * was stripped or the check was requested without the plugin injecting it.
         */
        public object BaselineMissing : Reason

        /**
         * The current signer did not match the expected fingerprint, or the
         * signing certificate could not be read.
         */
        public object SignerMismatch : Reason

        private object Stub : Reason
    }
}
