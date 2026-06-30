package com.axguard.sdk.internal.models

import com.axguard.sdk.api.models.AxGuardReport
import com.axguard.sdk.api.models.threats.SecurityCheckResult

internal data class AxGuardReportImpl(
    override val results: List<SecurityCheckResult>,
) : AxGuardReport
