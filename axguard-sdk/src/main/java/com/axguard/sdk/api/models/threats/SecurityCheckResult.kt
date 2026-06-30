package com.axguard.sdk.api.models.threats

import com.axguard.sdk.api.models.SecurityCheckId

/**
 * Outcome of a single security check: [Secure], [ThreatDetected], or [Unavailable].
 */
public sealed interface SecurityCheckResult {

    /**
     * Id of the check that produced this result.
     */
    public val checkId: @SecurityCheckId Int

    /**
     * The check ran and found no threat.
     */
    public interface Secure : SecurityCheckResult

    /**
     * The check found a threat. Concrete subtypes carry the per-check detail.
     */
    public sealed interface ThreatDetected : SecurityCheckResult {

        private object Stub : ThreatDetected {
            override val checkId: @SecurityCheckId Int = -1
        }
    }

    /**
     * The check could not produce a verdict.
     */
    public interface Unavailable : SecurityCheckResult {

        /**
         * Why the check is unavailable.
         */
        public val reason: Reason

        /**
         * Reason a check is [Unavailable].
         */
        public sealed interface Reason {

            /**
             * The check did not finish within the report's wall-clock budget.
             */
            public object Timeout : Reason

            /**
             * The check is not observable on this device.
             */
            public object Unsupported : Reason

            /**
             * A probe failed; [kind] categorises the failure without exposing the underlying throwable.
             */
            public interface Error : Reason {
                /**
                 * Coarse failure category.
                 */
                public val kind: @SecurityCheckErrorKind Int
            }

            private object Stub : Reason
        }
    }

    private object Stub : SecurityCheckResult {
        override val checkId: @SecurityCheckId Int = -1
    }
}
