package com.bestplus.mobileinspector.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO: Маршрутный лист из 1С (GET response)
 * C# source: ListRouteSheets.cs
 */
@Serializable
data class RouteSheetDto(
    @SerialName("Key") val key: String = "",
    @SerialName("Value") val value: String = "",
    @SerialName("UUIDDocument") val uuidDocument: String = "",
    @SerialName("Status") val status: String = "",
    @SerialName("StatusTask") val statusTask: String = "",
    @SerialName("PlanDateTime") val planDateTime: String = "",
    @SerialName("OrganizationKey") val organizationKey: String = "",
    @SerialName("Country") val country: String = "",
    @SerialName("ListInfoSubscriber") val subscribers: List<InfoSubscriberDto> = emptyList(),
)

/**
 * DTO: Абонент в маршрутном листе
 * C# source: InfoSubscriber.cs
 */
@Serializable
data class InfoSubscriberDto(
    @SerialName("UUID") val uuid: String = "",
    @SerialName("Subscriber") val subscriber: SubscriberDto = SubscriberDto(),
    @SerialName("Address") val address: AddressDto = AddressDto(),
    @SerialName("MeteringDevices") val meteringDevices: List<MeteringDeviceDto> = emptyList(),
    @SerialName("PlanDate") val planDate: String = "",
    @SerialName("StatusTask") val statusTask: String = "",
    @SerialName("UUIDAccount") val uuidAccount: String = "",
    @SerialName("Latitude") val latitude: Double = 0.0,
    @SerialName("Longitude") val longitude: Double = 0.0,
)

/**
 * DTO: Данные абонента (ФИО, лицевой счёт, телефон)
 * C# source: Subscribers.cs
 */
@Serializable
data class SubscriberDto(
    @SerialName("Key") val key: String = "",
    @SerialName("Value") val value: String = "",
    @SerialName("PersonalAccount") val personalAccount: String = "",
    @SerialName("UUIDAccount") val uuidAccount: String = "",
    @SerialName("TypeSubscriber") val typeSubscriber: String = "",
    @SerialName("Building") val building: String = "",
    @SerialName("Phone") val phone: String = "",
    @SerialName("Passport") val passport: String = "",
    @SerialName("DateOfPassport") val dateOfPassport: String = "",
)

/**
 * DTO: Адрес абонента
 * C# source: AddressSub.cs
 */
@Serializable
data class AddressDto(
    @SerialName("Key") val key: String = "",
    @SerialName("Value") val value: String = "",
    @SerialName("Country") val country: String = "",
    @SerialName("Region") val region: String = "",
    @SerialName("City") val city: String = "",
    @SerialName("Area") val area: String = "",
    @SerialName("Street") val street: String = "",
    @SerialName("SideStreet") val sideStreet: String = "",
    @SerialName("House") val house: String = "",
    @SerialName("Body") val body: String = "",
    @SerialName("Apartment") val apartment: String = "",
)

/**
 * DTO: Прибор учёта
 * C# source: MeteringDevices.cs + CatalogMeteringDevice.cs
 */
@Serializable
data class MeteringDeviceDto(
    @SerialName("Key") val key: String = "",
    @SerialName("Value") val value: String = "",
    @SerialName("NameService") val nameService: String = "",
    @SerialName("Number") val number: String = "",
    @SerialName("CounterBrand") val counterBrand: String = "",
    @SerialName("FactoryNumber") val factoryNumber: String = "",
    @SerialName("CountingPointKey") val countingPointKey: String = "",
    @SerialName("DateTimePlomb") val dateTimePlomb: String = "",
    @SerialName("NumberPlomb") val numberPlomb: String = "",
    @SerialName("TypeDevice") val typeDevice: String = "",
    @SerialName("UUIDDevice") val uuidDevice: String = "",
    @SerialName("ListScales") val scales: List<ScaleDto> = emptyList(),
)

/**
 * DTO: Шкала прибора учёта
 * C# source: CatalogScales.cs + Scales.cs
 */
@Serializable
data class ScaleDto(
    @SerialName("Key") val key: String = "",
    @SerialName("Value") val value: String = "",
    @SerialName("NameScale") val nameScale: String = "",
    @SerialName("UnitType") val unitType: String = "",
    @SerialName("UUIDScaleDevices") val uuidScaleDevices: String = "",
    @SerialName("ListTestimony") val testimonies: List<TestimonyDto> = emptyList(),
)

/**
 * DTO: Показание (текущие и предыдущие)
 * C# source: Testimony.cs
 */
@Serializable
data class TestimonyDto(
    @SerialName("Key") val key: String = "",
    @SerialName("Value") val value: String = "",
    @SerialName("IdCountingPointKey") val idCountingPointKey: String = "",
    @SerialName("IdScale") val idScale: String = "",
    @SerialName("UUIDTariffZone") val uuidTariffZone: String = "",
    @SerialName("Index") val index: Int = 0,
    @SerialName("NameScale") val nameScale: String = "",
    @SerialName("PreviousTestimony") val previousTestimony: String = "",
    @SerialName("CurrentTestimony") val currentTestimony: String = "",
    @SerialName("NameTariff") val nameTariff: String = "",
    @SerialName("DateTimePrevious") val dateTimePrevious: String = "",
    @SerialName("X") val x: String = "",
    @SerialName("Y") val y: String = "",
)
