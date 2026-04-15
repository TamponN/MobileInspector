package com.bestplus.mobileinspector.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/** Маршрутный лист */
@Serializable
data class RouteSheet(
    val key: String,
    val name: String,
    val uuidDocument: String,
    val status: String,
    val statusTask: String,
    val planDateTime: String,
    val organizationKey: String,
    val country: String,
    val subscribers: List<InfoSubscriber>,
    val street: String = "",
) {
    val countSubs: Int get() = subscribers.size
    val countClosedSubs: Int get() = subscribers.count { it.statusTask == "Закрыт" }
    val countCompletedSubs: Int get() = subscribers.count { it.statusTask == "Выполнена" }
    val isFullyClosed: Boolean get() = countSubs > 0 && subscribers.all { it.statusTask == "Выполнена" || it.statusTask == "Закрыт" }
}

/** Абонент внутри маршрутного листа */
@Serializable
data class InfoSubscriber(
    val uuid: String,
    val subscriber: Subscriber,
    val address: Address,
    val meteringDevices: List<MeteringDevice>,
    val planDate: String,
    val statusTask: String,
    val uuidAccount: String,
    val position: GeoPosition? = null,
    val photos: List<String> = emptyList(),
) {
    val displayName: String get() = subscriber.fullName
    val allReadingsTaken: Boolean
        get() = meteringDevices.all { device ->
            device.scales.all { scale ->
                scale.testimonies.all { it.currentTestimony.isNotBlank() && it.currentTestimony != "0" }
            }
        }
    val unreadDeviceCount: Int
        get() = meteringDevices.count { device ->
            device.scales.any { scale ->
                scale.testimonies.any { it.currentTestimony.isBlank() || it.currentTestimony == "0" }
            }
        }
}

/** Данные абонента */
@Serializable
data class Subscriber(
    val key: String,
    val fullName: String,
    val personalAccount: String,
    val uuidAccount: String,
    val typeSubscriber: String,
    val building: String,
    val phone: String,
    val passport: String,
    val dateOfPassport: String,
)

/** Адрес */
@Serializable
data class Address(
    val key: String,
    val value: String,
    val country: String,
    val region: String,
    val city: String,
    val area: String,
    val street: String,
    val sideStreet: String,
    val house: String,
    val body: String,
    val apartment: String,
) {
    val shortAddress: String
        get() = buildString {
            append("ул. $street, д. $house")
            if (body.isNotBlank()) append(", корп. $body")
            if (apartment.isNotBlank()) append(", кв. $apartment")
        }

    val fullAddress: String
        get() = buildString {
            if (city.isNotBlank()) append("г. $city, ")
            append(shortAddress)
        }
}

/** Прибор учёта */
@Serializable
data class MeteringDevice(
    val key: String,
    val name: String,
    val nameService: String,
    val number: String,
    val counterBrand: String,
    val factoryNumber: String,
    val countingPointKey: String,
    val dateTimePlomb: String,
    val numberPlomb: String,
    val typeDevice: String,
    val uuidDevice: String,
    val scales: List<Scale>,
    val actCheck: ActCheck? = null,
    val actAccess: ActAccess? = null,
)

/** Шкала прибора учёта */
@Serializable
data class Scale(
    val key: String,
    val name: String,
    val nameScale: String,
    val unitType: String,
    val uuidScaleDevices: String,
    val testimonies: List<Testimony>,
) {
    val allRead: Boolean
        get() = testimonies.all { it.currentTestimony.isNotBlank() && it.currentTestimony != "0" }
}

/** Показание прибора учёта */
@Serializable
data class Testimony(
    val key: String,
    val idCountingPointKey: String,
    val idScale: String,
    val uuidTariffZone: String,
    val index: Int,
    val nameScale: String,
    val nameTariff: String,
    val previousTestimony: String,
    val currentTestimony: String,
    val dateTimePrevious: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTimeCurrent: LocalDateTime = LocalDateTime.now(),
    val picturePath: String? = null,
    val coup: Boolean = false,
    val x: String = "",
    val y: String = "",
)

/** Акт проверки ПУ */
@Serializable
data class ActCheck(
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTimeCheck: LocalDateTime = LocalDateTime.now(),
    val roomCheck: String = "",
    val sealIntegrity: String = "",
    val revealed: String = "",
    val reconciliation: String = "",
    val consentTestimony: String = "",
    val causeNoAgreeCheck: String = "",
    val unauthorizedPersonsCheck: String = "",
    @Serializable(with = LocalDateTimeSerializer::class)
    val verificationDate: LocalDateTime = LocalDateTime.now(),
    val isCreateAct: Boolean = false,
    // Act-specific fields
    val inspectionResult: String = "",
    val factTestimony: String = "",
    val isNoAgreesTestimony: Boolean = false,
    val reasonForDisagreeing: String = "",
    val testimonyAgrees: String = "",
)

/** Акт допуска */
@Serializable
data class ActAccess(
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTimeAccess: LocalDateTime = LocalDateTime.now(),
    val roomAccess: String = "",
    val agreeAccess: String = "",
    val causeNoAgreeAccess: String = "",
    val unauthorizedPersonsAccess: String = "",
    val isCreateActAccess: Boolean = false,
)

/** Геопозиция */
@Serializable
data class GeoPosition(
    val latitude: Double,
    val longitude: Double,
)

/** Пользователь / сессия */
data class UserSession(
    val login: String,
    val token: String,
    val guid: String,
    val licenseDate: LocalDateTime? = null,
)

/** Настройки сервера 1С */
data class ServerSettings(
    val address: String,
    val databaseName: String,
    val useSsl: Boolean,
    val guid: String,
) {
    val baseUrl: String
        get() {
            val scheme = if (useSsl) "https" else "http"
            return "$scheme://${address.trim()}/${databaseName.trim()}/hs/api/WorkTasks"
        }
}

/** Статус синхронизации */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR_NO_SERVER,
    ERROR_NO_INTERNET,
    ERROR_UUID,
}
