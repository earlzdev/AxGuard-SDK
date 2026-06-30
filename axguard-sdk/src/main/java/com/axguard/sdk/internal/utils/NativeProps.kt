package com.axguard.sdk.internal.utils

import com.axguard.sdk.internal.NativeLibrary

/**
 * System-property accessor backed by libc's __system_property_get: no reflection,
 * no Binder, read from native code a Java-level hook can't rewrite. [get] returns
 * null when the library is absent so [SystemPropertiesUtil] can fall back to reflection.
 */
internal object NativeProps {

    val available: Boolean get() = NativeLibrary.loaded

    fun get(key: String): String? {
        if (!NativeLibrary.loaded) return null
        return try {
            nativeGet(key)?.takeIf { it.isNotEmpty() }
        } catch (e: UnsatisfiedLinkError) {
            null
        }
    }

    private external fun nativeGet(key: String): String?
}
