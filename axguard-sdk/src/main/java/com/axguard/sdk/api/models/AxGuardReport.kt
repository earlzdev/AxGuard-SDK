package com.axguard.sdk.api.models

import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.models.AxGuardReportImpl

/**
 * Outcome of a [com.axguard.sdk.api.AxGuardSdk.runChecks] call.
 */
public interface AxGuardReport {

    /**
     * One result per check that ran, ordered by [SecurityCheckId] ascending.
     */
    public val results: List<SecurityCheckResult>
}

internal fun AxGuardReport(
    results: List<SecurityCheckResult>,
): AxGuardReport {
    return AxGuardReportImpl(
        results = results,
    )
}
