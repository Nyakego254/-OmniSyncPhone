package com.omnisync.phone

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    // Using Worker instead of CoroutineWorker to avoid extra complexity

    override fun doWork(): Result {
        val db = DatabaseHelper(applicationContext)

        val unsyncedCalls = db.getUnsyncedCalls()
        val unsyncedSms = db.getUnsyncedSms()
        val unsyncedNotifications = db.getUnsyncedNotifications()

        if (unsyncedCalls.isEmpty() && unsyncedSms.isEmpty() && unsyncedNotifications.isEmpty()) {
            return Result.success()
        }

        return try {
            // Use runBlocking? No, Worker runs on background thread already.
            // We'll use a CountDownLatch style via suspend but Worker is blocking.
            // Simpler: upload synchronously using Firebase's await()? But Firebase doesn't have await.
            // Instead, we'll run blocking uploads with onSuccessListener using a latch.
            uploadAndWait(unsyncedCalls, unsyncedSms, unsyncedNotifications, db)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun uploadAndWait(
        calls: List<DatabaseHelper.CallRecord>,
        smsList: List<DatabaseHelper.SmsRecord>,
        notifications: List<DatabaseHelper.NotificationRecord>,
        db: DatabaseHelper
    ) {
        // Use CountDownLatch to wait for all uploads to finish
        val latch = java.util.concurrent.CountDownLatch(3)

        var callSuccessIds = mutableListOf<Long>()
        var smsSuccessIds = mutableListOf<Long>()
        var notifSuccessIds = mutableListOf<Long>()

        FirebaseManager.uploadCalls(calls) { successIds ->
            callSuccessIds = successIds.toMutableList()
            latch.countDown()
        }

        FirebaseManager.uploadSms(smsList) { successIds ->
            smsSuccessIds = successIds.toMutableList()
            latch.countDown()
        }

        FirebaseManager.uploadNotifications(notifications) { successIds ->
            notifSuccessIds = successIds.toMutableList()
            latch.countDown()
        }

        // Wait up to 30 seconds for all uploads
        latch.await(30, java.util.concurrent.TimeUnit.SECONDS)

        // Mark as synced only those that succeeded
        db.markCallsSynced(callSuccessIds)
        db.markSmsSynced(smsSuccessIds)
        db.markNotificationsSynced(notifSuccessIds)
    }

    companion object {
        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_work",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}