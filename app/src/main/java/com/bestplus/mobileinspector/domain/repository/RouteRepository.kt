package com.bestplus.mobileinspector.domain.repository

import com.bestplus.mobileinspector.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий маршрутных листов.
 * Абстракция над сетью (1С) и локальным хранилищем.
 */
interface RouteRepository {

    /** Поток текущих маршрутных листов (из локального кэша) */
    fun observeRouteSheets(): Flow<List<RouteSheet>>

    /** Синхронизация с 1С: отправить выполненные → получить новые */
    suspend fun sync(): SyncStatus

    /** Обновить показание прибора учёта (по индексам в списках) */
    suspend fun updateTestimony(
        routeUuid: String,
        subscriberUuid: String,
        deviceIndex: Int,
        scaleIndex: Int,
        testimonyIndex: Int,
        currentValue: String,
        picturePath: String?,
    )

    /** Пометить абонента как выполненного */
    suspend fun markSubscriberCompleted(routeUuid: String, subscriberUuid: String)

    /** Сохранить акт проверки для прибора */
    suspend fun saveActCheck(
        routeUuid: String,
        subscriberUuid: String,
        deviceKey: String,
        actCheck: ActCheck,
    )

    /** Сохранить акт допуска для прибора */
    suspend fun saveActAccess(
        routeUuid: String,
        subscriberUuid: String,
        deviceKey: String,
        actAccess: ActAccess,
    )
}
