package com.bestplus.mobileinspector.data.remote

import com.bestplus.mobileinspector.data.remote.dto.RouteSheetDto
import com.bestplus.mobileinspector.data.remote.dto.SendInfoDto
import com.bestplus.mobileinspector.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обёртка над Retrofit для работы с 1С.
 * Заменяет C# MSystem : IMasterSystem
 */
@Singleton
class OneCDataSource @Inject constructor(
    private val api: OneCApi,
) {
    /**
     * Получить маршрутные листы из 1С.
     * GET {baseUrl}
     */
    suspend fun fetchRouteSheets(
        baseUrl: String,
        uuid: String,
    ): Result<List<RouteSheetDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getRoutSheets(url = baseUrl, uuid = uuid)
            if (!response.isSuccessful) {
                error("HTTP ${response.code()}")
            }
            response.body().orEmpty()
        }
    }

    /**
     * Отправить выполненные задания в 1С.
     * POST {baseUrl}
     */
    suspend fun sendCompletedTasks(
        baseUrl: String,
        uuid: String,
        data: List<SendInfoDto>,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.sendCompletedTasks(url = baseUrl, uuid = uuid, data = data)
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        /**
         * Определяет статус по результату запроса.
         * Повторяет логику LostPackage enum из C#.
         */
        fun classifyError(exception: Throwable): SyncStatus {
            val message = exception.message.orEmpty()
            return when {
                message.contains("Unable to resolve host") ||
                    message.contains("No address associated") -> SyncStatus.ERROR_NO_INTERNET
                message.contains("Empty response") -> SyncStatus.ERROR_UUID
                else -> SyncStatus.ERROR_NO_SERVER
            }
        }
    }
}
