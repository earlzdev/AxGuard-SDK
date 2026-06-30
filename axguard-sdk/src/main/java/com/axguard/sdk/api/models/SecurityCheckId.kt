package com.axguard.sdk.api.models

import androidx.annotation.IntDef

/**
 * `@IntDef` of the available security checks, used to select checks and to tag results.
 */
@IntDef(
    SecurityCheckId.ROOT,
    SecurityCheckId.DEBUGGER,
    SecurityCheckId.HOOK,
    SecurityCheckId.SELINUX,
    SecurityCheckId.VERIFIED_BOOT,
    SecurityCheckId.EMULATOR,
    SecurityCheckId.ENCRYPTION,
    SecurityCheckId.APP_INTEGRITY,
    SecurityCheckId.ADB_OVER_NETWORK,
    SecurityCheckId.DEVELOPER_OPTIONS,
    SecurityCheckId.USER_CA,
    SecurityCheckId.VPN,
    SecurityCheckId.PROXY,
)
@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
)
@Retention(AnnotationRetention.SOURCE)
public annotation class SecurityCheckId {
    public companion object {

        /**
         * Root or privilege-escalation framework.
         */
        public const val ROOT: Int = 0

        /**
         * Attached debugger or native tracer.
         */
        public const val DEBUGGER: Int = 1

        /**
         * Hooking/instrumentation framework (Frida, Xposed, …).
         */
        public const val HOOK: Int = 2

        /**
         * SELinux enforcing mode.
         */
        public const val SELINUX: Int = 3

        /**
         * Verified Boot state.
         */
        public const val VERIFIED_BOOT: Int = 4

        /**
         * Emulator environment.
         */
        public const val EMULATOR: Int = 5

        /**
         * At-rest storage encryption.
         */
        public const val ENCRYPTION: Int = 6

        /**
         * APK signing-certificate integrity.
         */
        public const val APP_INTEGRITY: Int = 7

        /**
         * ADB reachable over the network.
         */
        public const val ADB_OVER_NETWORK: Int = 8

        /**
         * Developer options / USB debugging.
         */
        public const val DEVELOPER_OPTIONS: Int = 9

        /**
         * User-installed CA certificates.
         */
        public const val USER_CA: Int = 10

        /**
         * Active VPN.
         */
        public const val VPN: Int = 11

        /**
         * HTTP proxy configured.
         */
        public const val PROXY: Int = 12
    }
}
