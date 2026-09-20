package com.jjackpark.healthcalendar

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
 override suspend fun doWork(): Result {
  if (TokenVault(applicationContext).token() == null) return Result.success()
  return try { HealthSync.sync(applicationContext, true); Result.success() }
  catch (e: CancellationException) { throw e }
  catch (e: Exception) {
   TokenVault(applicationContext).status(e.message ?: "자동 동기화에 실패했습니다. 앱에서 확인해주세요.")
   if (e is IOException || (e is SyncHttpException && (e.status >= 500 || e.status == 429))) Result.retry() else Result.failure()
  }
 }
 companion object {
  private const val NAME = "health-calendar-step-sync"
  fun schedule(context: Context, minutes: Int = TokenVault(context).syncMinutes()) {
   if (minutes == 0) { cancel(context); return }
   val safeMinutes = minutes.coerceAtLeast(15)
   val request = PeriodicWorkRequestBuilder<SyncWorker>(safeMinutes.toLong(), TimeUnit.MINUTES, 5.coerceAtMost(safeMinutes).toLong(), TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
   WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
  }
  fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(NAME) }
 }
}
