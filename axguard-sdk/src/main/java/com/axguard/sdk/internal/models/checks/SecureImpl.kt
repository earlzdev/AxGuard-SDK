package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckResult

internal data class SecureImpl(
    override val checkId: @SecurityCheckId Int,
) : SecurityCheckResult.Secure
