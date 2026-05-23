package com.parsfilo.contentapp.core.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merkezi analytics event loglama yardımcısı (Core).
 *
 * Domain spesifik eventler extension dosyalarına ayrılmıştır:
 * - AnalyticsContent.kt
 * - AnalyticsAudio.kt
 * - AnalyticsAds.kt
 * - AnalyticsBilling.kt
 * - AnalyticsSharing.kt
 * - AnalyticsUser.kt
 * - AnalyticsScreen.kt
 * - AnalyticsLegacy.kt
 */
@Singleton
class AppAnalytics @Inject constructor(
    internal val analytics: FirebaseAnalytics
) {
    /**
     * Extension fonksiyonlar tarafından kullanılan generic loglama metodu.
     */
    fun logEvent(name: String, params: Bundle? = null) {
        analytics.logEvent(name, params)
    }

    fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    fun setConsent(
        adStorageGranted: Boolean,
        analyticsStorageGranted: Boolean,
        adUserDataGranted: Boolean = adStorageGranted,
        adPersonalizationGranted: Boolean = adStorageGranted,
    ) {
        analytics.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.AD_STORAGE to
                    if (adStorageGranted) {
                        FirebaseAnalytics.ConsentStatus.GRANTED
                    } else {
                        FirebaseAnalytics.ConsentStatus.DENIED
                    },
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
                    if (analyticsStorageGranted) {
                        FirebaseAnalytics.ConsentStatus.GRANTED
                    } else {
                        FirebaseAnalytics.ConsentStatus.DENIED
                    },
                FirebaseAnalytics.ConsentType.AD_USER_DATA to
                    if (adUserDataGranted) {
                        FirebaseAnalytics.ConsentStatus.GRANTED
                    } else {
                        FirebaseAnalytics.ConsentStatus.DENIED
                    },
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to
                    if (adPersonalizationGranted) {
                        FirebaseAnalytics.ConsentStatus.GRANTED
                    } else {
                        FirebaseAnalytics.ConsentStatus.DENIED
                    },
            ),
        )
    }

    fun setDefaultEventParameters(params: Bundle) {
        analytics.setDefaultEventParameters(params)
    }
}
