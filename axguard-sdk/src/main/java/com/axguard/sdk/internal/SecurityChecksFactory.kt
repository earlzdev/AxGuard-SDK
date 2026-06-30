package com.axguard.sdk.internal

import android.content.Context
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.checks.AdbOverNetworkCheck
import com.axguard.sdk.internal.checks.AppIntegrityCheck
import com.axguard.sdk.internal.checks.DebuggerCheck
import com.axguard.sdk.internal.checks.DeveloperOptionsCheck
import com.axguard.sdk.internal.checks.EmulatorCheck
import com.axguard.sdk.internal.checks.EncryptionCheck
import com.axguard.sdk.internal.checks.HookCheck
import com.axguard.sdk.internal.checks.ProxyCheck
import com.axguard.sdk.internal.checks.RootCheck
import com.axguard.sdk.internal.checks.SelinuxCheck
import com.axguard.sdk.internal.checks.UserCaCheck
import com.axguard.sdk.internal.checks.VerifiedBootCheck
import com.axguard.sdk.internal.checks.VpnCheck

internal class SecurityChecksFactory {

    @Throws
    fun createChecks(context: Context, checkIds: Set<@SecurityCheckId Int>): List<SecurityCheck> {
        return checkIds
            .ifEmpty { ALL_AVAILABLE_SECURITY_CHECK_IDS }
            .map { createSecurityCheckById(id = it, context = context) }
    }

    private fun createSecurityCheckById(id: Int, context: Context): SecurityCheck {
        return when (id) {
            SecurityCheckId.ROOT -> RootCheck()
            SecurityCheckId.DEBUGGER -> DebuggerCheck(context)
            SecurityCheckId.HOOK -> HookCheck()
            SecurityCheckId.SELINUX -> SelinuxCheck()
            SecurityCheckId.VERIFIED_BOOT -> VerifiedBootCheck()
            SecurityCheckId.EMULATOR -> EmulatorCheck(context)
            SecurityCheckId.ENCRYPTION -> EncryptionCheck(context)
            SecurityCheckId.APP_INTEGRITY -> AppIntegrityCheck(context)
            SecurityCheckId.ADB_OVER_NETWORK -> AdbOverNetworkCheck(context)
            SecurityCheckId.DEVELOPER_OPTIONS -> DeveloperOptionsCheck(context)
            SecurityCheckId.USER_CA -> UserCaCheck()
            SecurityCheckId.VPN -> VpnCheck(context)
            SecurityCheckId.PROXY -> ProxyCheck(context)
            else -> throw IllegalArgumentException("Unknown security check id: $id")
        }
    }

    companion object {
        private val ALL_AVAILABLE_SECURITY_CHECK_IDS = setOf(
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
    }
}
