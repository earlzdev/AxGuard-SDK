package com.axguard.sdk.internal.checks

import android.os.SystemClock
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.api.models.threats.SecurityCheckResult.Unavailable.Reason
import com.axguard.sdk.api.models.threats.VerifiedBootThreat
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.models.checks.VerifiedBootThreatImpl
import com.axguard.sdk.internal.utils.AttestationDerParser
import com.axguard.sdk.internal.utils.KeyAttestationUtil
import com.axguard.sdk.internal.utils.SystemPropertiesUtil

internal class VerifiedBootCheck : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.VERIFIED_BOOT

    override fun run(): SecurityCheckResult {
        return when (resolveState()) {
            RawState.GREEN   -> SecureImpl(id)
            RawState.YELLOW  -> VerifiedBootThreatImpl(state = VerifiedBootThreat.State.Yellow)
            RawState.ORANGE  -> VerifiedBootThreatImpl(state = VerifiedBootThreat.State.Orange)
            RawState.RED     -> VerifiedBootThreatImpl(state = VerifiedBootThreat.State.Red)
            RawState.UNKNOWN -> UnavailableImpl(id, Reason.Unsupported)
        }
    }

    private fun resolveState(): RawState {
        cachedState?.let { return it }
        return synchronized(cacheLock) {
            cachedState?.let { return@synchronized it }
            if (SystemClock.elapsedRealtime() < unknownUntilMs) return@synchronized RawState.UNKNOWN
            val observed = getVerifiedBootState()
            if (observed == RawState.UNKNOWN) {
                unknownUntilMs = SystemClock.elapsedRealtime() + UNKNOWN_TTL_MS
            } else {
                cachedState = observed
            }
            observed
        }
    }

    private fun getVerifiedBootState(): RawState {
        attestationState()?.let { return it }
        propertyState()?.let { return it }
        return RawState.UNKNOWN
    }

    private fun attestationState(): RawState? {
        return try {
            when (KeyAttestationUtil.readVerifiedBootState()) {
                AttestationDerParser.VerifiedBootState.VERIFIED    -> RawState.GREEN
                AttestationDerParser.VerifiedBootState.SELF_SIGNED -> RawState.YELLOW
                AttestationDerParser.VerifiedBootState.UNVERIFIED  -> RawState.ORANGE
                AttestationDerParser.VerifiedBootState.FAILED      -> RawState.RED
                null -> null
            }
        } catch (e: Exception) {
            AxLog.w(TAG, "Key attestation path failed", e)
            null
        }
    }

    private fun propertyState(): RawState? {
        return try {
            when (SystemPropertiesUtil.get("ro.boot.verifiedbootstate")?.lowercase()) {
                "green"  -> RawState.GREEN
                "yellow" -> RawState.YELLOW
                "orange" -> RawState.ORANGE
                "red"    -> RawState.RED
                else     -> null
            }
        } catch (e: Exception) {
            AxLog.w(TAG, "Failed to read verified boot property", e)
            null
        }
    }

    private enum class RawState {
        GREEN,
        YELLOW,
        ORANGE,
        RED,
        UNKNOWN,
    }

    companion object {
        private const val TAG = "VerifiedBootCheck"

        private const val UNKNOWN_TTL_MS = 60_000L

        private val cacheLock = Any()

        @Volatile
        private var cachedState: RawState? = null

        @Volatile
        private var unknownUntilMs: Long = 0L
    }
}
