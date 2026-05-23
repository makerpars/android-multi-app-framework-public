package com.parsfilo.contentapp.core.firebase

import com.google.firebase.analytics.FirebaseAnalytics

fun AppAnalytics.logAppShared(platform: String) {
    logEvent(FirebaseAnalytics.Event.SHARE, android.os.Bundle().apply {
        putString(FirebaseAnalytics.Param.CONTENT_TYPE, "app_recommendation")
        putString("platform", platform)
    })
}
