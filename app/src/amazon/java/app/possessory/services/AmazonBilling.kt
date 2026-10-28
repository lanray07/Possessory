package app.possessory.services

import androidx.datastore.preferences.core.*
import app.possessory.BuildConfig
import app.possessory.PossessoryApp
import app.possessory.settings
import com.amazon.device.drm.LicensingService
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import java.net.URL
import java.security.*
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** No entitlement is granted from an SDK callback alone. The server must verify RVS and sign it. */
class AmazonBilling(private val app: PossessoryApp) : PurchasingListener {
    val enabled =
        BuildConfig.AMAZON_SKU.isNotBlank() &&
            BuildConfig.VERIFIER_URL.startsWith("https://") &&
            BuildConfig.LICENSE_PUBLIC_KEY.isNotBlank()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val state = MutableStateFlow(BillingState())
    private var currentUser: String? = null
    private var tokenUser: String? = null
    private var tokenReceipt: String? = null
    private val cancelledReceipts = mutableSetOf<Pair<String, String>>()
    private var timeout: Job? = null
    private var started = false
    private var sdkEnvironmentMatches = false
    private val verificationMutex = Mutex()
    private var expiry: Job? = null
    private var clockAnchor = System.currentTimeMillis() / 1000
    private val elapsedAnchor = android.os.SystemClock.elapsedRealtime()

    private fun licenseTime(): Long =
        maxOf(
            System.currentTimeMillis() / 1000,
            clockAnchor + (android.os.SystemClock.elapsedRealtime() - elapsedAnchor) / 1000,
        )

    private fun expireAt(time: Long) {
        expiry?.cancel()
        expiry =
            scope.launch {
                delay(((time - licenseTime()).coerceAtLeast(0)) * 1000)
                state.value = state.value.copy(premium = false, status = "retry")
            }
    }

    fun start() {
        if (!enabled || started) return
        started = true
        scope.launch {
            val clockKey = longPreferencesKey("license_last_seen")
            clockAnchor = maxOf(clockAnchor, app.settings.data.first()[clockKey] ?: 0)
            app.settings.edit { it[clockKey] = licenseTime() }
            scope.launch {
                while (isActive) {
                    delay(60000)
                    app.settings.edit { it[clockKey] = licenseTime() }
                }
            }
            val stored = app.settings.data.first()[stringPreferencesKey("signed_license")]
            if (stored != null)
                runCatching {
                        LicenseVerifier.verify(
                            Files.gson.fromJson(stored, SignedLicense::class.java),
                            BuildConfig.LICENSE_PUBLIC_KEY,
                            BuildConfig.AMAZON_SKU,
                            now = licenseTime(),
                            expectedEnvironment = BuildConfig.BILLING_ENV,
                        )
                    }
                    .onSuccess {
                        tokenUser = it.userId
                        tokenReceipt = it.receiptId
                        state.value = state.value.copy(premium = it.active, status = "cached")
                        expireAt(it.validUntil)
                    }
            runCatching {
                    // SDK 3 initializes its environment through the licensing entry point.
                    // This free app does not use the DRM result to grant Premium.
                    LicensingService.verifyLicense(app) { }
                    PurchasingService.registerListener(app, this@AmazonBilling)
                    sdkEnvironmentMatches =
                        LicensingService.getAppstoreSDKMode() ==
                            (if (BuildConfig.BILLING_ENV == "sandbox") "SANDBOX" else "PRODUCTION")
                    check(sdkEnvironmentMatches) { "Unexpected store environment" }
                    PurchasingService.getUserData()
                    PurchasingService.getProductData(setOf(BuildConfig.AMAZON_SKU))
                    PurchasingService.getPurchaseUpdates(true)
                }
                .onFailure { state.value = state.value.copy(status = "unavailable", busy = false) }
        }
    }

    private fun pending() {
        timeout?.cancel()
        state.value = state.value.copy(busy = true, status = "pending")
        timeout =
            scope.launch {
                delay(60000)
                state.value = state.value.copy(busy = false, status = "retry")
            }
    }

    fun purchase() {
        if (
            !enabled ||
                !sdkEnvironmentMatches ||
                state.value.busy ||
                state.value.price == null ||
                state.value.premium
        )
            return
        pending()
        runCatching { PurchasingService.purchase(BuildConfig.AMAZON_SKU) }
            .onFailure { state.value = state.value.copy(busy = false, status = "retry") }
    }

    fun restore() {
        if (!enabled || !sdkEnvironmentMatches || state.value.busy) return
        pending()
        runCatching {
                PurchasingService.getUserData()
                PurchasingService.getPurchaseUpdates(true)
            }
            .onFailure { state.value = state.value.copy(busy = false, status = "retry") }
    }

    override fun onUserDataResponse(response: UserDataResponse) {
        if (response.requestStatus == UserDataResponse.RequestStatus.SUCCESSFUL) {
            currentUser = response.userData.userId
            if (tokenUser != null && tokenUser != currentUser) {
                state.value = state.value.copy(premium = false)
                scope.launch {
                    app.settings.edit { it.remove(stringPreferencesKey("signed_license")) }
                }
            }
        }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        val product =
            if (response.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL)
                response.productData[BuildConfig.AMAZON_SKU]
            else null
        state.value =
            state.value.copy(
                price = product?.price,
                  status = if (state.value.status == "revoked") "revoked"
                      else if (product != null) "ready" else "unavailable",
            )
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        timeout?.cancel()
        when (response.requestStatus) {
            PurchaseResponse.RequestStatus.SUCCESSFUL ->
                validate(response.receipt, response.userData.userId)
            PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                state.value = state.value.copy(busy = false)
                restore()
            }
            else -> state.value = state.value.copy(busy = false, status = "retry")
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        if (response.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            state.value = state.value.copy(busy = false, status = "retry")
            return
        }
        val receipts = response.receipts.filter { it.sku == BuildConfig.AMAZON_SKU }
        // One-time entitlement should have a single current receipt; process cancellation last.
        receipts.sortedBy { it.isCanceled }.forEach { validate(it, response.userData.userId) }
        if (response.hasMore()) PurchasingService.getPurchaseUpdates(false)
        else if (receipts.isEmpty())
            state.value = state.value.copy(busy = false, status = "checked")
    }

    private fun validate(receipt: Receipt, userId: String) {
        if (receipt.sku != BuildConfig.AMAZON_SKU) return
        val receiptKey = userId to receipt.receiptId
        if (receipt.isCanceled) cancelledReceipts.add(receiptKey)
        if (receiptKey in cancelledReceipts) {
            fun isCurrentEntitlement() =
                (tokenUser == null || tokenUser == userId) &&
                    (tokenReceipt == null || tokenReceipt == receipt.receiptId)
            // An SDK cancellation can arrive before RVS reflects it. Denial never needs
            // an active server claim, and an in-flight verification must not undo it.
            if (isCurrentEntitlement()) {
                expiry?.cancel()
                timeout?.cancel()
                state.value = state.value.copy(premium = false, busy = false, status = "revoked")
            }
            scope.launch {
                verificationMutex.withLock {
                    if (isCurrentEntitlement()) {
                        app.settings.edit { it.remove(stringPreferencesKey("signed_license")) }
                        state.value = state.value.copy(premium = false, busy = false, status = "revoked")
                    }
                }
            }
            return
        }
        scope.launch {
            verificationMutex.withLock {
                state.value = state.value.copy(busy = true, status = "verifying")
                try {
                    val signed =
                        withContext(Dispatchers.IO) {
                            val connection =
                                URL(BuildConfig.VERIFIER_URL).openConnection() as HttpsURLConnection
                            connection.instanceFollowRedirects = false
                            connection.requestMethod = "POST"
                            connection.connectTimeout = 15000
                            connection.readTimeout = 15000
                            connection.doOutput = true
                            connection.setRequestProperty("Content-Type", "application/json")
                            try {
                                connection.outputStream.use {
                                    it.write(
                                        Files.gson
                                            .toJson(
                                                mapOf(
                                                    "userId" to userId,
                                                    "receiptId" to receipt.receiptId,
                                                )
                                            )
                                            .toByteArray(Charsets.UTF_8)
                                    )
                                }
                                require(connection.responseCode == 200)
                                val bytes =
                                    connection.inputStream.use { Files.readBounded(it, 64 * 1024) }
                                Files.gson.fromJson(
                                    String(bytes, Charsets.UTF_8),
                                    SignedLicense::class.java,
                                )
                            } finally {
                                connection.disconnect()
                            }
                        }
                    val claims =
                        LicenseVerifier.verify(
                            signed,
                            BuildConfig.LICENSE_PUBLIC_KEY,
                            BuildConfig.AMAZON_SKU,
                            now = licenseTime(),
                            expectedEnvironment = BuildConfig.BILLING_ENV,
                        )
                    require(
                        claims.userId == userId &&
                            claims.receiptId == receipt.receiptId &&
                            (currentUser == null || currentUser == userId) &&
                            receiptKey !in cancelledReceipts
                    )
                    app.settings.edit {
                        it[stringPreferencesKey("signed_license")] = Files.gson.toJson(signed)
                    }
                    // DataStore suspends; cancellation or account change may arrive while saving.
                    require(receiptKey !in cancelledReceipts &&
                        (currentUser == null || currentUser == userId))
                    tokenUser = userId
                    tokenReceipt = receipt.receiptId
                    state.value =
                        state.value.copy(
                            premium = claims.active,
                            busy = false,
                            status = if (claims.active) "verified" else "revoked",
                        )
                    expireAt(claims.validUntil)
                    if (claims.active)
                        PurchasingService.notifyFulfillment(
                            receipt.receiptId,
                            FulfillmentResult.FULFILLED,
                        )
                } catch (e: Exception) {
                    state.value = state.value.copy(busy = false, status = "retry")
                }
            }
        }
    }
}
