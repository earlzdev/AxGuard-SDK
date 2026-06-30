package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.UserCaThreat

internal data class UserCaThreatImpl(
    override val subjects: List<String>,
    override val systemCaCount: Int,
) : UserCaThreat {
    override val checkId: Int get() = SecurityCheckId.USER_CA
}
