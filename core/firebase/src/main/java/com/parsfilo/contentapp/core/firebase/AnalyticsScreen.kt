package com.parsfilo.contentapp.core.firebase

import com.google.firebase.analytics.FirebaseAnalytics

fun AppAnalytics.logScreenView(screenName: String, screenClass: String) {
    logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, android.os.Bundle().apply {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
    })
}
