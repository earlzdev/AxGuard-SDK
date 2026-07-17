package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.DexIntegrityThreat

internal data class DexIntegrityThreatImpl(
    override val reason: DexIntegrityThreat.Reason,
) : DexIntegrityThreat {

    override val checkId: Int
        get() = SecurityCheckId.DEX_INTEGRITY
}
