package com.parsfilo.contentapp.feature.counter.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.parsfilo.contentapp.feature.counter.data.ZikirRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ZikirReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var zikirReminderNotifier: ZikirReminderNotifier

    @Inject
    lateinit var zikirReminderScheduler: ZikirReminderScheduler

    @Inject
    lateinit var zikirRepository: ZikirRepository

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Timber.d("ZikirReminderReceiver triggered action=%s", action)
        val pendingResult = goAsync()

        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        receiverScope.launch {
            try {
                when (action) {
                    ZikirReminderScheduler.ACTION_ZIKIR_REMINDER -> {
                        val todayTotal = zikirRepository.getTodayTotalCount().first()
                        Timber.d("Zikir daily reminder fired todayTotal=%d", todayTotal)
                        zikirReminderNotifier.showDailyReminder(todayTotalCount = todayTotal)
                        zikirReminderScheduler.scheduleOrCancelFromPreferences()
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        Timber.d("Zikir schedule refresh requested by system action=%s", action)
                        zikirReminderScheduler.scheduleOrCancelFromPreferences()
                        zikirReminderScheduler.scheduleStreakCheckWorker()
                    }
                }
            } finally {
                Timber.d("ZikirReminderReceiver finished action=%s", action)
                pendingResult.finish()
            }
        }
    }
}
