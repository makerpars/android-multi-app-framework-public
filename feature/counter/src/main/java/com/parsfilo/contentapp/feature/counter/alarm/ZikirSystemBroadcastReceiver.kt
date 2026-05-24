package com.parsfilo.contentapp.feature.counter.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ZikirSystemBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var zikirReminderScheduler: ZikirReminderScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Timber.d("ZikirSystemBroadcastReceiver action=%s", action)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                zikirReminderScheduler.scheduleOrCancelFromPreferences()
                zikirReminderScheduler.scheduleStreakCheckWorker()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
