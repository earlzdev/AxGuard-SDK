package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.DebuggerThreat

internal data class DebuggerThreatImpl(
    override val jdwpConnected: Boolean,
    override val waitingForDebugger: Boolean,
    override val nativeAttached: Boolean,
    override val anyThreadTraced: Boolean,
    override val appDebuggable: Boolean,
) : DebuggerThreat {
    override val checkId: Int get() = SecurityCheckId.DEBUGGER
}
