package com.axguard.sdk.internal.checks

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.internal.log.AxLog
import com.axguard.sdk.api.models.threats.SecurityCheckResult
import com.axguard.sdk.internal.SecurityCheck
import com.axguard.sdk.internal.models.checks.EmulatorThreatImpl
import com.axguard.sdk.internal.models.checks.SecureImpl
import com.axguard.sdk.internal.utils.SystemPropertiesUtil
import java.io.File

internal class EmulatorCheck(
    private val context: Context,
) : SecurityCheck {

    override val id: @SecurityCheckId Int = SecurityCheckId.EMULATOR

    override fun run(): SecurityCheckResult {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val hw = Build.HARDWARE.lowercase()
        val product = Build.PRODUCT.lowercase()

        // Strong signals: no real device exhibits any of these, so one alone convicts.
        val emulatorHardware = EMULATOR_HARDWARE.any { hw == it || hw.startsWith(it) }
        val qemuDeviceFile = QEMU_DEVICE_FILES.any { File(it).exists() }
        val goldfishTtyDriver = checkProcTtyDrivers()
        val genymotionManufacturer = Build.MANUFACTURER.lowercase() == "genymotion"
        val emulatorModel = EMULATOR_MODELS.any { model.contains(it) }
        val qemuProperties = SystemPropertiesUtil.get("ro.kernel.qemu") == "1" ||
            SystemPropertiesUtil.get("ro.boot.qemu") == "1"

        // Weak signals: individually noisy (custom ROMs, broken HALs), so they
        // only count when several corroborate.
        val genericFingerprint = fp.contains("generic") || fp.contains("unknown")
        val genericBrand = Build.BRAND.lowercase() == "generic"
        val emulatorProduct = product.contains("sdk") || product.contains("emulator")
        val zeroBatteryTemp = checkBatteryTemp()
        val noAccelerometer = !hasAccelerometer()

        val strongSignals = listOf(
            emulatorHardware, qemuDeviceFile, goldfishTtyDriver,
            genymotionManufacturer, emulatorModel, qemuProperties,
        )
        val weakSignals = listOf(
            genericFingerprint, genericBrand, emulatorProduct,
            zeroBatteryTemp, noAccelerometer,
        )
        val detected = strongSignals.any { it } || weakSignals.count { it } >= WEAK_THRESHOLD
        if (!detected) return SecureImpl(id)

        return EmulatorThreatImpl(
            emulatorHardware = emulatorHardware,
            qemuDeviceFile = qemuDeviceFile,
            goldfishTtyDriver = goldfishTtyDriver,
            genymotionManufacturer = genymotionManufacturer,
            emulatorModel = emulatorModel,
            qemuProperties = qemuProperties,
            genericFingerprint = genericFingerprint,
            genericBrand = genericBrand,
            emulatorProduct = emulatorProduct,
            zeroBatteryTemp = zeroBatteryTemp,
            noAccelerometer = noAccelerometer,
        )
    }

    private fun hasAccelerometer(): Boolean {
        return try {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        } catch (e: Exception) {
            AxLog.e(TAG, "Failed to check accelerometer", e)
            // Fail toward "sensor present" so an error never fabricates a signal.
            true
        }
    }

    private fun checkBatteryTemp(): Boolean {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            temp == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun checkProcTtyDrivers(): Boolean {
        return try {
            File("/proc/tty/drivers").readText().contains("goldfish")
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "EmulatorCheck"

        // Weak signals needed to convict without a strong signal. 3 is empirical:
        // legitimate custom-ROM devices show up to 2 (generic build props + broken
        // battery HAL), so 3 keeps false positives negligible.
        private const val WEAK_THRESHOLD = 3

        private val QEMU_DEVICE_FILES = listOf("/dev/qemu_pipe", "/dev/goldfish_pipe", "/dev/vboxguest")
        private val EMULATOR_HARDWARE = listOf("goldfish", "ranchu", "vbox86", "nox")
        private val EMULATOR_MODELS = listOf("google_sdk", "emulator", "android sdk", "sdk_gphone")
    }
}
