package com.axguard.sdk.internal.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.models.checks.UserCaThreatImpl
import com.axguard.sdk.internal.utils.toErrorKind
import java.security.KeyStore
import java.security.cert.X509Certificate

internal class UserCaCheck : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.USER_CA

    override fun run(): SecurityCheckResult {
        val userSubjects = mutableListOf<String>()
        var systemCaCount = 0

        try {
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                when {
                    alias.startsWith("user:") -> {
                        // Per-alias isolation: one unreadable cert must not abort the enumeration.
                        try {
                            val cert = keyStore.getCertificate(alias) as? X509Certificate ?: continue
                            userSubjects.add(cert.subjectX500Principal.name)
                        } catch (e: Exception) {
                            AxLog.w(TAG, "Failed to read user cert alias: $alias", e)
                        }
                    }
                    alias.startsWith("system:") -> systemCaCount++
                }
            }
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to enumerate CA certs", e)
            // A user cert read before the failure still counts; otherwise a failed
            // enumeration must not read as "no user CAs".
            if (userSubjects.isEmpty()) {
                return UnavailableImpl(
                    checkId = id,
                    reason = ErrorReasonImpl(e.toErrorKind()),
                )
            }
        }

        if (userSubjects.isEmpty()) {
            return SecureImpl(checkId = id)
        }

        return UserCaThreatImpl(
            subjects = userSubjects,
            systemCaCount = systemCaCount,
        )
    }

    companion object {
        private const val TAG = "UserCaCheck"
    }
}
