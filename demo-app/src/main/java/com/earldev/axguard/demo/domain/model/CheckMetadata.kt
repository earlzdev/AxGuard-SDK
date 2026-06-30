package com.earldev.axguard.demo.domain.model

/**
 * Static, human-authored reference material describing what a check does and why it
 * matters. Independent of any run's outcome — see [CheckCatalog] for the values.
 */
data class CheckMetadata(
    val id: Int,
    val title: String,
    val category: CheckCategory,
    /** Short one-liner shown on the list row. */
    val tagline: String,
    /** What the check inspects. */
    val whatItChecks: String,
    /** Why an insecure result is dangerous. */
    val whyItMatters: String,
    /** Suggested response when the check is not clean. */
    val recommendation: String,
)

/**
 * Coarse grouping of checks by the class of threat they defend against. Drives grouping
 * and iconography in the UI.
 */
enum class CheckCategory(val displayName: String) {
    DEVICE_INTEGRITY("Device integrity"),
    TAMPERING("Tampering & instrumentation"),
    NETWORK("Network & interception"),
    CONFIGURATION("Device configuration"),
}
