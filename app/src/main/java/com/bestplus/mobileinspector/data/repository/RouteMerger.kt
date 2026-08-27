package com.bestplus.mobileinspector.data.repository

import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import com.bestplus.mobileinspector.domain.model.MeteringDevice
import com.bestplus.mobileinspector.domain.model.RouteSheet
import com.bestplus.mobileinspector.domain.model.Scale
import com.bestplus.mobileinspector.domain.model.Testimony

/**
 * Послойное слияние серверных и локальных маршрутных листов.
 *
 * Принцип: сервер — источник истины по составу и справочным данным
 * (адрес, ФИО, тарифы, предыдущие показания), локальные правки пользователя
 * (текущие показания, фото, акты, статус «Выполнена») сохраняются при обновлении.
 *
 * Это предотвращает потерю введённых в офлайне данных: 1С в GET всегда отдаёт
 * пустой CurrentTestimony, поэтому простая замена затирала бы правки.
 */
object RouteMerger {

    /** Сопоставление сущностей [items] по ключу [keyOf], с fallback на индекс. */
    private fun <T> indexByKey(items: List<T>, keyOf: (T) -> String): Map<String, T> {
        val byKey = HashMap<String, T>()
        items.forEachIndexed { idx, item ->
            val k = keyOf(item)
            // Пустой ключ не уникален — используем индекс, чтобы не перетирать
            val mapKey = if (k.isBlank()) "__idx_$idx" else k
            byKey.putIfAbsent(mapKey, item)
        }
        return byKey
    }

    /** Merge RouteSheet: справка с сервера, правки из local (если есть). */
    fun merge(server: RouteSheet, local: RouteSheet?): RouteSheet {
        // Нет локальной копии — берём серверные данные как есть
        if (local == null) return server

        val localSubs = indexByKey(local.subscribers) { it.uuid }
        val mergedSubs = server.subscribers.map { serverSub ->
            mergeSubscriber(serverSub, localSubs[serverSub.uuid])
        }
        return server.copy(subscribers = mergedSubs)
    }

    private fun mergeSubscriber(server: InfoSubscriber, local: InfoSubscriber?): InfoSubscriber {
        if (local == null) return server

        val localDevs = indexByKey(local.meteringDevices) { it.uuidDevice }
        val mergedDevs = server.meteringDevices.map { serverDev ->
            mergeDevice(serverDev, localDevs[serverDev.uuidDevice])
        }
        return server.copy(
            // Справка с сервера (address, subscriber, planDate, position…),
            // но статус задачи сохраняем локальный — он отражает работу пользователя
            statusTask = local.statusTask,
            meteringDevices = mergedDevs,
            photos = local.photos,
        )
    }

    private fun mergeDevice(server: MeteringDevice, local: MeteringDevice?): MeteringDevice {
        if (local == null) return server

        val localScales = indexByKey(local.scales) { it.uuidScaleDevices }
        val mergedScales = server.scales.map { serverScale ->
            mergeScale(serverScale, localScales[serverScale.uuidScaleDevices])
        }
        return server.copy(
            scales = mergedScales,
            // Акты — локальный ввод пользователя
            actCheck = local.actCheck,
            actAccess = local.actAccess,
        )
    }

    private fun mergeScale(server: Scale, local: Scale?): Scale {
        if (local == null) return server

        // Сопоставление показаний по uuid тарифной зоны, fallback на индекс
        val localTestimonies = HashMap<String, Testimony>()
        local.testimonies.forEachIndexed { idx, t ->
            val k = t.uuidTariffZone
            val mapKey = if (k.isBlank()) "__idx_$idx" else k
            localTestimonies.putIfAbsent(mapKey, t)
        }

        val mergedTestimonies = server.testimonies.mapIndexed { idx, serverT ->
            val mapKey = if (serverT.uuidTariffZone.isBlank()) "__idx_$idx" else serverT.uuidTariffZone
            mergeTestimony(serverT, localTestimonies[mapKey])
        }
        return server.copy(testimonies = mergedTestimonies)
    }

    private fun mergeTestimony(server: Testimony, local: Testimony?): Testimony {
        if (local == null) return server
        return server.copy(
            // Справка с сервера (previousTestimony, nameTariff, index…),
            // правки пользователя сохраняем локально
            currentTestimony = local.currentTestimony,
            picturePath = local.picturePath,
            coup = local.coup,
            dateTimeCurrent = local.dateTimeCurrent,
        )
    }
}
