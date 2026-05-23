package com.parsfilo.contentapp.feature.prayertimes.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PrayerRescheduleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prayerAlarmScheduler: PrayerAlarmScheduler

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        val action = intent?.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        val pendingResult = goAsync()

        val receiverJob = SupervisorJob()
        val receiverScope = CoroutineScope(receiverJob + Dispatchers.IO)

        receiverScope.launch {
            try {
                runCatching { prayerAlarmScheduler.scheduleNextForCurrentFlavor() }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.w(error, "Prayer alarm reschedule failed for action=%s", action)
                    }
            } finally {
                receiverJob.cancel()
                pendingResult.finish()
            }
        }
    }

    private companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
