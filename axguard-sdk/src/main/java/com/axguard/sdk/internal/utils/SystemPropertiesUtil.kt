package com.axguard.sdk.internal.utils

import android.annotation.SuppressLint
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
internal object SystemPropertiesUtil {

    // Reflection resolved once; a failed lookup is cached as null so repeated
    // calls across checks don't pay ClassNotFoundException churn.
    private val getMethod: Method? by lazy {
        try {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private val getBooleanMethod: Method? by lazy {
        try {
            Class.forName("android.os.SystemProperties")
                .getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
        } catch (_: Exception) {
            null
        }
    }

    fun get(key: String): String? {
        // Prefer the native __system_property_get accessor: no reflection, no
        // Binder, and read from a site a Java-level hook can't intercept.
        NativeProps.get(key)?.let { return it }

        val method = getMethod ?: return null
        return try {
            val value = method.invoke(null, key) as? String
            if (value.isNullOrEmpty()) null else value
        } catch (_: Exception) {
            null
        }
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        val method = getBooleanMethod ?: return default
        return try {
            method.invoke(null, key, default) as? Boolean ?: default
        } catch (_: Exception) {
            default
        }
    }
}
