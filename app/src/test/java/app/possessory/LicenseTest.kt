package app.possessory

import app.possessory.services.*
import java.security.*
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class LicenseTest {
    @Test
    fun licenseRequiresSignatureMatchingSkuAndBoundedExpiry() {
        val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val key = Base64.getEncoder().encodeToString(keys.public.encoded)
        val claims = LicenseClaims("premium", "user", "receipt", true, 1000, 1000 + 86400)
        fun sign(value: LicenseClaims): SignedLicense {
            val bytes = Files.gson.toJson(value).toByteArray()
            val signature =
                Signature.getInstance("SHA256withRSA")
                    .apply {
                        initSign(keys.private)
                        update(bytes)
                    }
                    .sign()
            return SignedLicense(
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
                Base64.getUrlEncoder().withoutPadding().encodeToString(signature),
            )
        }
        val token = sign(claims)
        assertTrue(LicenseVerifier.verify(token, key, "premium", 1001).active)
        assertTrue(runCatching { LicenseVerifier.verify(token, key, "other", 1001) }.isFailure)
        assertTrue(
            runCatching {
                    LicenseVerifier.verify(
                        sign(claims.copy(environment = "sandbox")),
                        key,
                        "premium",
                        1001,
                    )
                }
                .isFailure
        )
        assertTrue(
            LicenseVerifier.verify(
                    sign(claims.copy(environment = "sandbox")),
                    key,
                    "premium",
                    1001,
                    expectedEnvironment = "sandbox",
                )
                .active
        )
        assertTrue(runCatching { LicenseVerifier.verify(token, key, "premium", 9999999) }.isFailure)
        assertTrue(
            runCatching {
                    LicenseVerifier.verify(
                        sign(claims.copy(validUntil = 1000 + 100 * 86400)),
                        key,
                        "premium",
                        1001,
                    )
                }
                .isFailure
        )
        assertTrue(
            runCatching {
                    LicenseVerifier.verify(
                        token.copy(payload = token.payload + "x"),
                        key,
                        "premium",
                        1001,
                    )
                }
                .isFailure
        )
        assertFalse(
            LicenseVerifier.verify(sign(claims.copy(active = false)), key, "premium", 1001).active
        )
    }
}
