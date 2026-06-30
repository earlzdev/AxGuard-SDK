package com.earldev.axguard.demo.domain.model

/**
 * Presentation-level outcome of a single security check, collapsing the SDK's richer
 * `SecurityCheckResult` hierarchy into the states the UI actually distinguishes.
 */
enum class CheckStatus {
    /** The check ran and found no threat. */
    SECURE,

    /** The check found a threat. */
    THREAT,

    /** The check exceeded the shared wall-clock budget. */
    TIMEOUT,

    /** The check is not observable on this device. */
    UNSUPPORTED,

    /** A probe failed before a verdict could be reached. */
    ERROR,
    ;

    /**
     * Sort weight used to surface the most actionable results first
     * (threats, then inconclusive, then secure).
     */
    val sortPriority: Int
        get() = when (this) {
            THREAT -> 0
            TIMEOUT, ERROR -> 1
            UNSUPPORTED -> 2
            SECURE -> 3
        }
}
