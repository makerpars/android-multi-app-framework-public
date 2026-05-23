package com.parsfilo.contentapp.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.parsfilo.contentapp.core.common.network.AppDispatchers
import com.parsfilo.contentapp.core.common.network.Dispatcher
import com.parsfilo.contentapp.core.firebase.config.RemoteConfigManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val remoteConfigManager: RemoteConfigManager,
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun checkForUpdate(forceFetch: Boolean = false): UpdatePolicy =
            withContext(ioDispatcher) {
                val snapshot = fetchSnapshot(forceFetch)
                snapshot.resolvedPolicy
            }

        fun currentVersionCode(): Long = resolveCurrentVersionCode(context)

        fun getCachedRemoteUpdateConfig(): RemoteUpdateConfig {
            ensureDefaults()
            return readConfigFromRemote()
        }

        suspend fun refreshAndGetConfig(): RemoteUpdateConfig =
            withContext(ioDispatcher) {
                fetchSnapshot(forceFetch = true).config
            }

        suspend fun getDebugSnapshot(forceFetch: Boolean = false): UpdateDebugSnapshot =
            withContext(ioDispatcher) {
                fetchSnapshot(forceFetch)
            }

        private suspend fun fetchSnapshot(forceFetch: Boolean): UpdateDebugSnapshot {
            ensureDefaults()
            if (forceFetch) {
                Timber.d("Force update check requested (debug fetch interval still applies).")
            }
            runCatching {
                remoteConfigManager.fetchAndActivate()
            }.onFailure { throwable ->
                Timber.w(
                    throwable,
                    "Remote Config fetch failed for force update; cached/default values will be used.",
                )
            }

            val config = readConfigFromRemote()
            val versionCode = currentVersionCode()
            val policy = resolveUpdatePolicy(versionCode, config)
            return UpdateDebugSnapshot(
                currentVersionCode = versionCode,
                config = config,
                resolvedPolicy = policy,
            )
        }

        private fun ensureDefaults() {
            remoteConfigManager.applyClientSettingsIfNeeded()
            remoteConfigManager.setDefaults(RemoteUpdateConfigKeys.defaults)
        }

        private fun readConfigFromRemote(): RemoteUpdateConfig {
            val languageCode =
                context.resources.configuration.locales[0]
                    ?.language
                    ?: Locale.getDefault().language
            val minSupported =
                coerceRemoteVersionCode(
                    remoteConfigManager.getLong(RemoteUpdateConfigKeys.MIN_SUPPORTED_VERSION_CODE),
                )
            val latest =
                coerceRemoteVersionCode(
                    remoteConfigManager.getLong(RemoteUpdateConfigKeys.LATEST_VERSION_CODE),
                )
            val mode = remoteConfigManager.getString(RemoteUpdateConfigKeys.UPDATE_MODE).trim()

            val title =
                resolveLocalizedRemoteText(
                    languageCode = languageCode,
                    trValue = remoteConfigManager.getString(RemoteUpdateConfigKeys.UPDATE_TITLE_TR),
                    enValue = remoteConfigManager.getString(RemoteUpdateConfigKeys.UPDATE_TITLE_EN),
                    fallback = RemoteUpdateConfigKeys.defaults[RemoteUpdateConfigKeys.UPDATE_TITLE_EN] as String,
                )
            val message =
                resolveLocalizedRemoteText(
                    languageCode = languageCode,
                    trValue =
                        remoteConfigManager.getString(
                            RemoteUpdateConfigKeys.UPDATE_MESSAGE_TR,
                        ),
                    enValue =
                        remoteConfigManager.getString(
                            RemoteUpdateConfigKeys.UPDATE_MESSAGE_EN,
                        ),
                    fallback = RemoteUpdateConfigKeys.defaults[RemoteUpdateConfigKeys.UPDATE_MESSAGE_EN] as String,
                )
            val updateButton =
                resolveLocalizedRemoteText(
                    languageCode = languageCode,
                    trValue =
                        remoteConfigManager.getString(
                            RemoteUpdateConfigKeys.UPDATE_BUTTON_TR,
                        ),
                    enValue =
                        remoteConfigManager.getString(
                            RemoteUpdateConfigKeys.UPDATE_BUTTON_EN,
                        ),
                    fallback = RemoteUpdateConfigKeys.defaults[RemoteUpdateConfigKeys.UPDATE_BUTTON_EN] as String,
                )
            val laterButton =
                resolveLocalizedRemoteText(
                    languageCode = languageCode,
                    trValue = remoteConfigManager.getString(RemoteUpdateConfigKeys.LATER_BUTTON_TR),
                    enValue = remoteConfigManager.getString(RemoteUpdateConfigKeys.LATER_BUTTON_EN),
                    fallback = RemoteUpdateConfigKeys.defaults[RemoteUpdateConfigKeys.LATER_BUTTON_EN] as String,
                )

            return RemoteUpdateConfig(
                minSupportedVersionCode = minSupported,
                latestVersionCode = latest,
                updateMode = mode,
                title = title,
                message = message,
                updateButton = updateButton,
                laterButton = laterButton,
            )
        }
    }

internal fun resolveCurrentVersionCode(context: Context): Long =
    runCatching {
        val packageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

        PackageInfoCompat.getLongVersionCode(packageInfo)
    }.getOrElse { throwable ->
        Timber.w(
            throwable,
            "Failed to read installed package versionCode; using BuildConfig fallback.",
        )
        com.parsfilo.contentapp.BuildConfig.VERSION_CODE
            .toLong()
    }
