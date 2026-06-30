package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.SelinuxThreat

internal object SelinuxThreatImpl : SelinuxThreat {
    override val checkId: Int get() = SecurityCheckId.SELINUX
}
