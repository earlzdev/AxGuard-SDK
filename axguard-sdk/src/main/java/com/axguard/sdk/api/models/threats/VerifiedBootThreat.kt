package com.axguard.sdk.api.models.threats

/**
 * Verified Boot reports a non-green state (device integrity not fully assured).
 */
public interface VerifiedBootThreat : SecurityCheckResult.ThreatDetected {

    /**
     * The reported boot state.
     */
    public val state: State

    /**
     * Non-green Verified Boot states.
     */
    public sealed interface State {

        /**
         * Boot verified with a user-supplied key.
         */
        public object Yellow : State

        /**
         * Verified Boot disabled (typically an unlocked bootloader).
         */
        public object Orange : State

        /**
         * Boot verification failed (e.g. dm-verity corruption).
         */
        public object Red : State

        private object Stub : State
    }
}
