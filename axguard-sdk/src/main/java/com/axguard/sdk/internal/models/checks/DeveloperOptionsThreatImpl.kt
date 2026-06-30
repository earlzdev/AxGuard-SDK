package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.DeveloperOptionsThreat

internal data class DeveloperOptionsThreatImpl(
    override val developerOptionsEnabled: Boolean,
    override val adbEnabled: Boolean,
) : DeveloperOptionsThreat {
    override val checkId: Int get() = SecurityCheckId.DEVELOPER_OPTIONS
}
