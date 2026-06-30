package com.axguard.sdk.api.models.threats

/**
 * The app appears to be running on an emulator. A strong signal convicts on its
 * own; weak signals convict only when several corroborate.
 */
public interface EmulatorThreat : SecurityCheckResult.ThreatDetected {

    /**
     * ro.hardware matches a known emulator (goldfish, ranchu, …).
     */
    public val emulatorHardware: Boolean

    /**
     * A QEMU/VirtualBox device node exists.
     */
    public val qemuDeviceFile: Boolean

    /**
     * The goldfish TTY driver is present.
     */
    public val goldfishTtyDriver: Boolean

    /**
     * Build.MANUFACTURER is Genymotion.
     */
    public val genymotionManufacturer: Boolean

    /**
     * Build.MODEL matches a known emulator image.
     */
    public val emulatorModel: Boolean

    /**
     * A QEMU system property is set.
     */
    public val qemuProperties: Boolean

    /**
     * Build fingerprint contains "generic"/"unknown". (weak)
     */
    public val genericFingerprint: Boolean

    /**
     * Build.BRAND is "generic". (weak)
     */
    public val genericBrand: Boolean

    /**
     * Build.PRODUCT looks like an SDK/emulator image. (weak)
     */
    public val emulatorProduct: Boolean

    /**
     * Battery temperature reads 0. (weak)
     */
    public val zeroBatteryTemp: Boolean

    /**
     * No accelerometer present. (weak)
     */
    public val noAccelerometer: Boolean
}
