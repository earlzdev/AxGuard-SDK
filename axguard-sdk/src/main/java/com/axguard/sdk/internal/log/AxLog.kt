package com.axguard.sdk.internal.log

import com.axguard.sdk.BuildConfig

/**
 * Single log sink for the SDK. In release BuildConfig.DEBUG is false, so R8 folds
 * each guard away and the android.util.Log call is dead-code-eliminated — no
 * check name or probe outcome reaches release logcat. Every SDK log must go
 * through here; a direct android.util.Log call would leak the detection pipeline.
 */
internal object AxLog {

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) android.util.Log.d(tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) android.util.Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) android.util.Log.e(tag, message, throwable)
    }
}
