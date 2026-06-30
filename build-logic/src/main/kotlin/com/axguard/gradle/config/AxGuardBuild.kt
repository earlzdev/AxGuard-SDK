package com.axguard.gradle.config

/**
 * Single source of truth for AxGuard build settings shared across modules:
 * published coordinates, SDK/NDK levels, and the obfuscation key. The
 * convention plugin ([com.axguard.gradle.plugin.AxGuardLibraryPlugin]) and the
 * consumer plugin both read from here so versions and keys never drift.
 */
object AxGuardBuild {

    const val GROUP = "io.github.earlzdev"
    const val VERSION = "0.1.1"

    const val COMPILE_SDK = 34
    const val MIN_SDK = 26
    const val NDK_VERSION = "28.2.13676358"
    const val JAVA_VERSION = 11

    /**
     * XOR key shared by the native OBFS() macro, the Kotlin Obfs decoder, and this plugin.
     */
    const val OBFS_KEY = 0x5A

    /**
     * `0x5A`-style hex literal used in generated BuildConfig / CMake defines.
     */
    val obfsKeyHex: String get() = "0x${OBFS_KEY.toString(16).uppercase()}"
}
