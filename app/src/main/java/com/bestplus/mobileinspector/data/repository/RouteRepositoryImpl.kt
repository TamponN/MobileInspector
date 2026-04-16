package com.bestplus.mobileinspector.data.repository

import com.bestplus.mobileinspector.data.local.dao.RouteSheetDao
import com.bestplus.mobileinspector.data.local.entity.toDomain
import com.bestplus.mobileinspector.data.local.entity.toEntity
import com.bestplus.mobileinspector.data.remote.OneCDataSource
import com.bestplus.mobileinspector.data.remote.dto.SendInfoDto
import com.bestplus.mobileinspector.data.remote.dto.toDomain
import com.bestplus.mobileinspector.data.remote.dto.toSendSubscriberDto
import com.bestplus.mobileinspector.domain.model.*
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import com.bestplus.mobileinspector.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация RouteRepository.
 * Замена in-memory хранения из Xamarin на Room + Retrofit.
 */
@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val dao: RouteSheetDao,
    private val remote: OneCDataSource,
    private val settings: SettingsRepository,
) : RouteRepository {

    override fun observeRouteSheets(): Flow<List<RouteSheet>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun sync(): SyncStatus {
        val serverSettings = settings.getServerSettings()
        val session = settings.getUserSession() ?: return SyncStatus.ERROR_UUID
        val baseUrl = serverSettings.baseUrl

        // 1. Собрать выполненные задания для отправки
        val current = dao.getAll().map { it.toDomain() }
        val completed = current.filter { route ->
            route.subscribers.any { it.statusTask == "Выполнена" }
        }

        // 2. Отправить в 1С (POST)
        if (completed.isNotEmpty()) {
            val payload = completed.map { route ->
                val closedSubs = route.subscribers.filter { it.statusTask == "Выполнена" }
                SendInfoDto(
                    key = route.key,
                    uuidDocument = route.uuidDocument,
                    typeSubscriber = route.subscribers.firstOrNull()?.subscriber?.typeSubscriber ?: "1",
                    closeListRoute = route.isFullyClosed,
                    subscribers = closedSubs.map { it.toSendSubscriberDto() },
                )
            }
            remote.sendCompletedTasks(baseUrl, session.guid, payload)
        }

        // 3. Получить актуальные данные из 1С (GET)
        val result = remote.fetchRouteSheets(baseUrl, session.guid)
        return result.fold(
            onSuccess = { dtos ->
                val sheets = dtos.map { it.toDomain() }
                dao.deleteAll()
                dao.upsertAll(sheets.map { it.toEntity() })
                SyncStatus.SUCCESS
            },
            onFailure = { error ->
                OneCDataSource.classifyError(error)
            },
        )
    }

    override suspend fun updateTestimony(
        routeUuid: String,
        subscriberUuid: String,
        deviceIndex: Int,
        scaleIndex: Int,
        testimonyIndex: Int,
        currentValue: String,
        picturePath: String?,
    ) {
        mutateRoute(routeUuid) { route ->
            route.copy(
                subscribers = route.subscribers.map { sub ->
                    if (sub.uuid != subscriberUuid) return@map sub
                    sub.copy(
                        meteringDevices = sub.meteringDevices.mapIndexed { dIdx, dev ->
                            if (dIdx != deviceIndex) return@mapIndexed dev
                            dev.copy(
                                scales = dev.scales.mapIndexed { sIdx, scale ->
                                    if (sIdx != scaleIndex) return@mapIndexed scale
                                    scale.copy(
                                        testimonies = scale.testimonies.mapIndexed { tIdx, t ->
                                            if (tIdx != testimonyIndex) return@mapIndexed t
                                            t.copy(
                                                currentTestimony = currentValue,
                                                picturePath = picturePath ?: t.picturePath,
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    }

    override suspend fun markSubscriberCompleted(routeUuid: String, subscriberUuid: String) {
        mutateRoute(routeUuid) { route ->
            route.copy(
                subscribers = route.subscribers.map { sub ->
                    if (sub.uuid != subscriberUuid) return@map sub
                    sub.copy(statusTask = "Выполнена")
                }
            )
        }
    }

    override suspend fun saveActCheck(
        routeUuid: String,
        subscriberUuid: String,
        deviceKey: String,
        actCheck: ActCheck,
    ) {
        mutateRoute(routeUuid) { route ->
            route.copy(
                subscribers = route.subscribers.map { sub ->
                    if (sub.uuid != subscriberUuid) return@map sub
                    sub.copy(
                        meteringDevices = sub.meteringDevices.map { dev ->
                            if (dev.key != deviceKey) return@map dev
                            dev.copy(actCheck = actCheck)
                        }
                    )
                }
            )
        }
    }

    override suspend fun saveActAccess(
        routeUuid: String,
        subscriberUuid: String,
        deviceKey: String,
        actAccess: ActAccess,
    ) {
        mutateRoute(routeUuid) { route ->
            route.copy(
                subscribers = route.subscribers.map { sub ->
                    if (sub.uuid != subscriberUuid) return@map sub
                    sub.copy(
                        meteringDevices = sub.meteringDevices.map { dev ->
                            if (dev.key != deviceKey) return@map dev
                            dev.copy(actAccess = actAccess)
                        }
                    )
                }
            )
        }
    }

    /**
     * Атомарная мутация маршрутного листа: прочитать → изменить → сохранить.
     */
    private suspend fun mutateRoute(uuid: String, transform: (RouteSheet) -> RouteSheet) {
        val entity = dao.getByUuid(uuid) ?: return
        val route = entity.toDomain()
        val updated = transform(route)
        dao.upsert(updated.toEntity())
    }
}
