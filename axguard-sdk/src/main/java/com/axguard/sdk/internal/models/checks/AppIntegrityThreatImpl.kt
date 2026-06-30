package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.AppIntegrityThreat

internal data class AppIntegrityThreatImpl(
    override val actualFingerprints: List<String>,
) : AppIntegrityThreat {
    override val checkId: Int get() = SecurityCheckId.APP_INTEGRITY
}
