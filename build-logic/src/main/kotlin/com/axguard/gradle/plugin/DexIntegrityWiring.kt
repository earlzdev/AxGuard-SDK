package com.axguard.gradle.plugin

import com.android.build.api.artifact.impl.ArtifactsImpl
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.InternalMultipleArtifactType
import com.axguard.gradle.tasks.AxGuardDexHashTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

/**
 * Wires the dex-integrity hash task for one variant: final dex directories →
 * [AxGuardDexHashTask] → generated asset merged into the APK.
 *
 * Kept in its own file because the dex artifacts are only reachable through
 * internal AGP types (`ArtifactsImpl`, `InternalMultipleArtifactType.DEX`,
 * `InternalArtifactType.DESUGAR_LIB_DEX` / `GLOBAL_SYNTHETICS_DEX` — the same
 * set `PackageAndroidArtifact.getDexFolders` packages) with no stability
 * guarantee: they are classloaded only when the feature is enabled, and any
 * incompatibility with the consumer's AGP version lands in the catch below —
 * the asset is then not injected and the opted-in runtime check fails closed.
 */
internal fun wireDexIntegrity(project: Project, variant: ApplicationVariant, capitalized: String) {
    val android = project.extensions.getByType(ApplicationExtension::class.java)
    if (android.dynamicFeatures.isNotEmpty()) {
        project.logger.warn(
            "axguard: dexIntegrity does not support dynamic-feature apps (packaging uses a " +
                "different dex path) — hash asset not injected for variant ${variant.name}."
        )
        return
    }

    val dexDirs: Provider<List<Directory>>
    val desugarLibDex: Provider<Directory>
    val globalSyntheticsDex: Provider<Directory>
    try {
        val artifacts = variant.artifacts as ArtifactsImpl
        dexDirs = artifacts.getAll(InternalMultipleArtifactType.DEX)
        desugarLibDex = artifacts.get(InternalArtifactType.DESUGAR_LIB_DEX)
        globalSyntheticsDex = artifacts.get(InternalArtifactType.GLOBAL_SYNTHETICS_DEX)
    } catch (t: Throwable) {
        project.logger.warn(
            "axguard: dexIntegrity is enabled but this AGP version's internal dex artifacts " +
                "are unavailable (${t.javaClass.simpleName}) — hash asset not injected for " +
                "variant ${variant.name}; the runtime check will fail closed."
        )
        return
    }

    val assets = variant.sources.assets
    if (assets == null) {
        project.logger.warn(
            "axguard: variant ${variant.name} has no assets source set — dex hash not injected."
        )
        return
    }

    val hashTask = project.tasks.register(
        "hash${capitalized}AxguardDex",
        AxGuardDexHashTask::class.java,
    ) { task ->
        task.dexDirs.set(dexDirs)
        task.desugarLibDex.set(desugarLibDex)
        task.globalSyntheticsDex.set(globalSyntheticsDex)
    }
    assets.addGeneratedSourceDirectory(hashTask, AxGuardDexHashTask::outputDir)
}
