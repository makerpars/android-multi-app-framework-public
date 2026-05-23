package com.parsfilo.contentapp.core.firebase.push

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.parsfilo.contentapp.core.common.network.AppDispatchers
import com.parsfilo.contentapp.core.common.network.Dispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cihaz kaydını doğrudan Firestore'a (devices/{installationId}) yazar.
 * Cloud Function yerine client SDK kullanarak bir Firebase Function çağrısını ortadan kaldırır.
 */
@Singleton
class FirestorePushRegistrationSender @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : PushRegistrationSender {

    override suspend fun send(payload: PushRegistrationPayload): Boolean = withContext(ioDispatcher) {
        try {
            val deviceData = hashMapOf(
                "fcmToken" to payload.fcmToken,
                "timezone" to payload.timezone,
                "locale" to payload.locale,
                "packageName" to payload.packageName,
                "notificationsEnabled" to payload.notificationsEnabled,
                "appVersion" to payload.appVersion,
                "deviceModel" to payload.deviceModel,
                "reason" to payload.reason,
                "syncedAtEpochMs" to payload.syncedAtEpochMs,
                "tokenHash" to payload.tokenHash,
                "hasToken" to payload.hasToken,
                "lastRegistrationAttemptAt" to payload.lastAttemptAtEpochMs,
                "lastRegistrationSuccessAt" to payload.lastSuccessAtEpochMs,
                "lastRegistrationFailureReason" to payload.lastFailureReason,
                "adRuntimeWindowStartAt" to payload.adRuntimeWindowStartAtEpochMs,
                "adRuntimeLastUpdatedAt" to payload.adRuntimeLastUpdatedAtEpochMs,
                "adRuntimeFunnelCounts" to payload.adRuntimeFunnelCounts,
                "adRuntimeSuppressReasonCounts" to payload.adRuntimeSuppressReasonCounts,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )

            firestore.collection(DEVICES_COLLECTION)
                .document(payload.installationId)
                .set(deviceData, SetOptions.merge())
                .await()

            Timber.i(
                "Device registered via Firestore (installationId=%s, timezone=%s, pkg=%s, reason=%s)",
                payload.installationId,
                payload.timezone,
                payload.packageName,
                payload.reason,
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Timber.e(e, "Firestore device registration failed")
            crashlytics.log("Firestore device registration failed: ${e::class.simpleName}")
            crashlytics.recordException(e)
            false
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Firestore device registration failed")
            crashlytics.log("Firestore device registration failed: ${e::class.simpleName}")
            crashlytics.recordException(e)
            false
        }
    }

    companion object {
        private const val DEVICES_COLLECTION = "devices"
    }
}
