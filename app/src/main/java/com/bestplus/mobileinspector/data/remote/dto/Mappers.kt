package com.bestplus.mobileinspector.data.remote.dto

import com.bestplus.mobileinspector.domain.model.*
import java.time.LocalDateTime

/** RouteSheetDto → RouteSheet */
fun RouteSheetDto.toDomain(): RouteSheet = RouteSheet(
    key = key,
    name = value.ifBlank { "Обходной лист №$key" },
    uuidDocument = uuidDocument,
    status = status,
    statusTask = statusTask,
    planDateTime = planDateTime,
    organizationKey = organizationKey,
    country = country,
    subscribers = subscribers.map { it.toDomain() },
)

/** InfoSubscriberDto → InfoSubscriber */
fun InfoSubscriberDto.toDomain(): InfoSubscriber = InfoSubscriber(
    uuid = uuid.ifBlank { subscriber.uuidAccount },
    subscriber = subscriber.toDomain(),
    address = address.toDomain(),
    meteringDevices = meteringDevices.map { it.toDomain() },
    planDate = planDate,
    statusTask = statusTask,
    uuidAccount = uuidAccount.ifBlank { subscriber.uuidAccount },
    position = if (latitude != 0.0 && longitude != 0.0)
        com.bestplus.mobileinspector.domain.model.GeoPosition(latitude, longitude)
    else null,
)

/** SubscriberDto → Subscriber */
fun SubscriberDto.toDomain(): Subscriber = Subscriber(
    key = key,
    fullName = value,
    personalAccount = personalAccount,
    uuidAccount = uuidAccount,
    typeSubscriber = typeSubscriber,
    building = building,
    phone = phone,
    passport = passport,
    dateOfPassport = dateOfPassport,
)

/** AddressDto → Address */
fun AddressDto.toDomain(): Address = Address(
    key = key,
    value = value,
    country = country,
    region = region,
    city = city,
    area = area,
    street = street,
    sideStreet = sideStreet,
    house = house,
    body = body,
    apartment = apartment,
)

/** MeteringDeviceDto → MeteringDevice */
fun MeteringDeviceDto.toDomain(): MeteringDevice = MeteringDevice(
    key = key,
    name = value,
    nameService = nameService,
    number = number,
    counterBrand = counterBrand,
    factoryNumber = factoryNumber,
    countingPointKey = countingPointKey,
    dateTimePlomb = dateTimePlomb,
    numberPlomb = numberPlomb,
    typeDevice = typeDevice,
    uuidDevice = uuidDevice,
    scales = scales.map { it.toDomain() },
)

/** ScaleDto → Scale */
fun ScaleDto.toDomain(): Scale = Scale(
    key = key,
    name = value,
    nameScale = nameScale,
    unitType = unitType,
    uuidScaleDevices = uuidScaleDevices,
    testimonies = testimonies.map { it.toDomain() },
)

/** TestimonyDto → Testimony */
fun TestimonyDto.toDomain(): Testimony = Testimony(
    key = key,
    idCountingPointKey = idCountingPointKey,
    idScale = idScale,
    uuidTariffZone = uuidTariffZone,
    index = index,
    nameScale = nameScale,
    nameTariff = nameTariff,
    previousTestimony = previousTestimony,
    currentTestimony = currentTestimony,
    dateTimePrevious = dateTimePrevious,
    x = x,
    y = y,
)

/** Domain → DTO для отправки (InfoSubscriber → SendInfoDto часть) */
fun InfoSubscriber.toSendSubscriberDto(): SendSubscriberDto = SendSubscriberDto(
    subscriber = SendSubDto(
        phone = subscriber.phone,
        uuidAccount = subscriber.uuidAccount,
        isCreateAct = meteringDevices.any { it.actCheck?.isCreateAct == true },
        isCreateActAccess = meteringDevices.any { it.actAccess?.isCreateActAccess == true },
    ),
    datePayment = LocalDateTime.now().toString(),
    amountOfPayment = "0",
    meteringDevices = meteringDevices.map { it.toSendDto() },
)

/** MeteringDevice → SendMeteringDeviceDto */
fun MeteringDevice.toSendDto(): SendMeteringDeviceDto = SendMeteringDeviceDto(
    numberDevice = number,
    factoryNumber = factoryNumber,
    nameService = nameService,
    uuidDevice = uuidDevice,
    countingPointKey = countingPointKey,
    actCheck = actCheck?.toSendDto() ?: SendActCheckDto(),
    actAccess = actAccess?.toSendDto() ?: SendActAccessDto(),
    scales = scales.map { it.toSendDto() },
)

fun ActCheck.toSendDto(): SendActCheckDto = SendActCheckDto(
    dateTimeCheck = dateTimeCheck.toString(),
    roomCheck = roomCheck,
    sealIntegrity = sealIntegrity,
    revealed = inspectionResult,
    reconciliation = factTestimony,
    consentTestimony = isNoAgreesTestimony.toString(),
    causeNoAgreeCheck = reasonForDisagreeing,
    unauthorizedPersonsCheck = unauthorizedPersonsCheck,
    verificationDate = verificationDate.toString(),
    isCreateAct = isCreateAct,
)

fun ActAccess.toSendDto(): SendActAccessDto = SendActAccessDto(
    dateTimeAccess = dateTimeAccess.toString(),
    roomAccess = roomAccess,
    agreeAccess = agreeAccess,
    causeNoAgreeAccess = causeNoAgreeAccess,
    unauthorizedPersonsAccess = unauthorizedPersonsAccess,
    isCreateActAccess = isCreateActAccess,
)

fun Scale.toSendDto(): SendScaleDto = SendScaleDto(
    unitType = unitType,
    uuidScaleDevices = uuidScaleDevices,
    testimonies = testimonies.map { it.toSendDto() },
)

fun Testimony.toSendDto(): SendTestimonyDto = SendTestimonyDto(
    currentTestimony = currentTestimony.toIntOrNull() ?: 0,
    dateTimeCurrentTestimony = dateTimeCurrent.toString(),
    index = index,
    image = "", // Will be set from photo encoder
    nameTariff = nameTariff,
    uuidTariffZone = uuidTariffZone,
    coup = coup,
)
