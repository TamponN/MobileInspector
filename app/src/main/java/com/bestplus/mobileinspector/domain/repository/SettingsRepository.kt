package com.bestplus.mobileinspector.domain.repository

import com.bestplus.mobileinspector.domain.model.ServerSettings
import com.bestplus.mobileinspector.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий настроек приложения.
 * Заменяет Application.Current.Properties из Xamarin.
 */
interface SettingsRepository {

    fun observeServerSettings(): Flow<ServerSettings>
    suspend fun getServerSettings(): ServerSettings
    suspend fun saveServerSettings(settings: ServerSettings)

    fun observeUserSession(): Flow<UserSession?>
    suspend fun getUserSession(): UserSession?
    suspend fun saveUserSession(session: UserSession)
    suspend fun clearUserSession()

    suspend fun getSyncIntervalMinutes(): Int
    suspend fun setSyncIntervalMinutes(minutes: Int)

    suspend fun getSelectedModelFileName(): String
    suspend fun saveSelectedModelFileName(fileName: String)
}
