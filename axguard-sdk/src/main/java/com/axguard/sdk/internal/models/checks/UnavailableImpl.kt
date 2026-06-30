package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckErrorKind
import com.axguard.sdk.api.models.threats.SecurityCheckResult

internal data class UnavailableImpl(
    override val checkId: @SecurityCheckId Int,
    override val reason: SecurityCheckResult.Unavailable.Reason,
) : SecurityCheckResult.Unavailable

internal data class ErrorReasonImpl(
    override val kind: @SecurityCheckErrorKind Int,
) : SecurityCheckResult.Unavailable.Reason.Error
