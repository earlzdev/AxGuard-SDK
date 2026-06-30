package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.VerifiedBootThreat

internal data class VerifiedBootThreatImpl(
    override val state: VerifiedBootThreat.State,
) : VerifiedBootThreat {
    override val checkId: Int get() = SecurityCheckId.VERIFIED_BOOT
}
