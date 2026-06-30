package com.axguard.gradle.tasks

import com.axguard.gradle.internal.normalizeFingerprint
import com.axguard.gradle.internal.obfuscateToBase64
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Transforms the app's merged AndroidManifest, injecting a `<meta-data>` element
 * that carries the obfuscated expected signing-certificate fingerprint. The
 * prebuilt AxGuard SDK reads it at runtime (via PackageManager) for its App
 * Integrity check — a prebuilt AAR can't know the consumer's signing cert, so
 * the value has to travel in through the consumer's build.
 *
 * When no fingerprint is configured the manifest passes through untouched, so
 * the SDK reports the check Unsupported rather than failing.
 */
abstract class AxGuardConfigurationTask : DefaultTask() {

    /**
     * Expected SHA-256 signing fingerprint (hex; colons/whitespace optional).
     */
    @get:Input
    @get:Optional
    abstract val fingerprint: Property<String>

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    init {
        group = "axguard"
        description = "Injects the expected signing fingerprint into the merged manifest."
    }

    @TaskAction
    fun run() {
        val input = mergedManifest.get().asFile
        val output = updatedManifest.get().asFile

        val normalized = fingerprint.orNull?.let { normalizeFingerprint(it) }?.takeIf { it.isNotEmpty() }
        if (normalized == null) {
            input.copyTo(output, overwrite = true)
            logger.info("axguard: no fingerprint configured — manifest left unchanged.")
            return
        }

        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(input)

        val application = doc.getElementsByTagName("application").item(0) as? Element
        if (application == null) {
            input.copyTo(output, overwrite = true)
            logger.warn("axguard: <application> not found in merged manifest — left unchanged.")
            return
        }

        application.appendChild(
            doc.createElement("meta-data").apply {
                setAttributeNS(ANDROID_NS, "android:name", META_NAME)
                setAttributeNS(ANDROID_NS, "android:value", obfuscateToBase64(normalized))
            }
        )

        TransformerFactory.newInstance().newTransformer()
            .apply { setOutputProperty(OutputKeys.ENCODING, "UTF-8") }
            .transform(DOMSource(doc), StreamResult(output))

        logger.info("axguard: injected expected fingerprint into manifest meta-data.")
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

        // Manifest meta-data key. Must match AppIntegrityCheck.META_NAME in the SDK.
        internal const val META_NAME = "com.axguard.sdk.EXPECTED_FP"
    }
}
