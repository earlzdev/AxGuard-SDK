package com.axguard.sdk.api.models.threats

import androidx.annotation.IntDef

/**
 * `@IntDef` categorising a [SecurityCheckResult.Unavailable.Reason.Error]. The underlying
 * throwable is deliberately withheld, as a raw stack trace can embed device paths and other
 * identifying data.
 */
@IntDef(
    SecurityCheckErrorKind.IO,
    SecurityCheckErrorKind.SECURITY,
    SecurityCheckErrorKind.INTERRUPTED,
    SecurityCheckErrorKind.INTERNAL,
)
@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
)
@Retention(AnnotationRetention.SOURCE)
public annotation class SecurityCheckErrorKind {
    public companion object {

        /**
         * File, stream, or socket IO failed (missing node, EACCES, …).
         */
        public const val IO: Int = 0

        /**
         * The platform threw a SecurityException (permission not granted, …).
         */
        public const val SECURITY: Int = 1

        /**
         * The calling thread was interrupted while the probe ran.
         */
        public const val INTERRUPTED: Int = 2

        /**
         * Anything else: unexpected exception or internal invariant violation.
         */
        public const val INTERNAL: Int = 3
    }
}
