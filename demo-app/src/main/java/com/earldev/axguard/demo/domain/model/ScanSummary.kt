package com.earldev.axguard.demo.domain.model

/** Aggregate outcome of a full scan, derived from the individual [SecurityCheck]s. */
data class ScanSummary(
    val total: Int,
    val secure: Int,
    val threats: Int,
    val inconclusive: Int,
) {
    val verdict: Verdict = when {
        threats > 0 -> Verdict.AT_RISK
        inconclusive > 0 -> Verdict.INCONCLUSIVE
        else -> Verdict.PROTECTED
    }

    enum class Verdict { PROTECTED, AT_RISK, INCONCLUSIVE }

    companion object {
        fun from(checks: List<SecurityCheck>): ScanSummary {
            val threats = checks.count { it.status == CheckStatus.THREAT }
            val secure = checks.count { it.status == CheckStatus.SECURE }
            return ScanSummary(
                total = checks.size,
                secure = secure,
                threats = threats,
                inconclusive = checks.size - secure - threats,
            )
        }
    }
}
