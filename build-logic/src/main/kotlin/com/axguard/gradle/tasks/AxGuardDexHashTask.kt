package com.axguard.gradle.tasks

import com.axguard.gradle.internal.combinedDexHash
import com.axguard.gradle.internal.obfuscateToBase64
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Hashes the variant's final dex files and writes the obfuscated combined hash
 * as a generated asset ([ASSET_PATH]) that `DexIntegrityCheck` in the SDK
 * verifies at runtime.
 *
 * The inputs mirror `PackageAndroidArtifact.getDexFolders`: the main DEX
 * directories plus the desugar-lib and global-synthetics dex when present —
 * missing any of them would make release builds report a false threat.
 */
abstract class AxGuardDexHashTask : DefaultTask() {

    /**
     * Final per-variant dex directories (InternalMultipleArtifactType.DEX).
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dexDirs: ListProperty<Directory>

    /**
     * DESUGAR_LIB_DEX — present only with core-library desugaring.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val desugarLibDex: DirectoryProperty

    /**
     * GLOBAL_SYNTHETICS_DEX — present on non-debuggable, non-minified builds.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val globalSyntheticsDex: DirectoryProperty

    /**
     * Generated assets root; the location is assigned by addGeneratedSourceDirectory.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val roots = dexDirs.get().map { it.asFile } +
            listOfNotNull(desugarLibDex.orNull?.asFile, globalSyntheticsDex.orNull?.asFile)
        val dexFiles = roots.filter { it.isDirectory }
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "dex" }.toList() }

        val assetFile = outputDir.get().asFile.resolve(ASSET_PATH)
        if (dexFiles.isEmpty()) {
            logger.warn(
                "axguard: no dex files found — dex-integrity asset not written; " +
                    "the runtime check will fail closed."
            )
            assetFile.delete()
            return
        }

        val combined = combinedDexHash(dexFiles)
        logger.info("axguard: combined dex hash for ${dexFiles.size} file(s) = $combined")
        assetFile.parentFile.mkdirs()
        assetFile.writeText(obfuscateToBase64(combined))
    }

    companion object {
        // Must match DexIntegrityCheck.ASSET_PATH in the SDK.
        internal const val ASSET_PATH = "axg/dx.bin"
    }
}
