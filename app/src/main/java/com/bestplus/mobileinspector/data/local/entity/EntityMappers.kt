package com.bestplus.mobileinspector.data.local.entity

import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import com.bestplus.mobileinspector.domain.model.RouteSheet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** RouteSheet (domain) → RouteSheetEntity (Room) */
fun RouteSheet.toEntity(): RouteSheetEntity = RouteSheetEntity(
    uuidDocument = uuidDocument,
    key = key,
    name = name,
    status = status,
    statusTask = statusTask,
    planDateTime = planDateTime,
    organizationKey = organizationKey,
    country = country,
    subscribersJson = json.encodeToString(subscribers),
)

/** RouteSheetEntity (Room) → RouteSheet (domain) */
fun RouteSheetEntity.toDomain(): RouteSheet = RouteSheet(
    key = key,
    name = name,
    uuidDocument = uuidDocument,
    status = status,
    statusTask = statusTask,
    planDateTime = planDateTime,
    organizationKey = organizationKey,
    country = country,
    subscribers = json.decodeFromString<List<InfoSubscriber>>(subscribersJson),
)
