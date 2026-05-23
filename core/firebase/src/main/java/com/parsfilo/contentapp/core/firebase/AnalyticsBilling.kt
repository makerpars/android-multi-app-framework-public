package com.parsfilo.contentapp.core.firebase

import com.google.firebase.analytics.FirebaseAnalytics

fun AppAnalytics.logSubscriptionPurchased(productId: String, price: String, currency: String) {
    logEvent(FirebaseAnalytics.Event.PURCHASE, android.os.Bundle().apply {
        putString(FirebaseAnalytics.Param.ITEM_ID, productId)
        putDouble(FirebaseAnalytics.Param.PRICE, price.toDoubleOrNull() ?: 0.0)
        putString(FirebaseAnalytics.Param.CURRENCY, currency)
    })
}

fun AppAnalytics.logSubscriptionCancelled(productId: String) {
    logEvent("subscription_cancelled", android.os.Bundle().apply {
        putString("product_id", productId)
    })
}

fun AppAnalytics.logBillingError(errorCode: String, errorMessage: String) {
    logEvent("billing_error", android.os.Bundle().apply {
        putString("error_code", errorCode)
        putString("error_message", errorMessage)
    })
}
