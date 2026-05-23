package com.parsfilo.contentapp.monetization

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.parsfilo.contentapp.feature.ads.AppOpenTriggerReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenLifecycleCoordinator
    @Inject
    constructor(
        private val adOrchestrator: AdOrchestrator,
    ) : Application.ActivityLifecycleCallbacks,
        DefaultLifecycleObserver {
        @Volatile
        private var currentActivity: Activity? = null

        private val isRegistered = AtomicBoolean(false)
        private val pendingForegroundRequest = AtomicBoolean(false)
        private val firstForegroundHandled = AtomicBoolean(false)

        fun register(application: Application) {
            if (isRegistered.getAndSet(true)) {
                Timber.d("AppOpenLifecycleCoordinator already registered")
                return
            }
            application.registerActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            Timber.d("AppOpenLifecycleCoordinator registered")
        }

        override fun onStart(owner: LifecycleOwner) {
            pendingForegroundRequest.set(true)
            Timber.d("Process onStart: app-open request queued for next resumed activity")
        }

        override fun onStop(owner: LifecycleOwner) {
            Timber.d("Process onStop: notifying app paused")
            adOrchestrator.onAppPaused(currentActivity?.applicationContext)
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) = Unit

        override fun onActivityStarted(activity: Activity) {
            if (!adOrchestrator.isAppOpenAdShowing()) {
                currentActivity = activity
                Timber.d(
                    "Activity started for app-open tracking=%s",
                    activity::class.java.simpleName,
                )
            }
        }

        override fun onActivityResumed(activity: Activity) {
            if (adOrchestrator.isAppOpenAdShowing()) return
            currentActivity = activity
            if (pendingForegroundRequest.compareAndSet(true, false)) {
                val triggerReason =
                    if (firstForegroundHandled.compareAndSet(false, true)) {
                        AppOpenTriggerReason.COLD_START
                    } else {
                        AppOpenTriggerReason.RESUME
                    }
                requestAppOpen(
                    activity = activity,
                    source = "activity_resumed_after_process_on_start",
                    triggerReason = triggerReason,
                )
            }
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity === activity) {
                Timber.d("Tracked activity destroyed=%s", activity::class.java.simpleName)
                currentActivity = null
            }
        }

        private fun requestAppOpen(
            activity: Activity,
            source: String,
            triggerReason: AppOpenTriggerReason,
        ) {
            Timber.d(
                "Requesting app-open source=%s activity=%s",
                source,
                activity::class.java.simpleName,
            )
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                runCatching {
                    delay(350L)
                    val targetActivity = currentActivity
                    if (targetActivity != null && !targetActivity.isFinishing && !targetActivity.isDestroyed) {
                        adOrchestrator.refreshConsent(
                            activity = targetActivity,
                            scope = ProcessLifecycleOwner.get().lifecycleScope,
                        ) { canRequestAds ->
                            if (!canRequestAds) {
                                Timber.d("AppOpen aborted after foreground consent refresh: ads unavailable")
                                return@refreshConsent
                            }
                            ProcessLifecycleOwner.get().lifecycleScope.launch {
                                val refreshedActivity = currentActivity
                                if (
                                    refreshedActivity != null &&
                                    !refreshedActivity.isFinishing &&
                                    !refreshedActivity.isDestroyed
                                ) {
                                    adOrchestrator.showAppOpenAdIfEligible(refreshedActivity, triggerReason)
                                } else {
                                    Timber.w("AppOpen aborted after consent refresh: Activity changed or invalid")
                                }
                            }
                        }
                    } else {
                        Timber.w("AppOpen aborted in coordinator: Activity changed or invalid")
                    }
                }.onFailure { error ->
                    Timber.w(error, "Failed to show app open ad source=%s", source)
                }
            }
        }
    }
