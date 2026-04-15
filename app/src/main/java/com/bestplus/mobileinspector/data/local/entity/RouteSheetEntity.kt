package com.bestplus.mobileinspector.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room-сущность для хранения маршрутного листа целиком.
 * Вложенные данные (абоненты, приборы, показания) хранятся как JSON.
 * Соответствует C# ListRouteSheets + in-memory storage.
 */
@Entity(tableName = "route_sheets")
data class RouteSheetEntity(
    @PrimaryKey
    val uuidDocument: String,
    val key: String,
    val name: String,
    val status: String,
    val statusTask: String,
    val planDateTime: String,
    val organizationKey: String,
    val country: String,
    /** Полное дерево абонентов сериализуется в JSON (kotlinx.serialization) */
    val subscribersJson: String,
    /** Время последней синхронизации */
    val lastSyncMillis: Long = System.currentTimeMillis(),
)
