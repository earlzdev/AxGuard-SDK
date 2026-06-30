package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.HookThreat

internal data class HookThreatImpl(
    override val suspiciousLibraryInMaps: Boolean,
    override val xposedBridgeLoaded: Boolean,
    override val fridaServerOnDisk: Boolean,
    override val standardFridaPortOpen: Boolean,
    override val fridaThreadDetected: Boolean,
) : HookThreat {
    override val checkId: Int get() = SecurityCheckId.HOOK
}
