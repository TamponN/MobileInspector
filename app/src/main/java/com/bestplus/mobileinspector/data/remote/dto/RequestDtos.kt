package com.bestplus.mobileinspector.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для отправки выполненных заданий в 1С (POST body)
 * C# source: SendInfo.cs, ClassInfoSubscibers.cs, MeteringDevice.cs,
 *            ScalesOutput.cs, TestimonyOutput.cs, Subs.cs, ActCheck.cs, ActAccess.cs
 */
@Serializable
data class SendInfoDto(
    @SerialName("Key") val key: String,
    @SerialName("UUIDDocument") val uuidDocument: String,
    @SerialName("TypeSubscriber") val typeSubscriber: String = "",
    @SerialName("CloseListRoute") val closeListRoute: Boolean = false,
    @SerialName("ListInfoSubscriber") val subscribers: List<SendSubscriberDto> = emptyList(),
)

@Serializable
data class SendSubscriberDto(
    @SerialName("Subscriber") val subscriber: SendSubDto,
    @SerialName("DatePayment") val datePayment: String,
    @SerialName("AmountOfPayment") val amountOfPayment: String = "0",
    @SerialName("MeteringDevice") val meteringDevices: List<SendMeteringDeviceDto> = emptyList(),
)

@Serializable
data class SendSubDto(
    @SerialName("IsCreateAct") val isCreateAct: Boolean = false,
    @SerialName("IsCreateActAccess") val isCreateActAccess: Boolean = false,
    @SerialName("Phone") val phone: String = "",
    @SerialName("UUIDAccount") val uuidAccount: String = "",
)

@Serializable
data class SendMeteringDeviceDto(
    @SerialName("NumberDevice") val numberDevice: String = "",
    @SerialName("Plomb") val plomb: Boolean = false,
    @SerialName("Defect") val defect: Boolean = false,
    @SerialName("CountingPointKey") val countingPointKey: String = "",
    @SerialName("ActCheck") val actCheck: SendActCheckDto = SendActCheckDto(),
    @SerialName("ActAccess") val actAccess: SendActAccessDto = SendActAccessDto(),
    @SerialName("Scales") val scales: List<SendScaleDto> = emptyList(),
    @SerialName("Name") val name: String = "",
    @SerialName("Mark") val mark: String = "",
    @SerialName("FactoryNumber") val factoryNumber: String = "",
    @SerialName("NameService") val nameService: String = "",
    @SerialName("UUIDDevice") val uuidDevice: String = "",
)

@Serializable
data class SendActCheckDto(
    // 1С парсит эти поля как дату (ФорматДатыJSON.ISO), поэтому дефолт должен быть
    // валидной ISO-датой, а не пустой строкой — иначе ПрочитатьJSON падает с
    // "Представление даты имеет неверный формат". При IsCreateAct=false значение
    // игнорируется бизнес-логикой 1С (акты создаются только под IsCreateAct=true).
    @SerialName("DateTimeCheck") val dateTimeCheck: String = "1970-01-01T00:00:00",
    @SerialName("RoomCheck") val roomCheck: String = "",
    @SerialName("SealIntegrity") val sealIntegrity: String = "",
    @SerialName("Revealed") val revealed: String = "",
    @SerialName("Reconciliation") val reconciliation: String = "",
    @SerialName("ConsentTestimony") val consentTestimony: String = "",
    @SerialName("СauseNoAgreeCheck") val causeNoAgreeCheck: String = "",
    @SerialName("UnauthorizedPersonsCheck") val unauthorizedPersonsCheck: String = "",
    @SerialName("VerificationDate") val verificationDate: String = "1970-01-01T00:00:00",
    @SerialName("IsCreateAct") val isCreateAct: Boolean = false,
)

@Serializable
data class SendActAccessDto(
    @SerialName("DateTimeAccess") val dateTimeAccess: String = "1970-01-01T00:00:00",
    @SerialName("RoomAccess") val roomAccess: String = "",
    @SerialName("AgreeAccess") val agreeAccess: String = "",
    @SerialName("СauseNoAgreeAccess") val causeNoAgreeAccess: String = "",
    @SerialName("UnauthorizedPersonsAcсess") val unauthorizedPersonsAccess: String = "",
    @SerialName("IsCreateActAccess") val isCreateActAccess: Boolean = false,
)

@Serializable
data class SendScaleDto(
    @SerialName("ListTestimony") val testimonies: List<SendTestimonyDto> = emptyList(),
    @SerialName("UnitType") val unitType: String = "",
    @SerialName("UUIDScaleDevices") val uuidScaleDevices: String = "",
)

@Serializable
data class SendTestimonyDto(
    @SerialName("CurrentTestimony") val currentTestimony: Int = 0,
    @SerialName("DateTimeCurrentTestimony") val dateTimeCurrentTestimony: String = "",
    @SerialName("Index") val index: Int = 0,
    @SerialName("Image") val image: String = "",
    @SerialName("NameTariff") val nameTariff: String = "",
    @SerialName("UUIDTariffZone") val uuidTariffZone: String = "",
    @SerialName("Coup") val coup: Boolean = false,
)
