package com.axguard.sdk.api

import androidx.annotation.WorkerThread
import com.axguard.sdk.api.AxGuardSdk.Companion.getInstance
import com.axguard.sdk.api.models.AxGuardReport
import com.axguard.sdk.api.models.SecurityCheckConfig
import com.axguard.sdk.internal.AxGuardSdkImpl

/**
 * Entry point of the SDK. Obtain the instance via [getInstance] and run checks with [runChecks].
 */
public interface AxGuardSdk {

    /**
     * Runs the configured security checks and returns their outcomes.
     *
     * Per-check failures (probe errors, timeouts, unsupported states) surface
     * inside the report as [com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable];
     * only the two cases below are thrown.
     *
     * Blocking; call off the main thread. Thread-safe and re-entrant, but
     * concurrent calls share a bounded worker pool, so a check may spend part of
     * its budget waiting for capacity and surface as
     * [com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable.Reason.Timeout].
     *
     * @param config the checks to run and the shared wall-clock budget.
     * @return the report holding one result per requested check, ordered by check id.
     * @throws IllegalArgumentException if [config] names an unknown check id.
     * @throws InterruptedException if the calling thread is interrupted while the checks run.
     */
    @WorkerThread
    @Throws(
        InterruptedException::class,
        IllegalArgumentException::class,
    )
    public fun runChecks(
        config: SecurityCheckConfig,
    ): AxGuardReport

    public companion object {
        /**
         * Returns the shared [AxGuardSdk] instance.
         */
        public fun getInstance(): AxGuardSdk = AxGuardSdkImpl
    }
}
