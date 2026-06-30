package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.RootThreat

internal data class RootThreatImpl(
    override val suBinaryFound: Boolean,
    override val testKeysDetected: Boolean,
    override val adbRootProp: Boolean,
    override val insecureBuildProps: Boolean,
    override val suCommandAvailable: Boolean,
    override val systemPartitionRw: Boolean,
    override val magiskDetected: Boolean,
    override val kernelSuDetected: Boolean,
) : RootThreat {
    override val checkId: Int get() = SecurityCheckId.ROOT
}
