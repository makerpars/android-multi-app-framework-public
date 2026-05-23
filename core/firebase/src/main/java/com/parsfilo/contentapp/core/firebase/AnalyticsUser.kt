package com.parsfilo.contentapp.core.firebase

import com.google.firebase.analytics.FirebaseAnalytics

fun AppAnalytics.logUserLogin(method: String) {
    logEvent(FirebaseAnalytics.Event.LOGIN, android.os.Bundle().apply {
        putString(FirebaseAnalytics.Param.METHOD, method)
    })
}

fun AppAnalytics.logUserLogout() {
    logEvent("user_logout")
}
