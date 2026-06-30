package com.axguard.sdk.api.models.threats

public interface RootThreat : SecurityCheckResult.ThreatDetected {

    public val suBinaryFound: Boolean

    public val testKeysDetected: Boolean

    public val adbRootProp: Boolean

    public val insecureBuildProps: Boolean

    public val suCommandAvailable: Boolean

    public val systemPartitionRw: Boolean

    public val magiskDetected: Boolean

    public val kernelSuDetected: Boolean
}
