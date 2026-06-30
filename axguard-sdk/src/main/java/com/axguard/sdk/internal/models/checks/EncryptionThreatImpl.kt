package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.EncryptionThreat

internal data class EncryptionThreatImpl(
    override val storageEncryptionInactive: Boolean,
    override val defaultKeyEncryption: Boolean,
    override val noSecureLockScreen: Boolean,
) : EncryptionThreat {
    override val checkId: Int get() = SecurityCheckId.ENCRYPTION
}
