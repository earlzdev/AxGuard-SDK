package com.axguard.sdk.api.models.threats

/**
 * The APK's `classes*.dex` files failed integrity verification (possible code
 * tampering or repackaging).
 */
public interface DexIntegrityThreat : SecurityCheckResult.ThreatDetected {

    /**
     * Why verification failed.
     */
    public val reason: Reason

    /**
     * The reason a DEX-integrity check reports a threat.
     */
    public sealed interface Reason {

        /**
         * No expected hash was found in the APK — the build-time baseline was
         * stripped or the check was requested without the plugin injecting it.
         */
        public object BaselineMissing : Reason

        /**
         * The `classes*.dex` hash does not match the build-time baseline.
         */
        public object HashMismatch : Reason

        private object Stub : Reason
    }
}
