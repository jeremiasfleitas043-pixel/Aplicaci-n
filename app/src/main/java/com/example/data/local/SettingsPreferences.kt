package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clean_master_prefs")

data class UserSettings(
    val isDarkTheme: Boolean = true,
    val confirmClearData: Boolean = true
)

class SettingsPreferencesRepository(private val context: Context) {
    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val CONFIRM_CLEAR_DATA = booleanPreferencesKey("confirm_clear_data")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            confirmClearData = prefs[Keys.CONFIRM_CLEAR_DATA] ?: true
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = enabled
        }
    }

    suspend fun setConfirmClearData(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONFIRM_CLEAR_DATA] = enabled
        }
    }
}
