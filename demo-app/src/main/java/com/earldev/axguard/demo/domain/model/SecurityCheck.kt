package com.earldev.axguard.demo.domain.model

/**
 * A single security check, ready for presentation: static reference material from the
 * [CheckCatalog] merged with the runtime outcome produced by the SDK.
 */
data class SecurityCheck(
    val id: Int,
    val metadata: CheckMetadata,
    val status: CheckStatus,
    /** One-line, outcome-specific explanation (e.g. "No root indicators found"). */
    val statusHeadline: String,
    /** Individual indicators the check evaluated, with whether each one fired. */
    val signals: List<CheckSignal>,
    /** Free-form supporting values worth surfacing (ports, fingerprints, subjects, …). */
    val evidence: List<Evidence>,
)

/**
 * A named binary indicator within a check, e.g. "su binary found" or "JDWP connected".
 * [triggered] marks whether this specific indicator contributed to a threat verdict.
 */
data class CheckSignal(
    val label: String,
    val triggered: Boolean,
)

/** A supporting key/value observation, such as a proxy host or certificate subject. */
data class Evidence(
    val label: String,
    val value: String,
)
