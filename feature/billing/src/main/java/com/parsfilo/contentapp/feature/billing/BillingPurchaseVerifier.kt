package com.parsfilo.contentapp.feature.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.parsfilo.contentapp.core.common.network.TimberNetworkLoggingInterceptor
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val PURCHASE_VERIFICATION_URL = "purchase_verification_url"

@Singleton
class BillingPurchaseVerifier @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseAppCheck: FirebaseAppCheck,
    @Named(PURCHASE_VERIFICATION_URL) private val verificationUrl: String,
) {
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(TimberNetworkLoggingInterceptor("billing_verify"))
            .build()

    suspend fun verify(
        packageName: String,
        purchase: Purchase,
    ): VerificationResult {
        if (verificationUrl.isBlank()) {
            return VerificationResult(
                verified = false,
                expiryTimeMillis = null,
                isAutoRenewing = false,
                purchaseState = "MISCONFIGURED",
                acknowledgementState = "UNKNOWN",
                error = "Purchase verification URL is missing",
            )
        }

        val user = firebaseAuth.currentUser ?: return VerificationResult(
            verified = false,
            expiryTimeMillis = null,
            isAutoRenewing = false,
            purchaseState = "AUTH_REQUIRED",
            acknowledgementState = "UNKNOWN",
            error = "Firebase Auth user is required for purchase verification",
        )

        val idToken =
            runCatching { user.getIdToken(false).awaitResult()?.token.orEmpty() }.getOrElse {
                    Timber.w(it, "Billing verification failed: unable to read Firebase ID token")
                    ""
                }
        if (idToken.isBlank()) {
            return VerificationResult(
                verified = false,
                expiryTimeMillis = null,
                isAutoRenewing = false,
                purchaseState = "AUTH_TOKEN_MISSING",
                acknowledgementState = "UNKNOWN",
                error = "Firebase ID token is missing",
            )
        }

        val appCheckToken = runCatching {
            firebaseAppCheck.getAppCheckToken(false).awaitResult()?.token.orEmpty()
        }.getOrElse {
                Timber.w(it, "Billing verification failed: unable to read App Check token")
                ""
            }
        if (appCheckToken.isBlank()) {
            return VerificationResult(
                verified = false,
                expiryTimeMillis = null,
                isAutoRenewing = false,
                purchaseState = "APP_CHECK_MISSING",
                acknowledgementState = "UNKNOWN",
                error = "App Check token is missing",
            )
        }

        val productId = purchase.products.firstOrNull().orEmpty()
        val purchaseType =
            if (purchase.products.any { BillingCatalog.subscriptionProductIds.contains(it) }) {
                BillingClient.ProductType.SUBS
            } else {
                BillingClient.ProductType.INAPP
            }

        if (productId.isBlank()) {
            return VerificationResult(
                verified = false,
                expiryTimeMillis = null,
                isAutoRenewing = false,
                purchaseState = "INVALID_PURCHASE",
                acknowledgementState = "UNKNOWN",
                error = "Product ID is missing in purchase payload",
            )
        }

        val requestJson = JSONObject().put("packageName", packageName).put("productId", productId)
            .put("purchaseToken", purchase.purchaseToken).put(
                "purchaseType",
                if (purchaseType == BillingClient.ProductType.SUBS) "subs" else "inapp"
            )

        val request =
            Request.Builder().url(verificationUrl).addHeader("Authorization", "Bearer $idToken")
                .addHeader("X-Firebase-AppCheck", appCheckToken)
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)).build()

        val response =
            runCatching { okHttpClient.newCall(request).execute() }.getOrElse { throwable ->
                    Timber.w(throwable, "Billing purchase verification request failed")
                    return VerificationResult(
                        verified = false,
                        expiryTimeMillis = null,
                        isAutoRenewing = false,
                        purchaseState = "NETWORK_ERROR",
                        acknowledgementState = "UNKNOWN",
                        error = "Network error while verifying purchase",
                    )
                }

        response.use { httpResponse ->
            val body = httpResponse.body.string()
            if (!httpResponse.isSuccessful) {
                Timber.w(
                    "Billing purchase verification returned %s (%s)",
                    httpResponse.code,
                    body.take(180)
                )
                return VerificationResult(
                    verified = false,
                    expiryTimeMillis = null,
                    isAutoRenewing = false,
                    purchaseState = "HTTP_${httpResponse.code}",
                    acknowledgementState = "UNKNOWN",
                    error = "Verification endpoint returned HTTP ${httpResponse.code}",
                )
            }

            return parseVerificationResponse(body)
        }
    }

    private fun parseVerificationResponse(body: String): VerificationResult {
        return runCatching {
            val json = JSONObject(body)
            VerificationResult(
                verified = json.optBoolean("verified", false),
                expiryTimeMillis = json.optLong("expiryTimeMillis", 0L).takeIf { it > 0L },
                isAutoRenewing = json.optBoolean("autoRenewing", false),
                purchaseState = json.optString("purchaseState", "UNKNOWN"),
                acknowledgementState = json.optString("acknowledgementState", "UNKNOWN"),
                error = null,
            )
        }.getOrElse { throwable ->
            Timber.w(throwable, "Billing purchase verification response parse failed")
            VerificationResult(
                verified = false,
                expiryTimeMillis = null,
                isAutoRenewing = false,
                purchaseState = "PARSE_ERROR",
                acknowledgementState = "UNKNOWN",
                error = "Verification response parse failed",
            )
        }
    }
}

data class VerificationResult(
    val verified: Boolean,
    val expiryTimeMillis: Long?,
    val isAutoRenewing: Boolean,
    val purchaseState: String,
    val acknowledgementState: String,
    val error: String?,
)

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }.addOnFailureListener { throwable ->
            continuation.resumeWithException(throwable)
        }
    }

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
