package com.axguard.sdk.internal.checks

import android.content.Context
import android.os.Build
import android.os.NetworkOnMainThreadException
import android.provider.Settings
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.AdbOverNetworkThreatImpl
import com.axguard.sdk.internal.models.checks.ErrorReasonImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.models.checks.UnavailableImpl
import com.axguard.sdk.internal.utils.SystemPropertiesUtil
import com.axguard.sdk.internal.utils.toErrorKind
import java.net.InetSocketAddress
import java.net.Socket

internal class AdbOverNetworkCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.ADB_OVER_NETWORK

    override fun run(): SecurityCheckResult {
        var error: Throwable? = null
        var tcpPortSet = false
        var tcpPort: String? = null
        var wifiAdbEnabled = false
        var portReachable = false

        try {
            // persist.adb.tcp.port survives reboots and is honored by adbd even
            // before service.adb.tcp.port is mirrored (early boot).
            val port = SystemPropertiesUtil.get("service.adb.tcp.port")
                ?: SystemPropertiesUtil.get("persist.adb.tcp.port")
            if (!port.isNullOrEmpty() && port != "0" && port != "-1") {
                tcpPortSet = true
                tcpPort = port
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiAdbEnabled = Settings.Global.getInt(
                    /* cr = */ context.contentResolver,
                    /* name = */ "adb_wifi_enabled",
                    /* def = */ 0,
                ) == 1
            }
            // Probe the configured port when known; 5555 is only the legacy default.
            // Wireless debugging binds a random port, detectable via the setting only.
            val probePort = tcpPort?.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_ADB_TCP_PORT
            portReachable = try {
                Socket().use { socket ->
                    socket.connect(
                        /* endpoint = */ InetSocketAddress(
                            /* hostname = */ "127.0.0.1",
                            /* port = */ probePort,
                        ),
                        /* timeout = */ CONNECT_TIMEOUT_MS,
                    )
                }
                true
            } catch (e: NetworkOnMainThreadException) {
                // Threading-contract violation, not evidence the port is closed.
                error = e
                false
            } catch (_: Exception) {
                false
            }
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to check ADB over network", e)
            error = e
        }

        return when {
            tcpPortSet || wifiAdbEnabled || portReachable -> AdbOverNetworkThreatImpl(
                tcpPortSet = tcpPortSet,
                wifiAdbEnabled = wifiAdbEnabled,
                portReachable = portReachable,
                tcpPort = tcpPort,
            )
            error != null -> UnavailableImpl(id, ErrorReasonImpl(error.toErrorKind()))
            else -> SecureImpl(id)
        }
    }

    companion object {
        private const val TAG = "AdbOverNetworkCheck"
        private const val DEFAULT_ADB_TCP_PORT = 5555

        private const val CONNECT_TIMEOUT_MS = 200
    }
}
