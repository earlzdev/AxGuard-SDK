package com.axguard.sdk.internal.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.axguard.sdk.internal.log.AxLog
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

internal object KeyAttestationUtil {

    private const val TAG = "KeyAttestationUtil"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17"

    /**
     * Generates an ephemeral EC key with an attestation challenge and reads
     * RootOfTrust.verifiedBootState from the leaf certificate, then deletes the key.
     * The chain is NOT validated up to a Google root — this is a local heuristic,
     * not a Play Integrity replacement.
     *
     * @return the state, or null when the device has no usable hardware attestation.
     */
    fun readVerifiedBootState(): AttestationDerParser.VerifiedBootState? {
        val random = SecureRandom()
        val alias = "axguard-attest-${random.nextLong().toULong()}"
        val keyStore = try {
            KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        } catch (e: Exception) {
            AxLog.w(TAG, "AndroidKeyStore unavailable", e)
            return null
        }
        return try {
            val challenge = ByteArray(16).also(random::nextBytes)
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE)
                .apply { initialize(spec) }
                .generateKeyPair()

            val leaf = keyStore.getCertificateChain(alias)?.firstOrNull() as? X509Certificate
                ?: return null
            val extension = leaf.getExtensionValue(ATTESTATION_EXTENSION_OID) ?: return null
            AttestationDerParser.parseVerifiedBootState(extension)
        } catch (e: Exception) {
            AxLog.w(TAG, "Key attestation not available", e)
            null
        } finally {
            try {
                keyStore.deleteEntry(alias)
            } catch (_: Exception) {
                // best-effort cleanup of the ephemeral key
            }
        }
    }
}
