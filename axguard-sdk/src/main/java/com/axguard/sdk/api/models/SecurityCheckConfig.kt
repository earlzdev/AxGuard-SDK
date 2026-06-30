package com.axguard.sdk.api.models

import android.content.Context

/**
 * Configuration for a [com.axguard.sdk.api.AxGuardSdk.runChecks] call. Build it with the [SecurityCheckConfig] factory.
 */
public sealed interface SecurityCheckConfig {

    /**
     * Context used by the checks; the application context is retained.
     */
    public val context: Context

    /**
     * Ids of the checks to run; empty means all available checks.
     */
    public val checkIds: Set<@SecurityCheckId Int>

    /**
     * Shared wall-clock budget across every check; on expiry, running checks surface as Unavailable/Timeout.
     */
    public val timeoutMs: Long
}

internal class SecurityCheckConfigImpl(
    override val context: Context,
    override val checkIds: Set<@SecurityCheckId Int>,
    override val timeoutMs: Long,
) : SecurityCheckConfig

/**
 * Default wall-clock budget used when none is given.
 */
public const val DEFAULT_TIMEOUT_MS: Long = 10_000L

/**
 * Creates a [SecurityCheckConfig].
 *
 * @param context any context; the application context is retained so the config can outlive its creator.
 * @param checkIds ids to run, or empty to run all available checks.
 * @param timeoutMs shared wall-clock budget in milliseconds; must be positive.
 * @throws IllegalArgumentException if [timeoutMs] is not positive.
 */
public fun SecurityCheckConfig(
    context: Context,
    checkIds: Set<@SecurityCheckId Int> = emptySet(),
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
): SecurityCheckConfig {
    require(timeoutMs > 0) { "timeoutMs must be positive, was $timeoutMs" }
    return SecurityCheckConfigImpl(
        context = context.applicationContext,
        checkIds = checkIds,
        timeoutMs = timeoutMs,
    )
}
