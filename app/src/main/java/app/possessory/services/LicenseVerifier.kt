package app.possessory.services

import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class LicenseClaims(
    val sku: String,
    val userId: String,
    val receiptId: String,
    val active: Boolean,
    val issuedAt: Long,
    val validUntil: Long,
    val environment: String = "production",
)

data class SignedLicense(val payload: String, val signature: String)

data class BillingState(
    val premium: Boolean = false,
    val price: String? = null,
    val busy: Boolean = false,
    val status: String = "unavailable",
)

object LicenseVerifier {
    fun verify(
        license: SignedLicense,
        publicKey: String,
        sku: String,
        now: Long = System.currentTimeMillis() / 1000,
        expectedEnvironment: String = "production",
    ): LicenseClaims {
        val payload = Base64.getUrlDecoder().decode(license.payload)
        val key =
            KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
        val valid =
            Signature.getInstance("SHA256withRSA")
                .apply {
                    initVerify(key)
                    update(payload)
                }
                .verify(Base64.getUrlDecoder().decode(license.signature))
        require(valid) { "invalid_license" }
        val claims = Files.gson.fromJson(String(payload, Charsets.UTF_8), LicenseClaims::class.java)
        require(
            claims.environment == expectedEnvironment &&
                claims.sku == sku &&
                claims.issuedAt <= now + 300 &&
                claims.validUntil > now &&
                claims.validUntil - claims.issuedAt <= 91L * 86400 &&
                claims.userId.isNotBlank() &&
                claims.receiptId.isNotBlank()
        ) {
            "invalid_license"
        }
        return claims
    }
}
