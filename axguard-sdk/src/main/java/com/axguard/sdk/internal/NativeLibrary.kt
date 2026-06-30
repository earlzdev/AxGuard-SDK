package com.axguard.sdk.internal

import com.axguard.sdk.internal.log.AxLog

/**
 * Single load site for the native detection library. [loaded] resolves once at
 * class-init and is false when the .so is absent (dropped ABI, stripped libs).
 * Callers must treat false as "probe unavailable" and fall back to Kotlin — the
 * native path is a hardening of evidence gathering, never the sole verdict.
 */
internal object NativeLibrary {

    private const val TAG = "AxGuardNative"
    private const val LIB_NAME = "axguard-integrity"

    val loaded: Boolean = try {
        System.loadLibrary(LIB_NAME)
        true
    } catch (e: UnsatisfiedLinkError) {
        AxLog.w(TAG, "Native library not available: ${e.message}")
        false
    }
}
