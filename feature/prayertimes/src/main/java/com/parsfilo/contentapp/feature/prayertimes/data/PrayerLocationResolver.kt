package com.parsfilo.contentapp.feature.prayertimes.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the device's current location into a [PrayerAddressCandidate] using
 * the Nominatim reverse-geocoding API (OpenStreetMap).
 *
 * Why Nominatim instead of [android.location.Geocoder]?
 * - The blocking [android.location.Geocoder.getFromLocation] overload was
 *   deprecated in API 33 and has no non-deprecated alternative below API 33.
 * - Nominatim is a pure HTTP call — no deprecated APIs, no platform version
 *   guards, no Build. VERSION checks, and zero @Suppress annotations needed.
 * - The app already depends on OkHttp, so there is zero additional overhead.
 *
 * Nominatim ToS: https://operations.osmfoundation.org/policies/nominatim/
 * Rate limit: 1 req/s — easily satisfied for a single on-demand resolve.
 */
@Singleton
class PrayerLocationResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS).build()
    }

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveAddressCandidate(): PrayerAddressCandidate? {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val location = runCatching { fused.lastLocation.await() }.getOrElse { error ->
            when (error) {
                is CancellationException -> throw error
                is SecurityException -> {
                    Timber.w(error, "Location permission rejected while reading last location")
                    null
                }

                is ApiException -> {
                    Timber.w(
                        error,
                        "Failed to fetch last known location (ApiException code=${error.statusCode})",
                    )
                    null
                }

                is IllegalStateException, is IllegalArgumentException -> {
                    Timber.w(error, "Failed to fetch last known location")
                    null
                }

                else -> throw error
            }
        } ?: return null

        return withContext(Dispatchers.IO) {
            reverseGeocodeWithRetry(location.latitude, location.longitude)
        }
    }

    private suspend fun reverseGeocodeWithRetry(
        latitude: Double,
        longitude: Double,
    ): PrayerAddressCandidate? {
        var lastException: Throwable? = null

        repeat(GEOCODER_MAX_RETRIES) { attempt ->
            try {
                return reverseGeocode(latitude, longitude)
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                lastException = error
                Timber.w(error, "Nominatim attempt ${attempt + 1}/$GEOCODER_MAX_RETRIES failed")
                if (attempt < GEOCODER_MAX_RETRIES - 1) delay(GEOCODER_RETRY_DELAY_MS)
            } catch (error: IllegalStateException) {
                lastException = error
                Timber.w(error, "Nominatim attempt ${attempt + 1}/$GEOCODER_MAX_RETRIES failed")
                if (attempt < GEOCODER_MAX_RETRIES - 1) delay(GEOCODER_RETRY_DELAY_MS)
            }
        }

        Timber.w(lastException, "Nominatim exhausted $GEOCODER_MAX_RETRIES retries")
        return null
    }

    /**
     * Calls Nominatim reverse-geocoding and maps the JSON response to a
     * [PrayerAddressCandidate].
     *
     * Example response shape (abbreviated):
     * ```json
     * {
     *   "address": {
     *     "country": "Türkiye",
     *     "state":   "İzmir",    // → city  (adminArea equivalent)
     *     "county":  "Konak",    // → district (subAdminArea equivalent)
     *     "city":    "İzmir"     // fallback when state is absent
     *   }
     * }
     * ```
     */
    private fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): PrayerAddressCandidate? {
        val url = buildNominatimUrl(latitude, longitude)
        val request = Request.Builder().url(url).header("User-Agent", NOMINATIM_USER_AGENT)
            .header("Accept-Language", "tr,en").get().build()

        val body = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Nominatim HTTP ${response.code} for $url")
            }
            response.body.string()
        }

        val address = JSONObject(body).optJSONObject("address") ?: return null

        val country = address.optString("country").ifBlank { return null }
        val city = address.optString("state").ifBlank { address.optString("city") }
        val district = address.optString("county").ifBlank { address.optString("suburb") }

        return PrayerAddressCandidate(
            country = country,
            city = city,
            district = district,
        )
    }

    private fun buildNominatimUrl(latitude: Double, longitude: Double): String =
        "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=$NOMINATIM_ZOOM_LEVEL&addressdetails=1"
}

data class PrayerAddressCandidate(
    val country: String,
    val city: String,
    val district: String,
)

private const val GEOCODER_MAX_RETRIES = 2
private const val GEOCODER_RETRY_DELAY_MS = 500L
private const val CONNECT_TIMEOUT_MS = 10_000L
private const val READ_TIMEOUT_MS = 10_000L
private const val NOMINATIM_ZOOM_LEVEL = 10   // county/district level granularity
private const val NOMINATIM_USER_AGENT = "ParsfiloContentApp/1.0 (Android)"
