package com.axguard.sdk.internal.models.checks

import com.axguard.sdk.api.models.SecurityCheckId
import com.axguard.sdk.api.models.threats.EmulatorThreat

internal data class EmulatorThreatImpl(
    override val emulatorHardware: Boolean,
    override val qemuDeviceFile: Boolean,
    override val goldfishTtyDriver: Boolean,
    override val genymotionManufacturer: Boolean,
    override val emulatorModel: Boolean,
    override val qemuProperties: Boolean,
    override val genericFingerprint: Boolean,
    override val genericBrand: Boolean,
    override val emulatorProduct: Boolean,
    override val zeroBatteryTemp: Boolean,
    override val noAccelerometer: Boolean,
) : EmulatorThreat {
    override val checkId: Int get() = SecurityCheckId.EMULATOR
}
