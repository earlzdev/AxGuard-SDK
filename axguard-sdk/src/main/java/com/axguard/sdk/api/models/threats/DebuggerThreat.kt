package com.axguard.sdk.api.models.threats

/**
 * A debugger or native tracer is attached to the process.
 */
public interface DebuggerThreat : SecurityCheckResult.ThreatDetected {

    /**
     * A JDWP (Java) debugger is connected.
     */
    public val jdwpConnected: Boolean

    /**
     * The process is suspended waiting for a debugger to attach.
     */
    public val waitingForDebugger: Boolean

    /**
     * A native tracer is attached (a thread has a non-zero TracerPid).
     */
    public val nativeAttached: Boolean

    /**
     * A thread sits in tracing-stop state.
     */
    public val anyThreadTraced: Boolean

    /**
     * The app manifest is debuggable; corroborating only, never fires alone.
     */
    public val appDebuggable: Boolean
}
