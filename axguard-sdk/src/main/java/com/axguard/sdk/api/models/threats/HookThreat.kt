package com.axguard.sdk.api.models.threats

/**
 * A hooking or instrumentation framework (Frida, Xposed, …) was detected.
 */
public interface HookThreat : SecurityCheckResult.ThreatDetected {

    /**
     * A known hooking-framework library is mapped in /proc/self/maps.
     */
    public val suspiciousLibraryInMaps: Boolean

    /**
     * An Xposed bridge class is loadable in the app classloader.
     */
    public val xposedBridgeLoaded: Boolean

    /**
     * A frida-server binary is present on disk.
     */
    public val fridaServerOnDisk: Boolean

    /**
     * A standard frida port (27042/27043) is open on loopback.
     */
    public val standardFridaPortOpen: Boolean

    /**
     * A frida worker thread is running in the process.
     */
    public val fridaThreadDetected: Boolean
}
