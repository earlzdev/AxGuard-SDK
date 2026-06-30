package com.earldev.axguard.demo.domain

import android.content.Context
import com.axguard.sdk.api.AxGuardSdk
import com.axguard.sdk.api.models.SecurityCheckConfig
import com.earldev.axguard.demo.domain.model.SecurityCheck
import com.earldev.axguard.demo.domain.model.toSecurityCheck
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain entry point for running the AxGuard SDK. Wraps the blocking, context-bound SDK
 * call in a suspend function and maps its report into domain [SecurityCheck]s (in the SDK's
 * canonical check-id order; the presentation layer decides display ordering).
 */
class SecurityChecker(
    private val context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun runAllChecks(): List<SecurityCheck> = withContext(io) {
        AxGuardSdk.getInstance()
            .runChecks(SecurityCheckConfig(context = context.applicationContext))
            .results
            .map { it.toSecurityCheck() }
    }
}
