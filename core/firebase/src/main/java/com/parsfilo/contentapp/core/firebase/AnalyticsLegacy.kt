package com.parsfilo.contentapp.core.firebase

/**
 * Legacy events for backward compatibility.
 */

fun AppAnalytics.logPaywallView() {
    logEvent("paywall_view")
}

fun AppAnalytics.logPurchaseStart(planId: String) {
    logEvent("purchase_start", android.os.Bundle().apply {
        putString("plan_id", planId)
    })
}

fun AppAnalytics.logPurchaseSuccess(planId: String) {
    logEvent("purchase_success", android.os.Bundle().apply {
        putString("plan_id", planId)
    })
}

fun AppAnalytics.logPurchaseFailed(reason: String) {
    logEvent("purchase_failed", android.os.Bundle().apply {
        putString("reason", reason)
    })
}

fun AppAnalytics.logAdImpression(adType: String, adUnitId: String) {
    logEvent("ad_impression", android.os.Bundle().apply {
        putString("ad_type", adType)
        putString("ad_unit_id", adUnitId)
    })
}


// Moved to AnalyticsNotifications.kt


fun AppAnalytics.logContentPlayStart(verseId: Int?) {
    logEvent("content_play_start", android.os.Bundle().apply {
        putLong("verse_id", (verseId ?: 0).toLong())
    })
}

fun AppAnalytics.logContentPlayComplete() {
    logEvent("content_play_complete")
}
