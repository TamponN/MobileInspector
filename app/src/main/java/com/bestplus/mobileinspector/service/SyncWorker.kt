package com.bestplus.mobileinspector.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bestplus.mobileinspector.domain.model.SyncStatus
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Фоновая синхронизация с 1С через WorkManager.
 * Заменяет C# TimestampService (Xamarin.Forms.Device.StartTimer).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val routeRepository: RouteRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val status = routeRepository.sync()
        return when (status) {
            SyncStatus.SUCCESS -> Result.success()
            SyncStatus.ERROR_NO_INTERNET,
            SyncStatus.ERROR_NO_SERVER -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "sync_route_sheets"
    }
}
