package com.axguard.sdk.internal

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SecurityCheckResult

internal interface SecurityCheck {

    val id: @SecurityCheckId Int

    fun run(): SecurityCheckResult
}
