package com.earldev.axguard.demo.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.axguard.sdk.api.models.SecurityCheckId
import com.earldev.axguard.demo.domain.model.CheckStatus

/** Maps a check id to the icon that represents it on the row and in the detail sheet. */
fun checkIcon(id: Int): ImageVector = when (id) {
    SecurityCheckId.ROOT -> Icons.Filled.AdminPanelSettings
    SecurityCheckId.DEBUGGER -> Icons.Filled.BugReport
    SecurityCheckId.HOOK -> Icons.Filled.Extension
    SecurityCheckId.SELINUX -> Icons.Filled.Security
    SecurityCheckId.VERIFIED_BOOT -> Icons.Filled.VerifiedUser
    SecurityCheckId.EMULATOR -> Icons.Filled.Smartphone
    SecurityCheckId.ENCRYPTION -> Icons.Filled.Lock
    SecurityCheckId.APP_INTEGRITY -> Icons.Filled.Fingerprint
    SecurityCheckId.ADB_OVER_NETWORK -> Icons.Filled.Cable
    SecurityCheckId.DEVELOPER_OPTIONS -> Icons.Filled.DeveloperMode
    SecurityCheckId.USER_CA -> Icons.Filled.Policy
    SecurityCheckId.VPN -> Icons.Filled.VpnKey
    SecurityCheckId.PROXY -> Icons.Filled.Router
    SecurityCheckId.DEX_INTEGRITY -> Icons.Filled.Code
    else -> Icons.Filled.Shield
}

/** Small badge icon expressing the outcome of a check. */
fun statusIcon(status: CheckStatus): ImageVector = when (status) {
    CheckStatus.SECURE -> Icons.Filled.CheckCircle
    CheckStatus.THREAT -> Icons.Filled.GppBad
    CheckStatus.TIMEOUT -> Icons.Outlined.Timer
    CheckStatus.ERROR -> Icons.Filled.Warning
    CheckStatus.UNSUPPORTED -> Icons.Filled.HelpOutline
}

/** Short, all-caps label for a status pill. */
fun statusLabel(status: CheckStatus): String = when (status) {
    CheckStatus.SECURE -> "SECURE"
    CheckStatus.THREAT -> "THREAT"
    CheckStatus.TIMEOUT -> "TIMEOUT"
    CheckStatus.ERROR -> "ERROR"
    CheckStatus.UNSUPPORTED -> "N/A"
}
