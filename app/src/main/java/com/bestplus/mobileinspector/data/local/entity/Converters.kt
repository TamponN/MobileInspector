package com.bestplus.mobileinspector.data.local.entity

import androidx.room.TypeConverter
import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room TypeConverter: InfoSubscriber list ↔ JSON string.
 * Используется для сериализации вложенного дерева абонентов.
 */
class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun subscribersToJson(subscribers: List<@JvmSuppressWildcards InfoSubscriber>): String =
        json.encodeToString(subscribers)

    @TypeConverter
    fun jsonToSubscribers(value: String): List<InfoSubscriber> =
        json.decodeFromString(value)
}
