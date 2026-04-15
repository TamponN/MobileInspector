package com.bestplus.mobileinspector.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bestplus.mobileinspector.domain.model.ServerSettings
import com.bestplus.mobileinspector.domain.model.UserSession
import com.bestplus.mobileinspector.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Реализация SettingsRepository через DataStore.
 * Заменяет Xamarin Application.Current.Properties.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val SERVER_ADDRESS = stringPreferencesKey("server_address")
        val DATABASE_NAME = stringPreferencesKey("database_name")
        val USE_SSL = booleanPreferencesKey("use_ssl")
        val GUID = stringPreferencesKey("guid")
        val LOGIN = stringPreferencesKey("login")
        val TOKEN = stringPreferencesKey("token")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_minutes")
    }

    // --- Server Settings ---

    override fun observeServerSettings(): Flow<ServerSettings> =
        context.dataStore.data.map { prefs ->
            ServerSettings(
                address = prefs[Keys.SERVER_ADDRESS].orEmpty(),
                databaseName = prefs[Keys.DATABASE_NAME].orEmpty(),
                useSsl = prefs[Keys.USE_SSL] ?: false,
                guid = prefs[Keys.GUID].orEmpty(),
            )
        }

    override suspend fun getServerSettings(): ServerSettings =
        observeServerSettings().first()

    override suspend fun saveServerSettings(settings: ServerSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_ADDRESS] = settings.address
            prefs[Keys.DATABASE_NAME] = settings.databaseName
            prefs[Keys.USE_SSL] = settings.useSsl
            prefs[Keys.GUID] = settings.guid
        }
    }

    // --- User Session ---

    override fun observeUserSession(): Flow<UserSession?> =
        context.dataStore.data.map { prefs ->
            val login = prefs[Keys.LOGIN] ?: return@map null
            val token = prefs[Keys.TOKEN] ?: return@map null
            val guid = prefs[Keys.GUID].orEmpty()
            UserSession(login = login, token = token, guid = guid)
        }

    override suspend fun getUserSession(): UserSession? =
        observeUserSession().first()

    override suspend fun saveUserSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGIN] = session.login
            prefs[Keys.TOKEN] = session.token
            prefs[Keys.GUID] = session.guid
        }
    }

    override suspend fun clearUserSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.LOGIN)
            prefs.remove(Keys.TOKEN)
        }
    }

    // --- Sync ---

    override suspend fun getSyncIntervalMinutes(): Int =
        context.dataStore.data.first()[Keys.SYNC_INTERVAL] ?: 10

    override suspend fun setSyncIntervalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNC_INTERVAL] = minutes
        }
    }
}
