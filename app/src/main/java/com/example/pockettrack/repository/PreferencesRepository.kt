package com.example.pockettrack.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class PreferencesRepository(private val context: Context) {
    companion object {
        private val CURRENCY_KEY = stringPreferencesKey("currency")
        private val THEME_KEY    = stringPreferencesKey("theme_mode")
    }

    val currencyFlow: Flow<String>   = context.dataStore.data.map { it[CURRENCY_KEY] ?: "IDR" }
    val themeModeFlow: Flow<String>  = context.dataStore.data.map { it[THEME_KEY]    ?: "System" }

    suspend fun setCurrency(currency: String) { context.dataStore.edit { it[CURRENCY_KEY] = currency } }
    suspend fun setThemeMode(mode: String)    { context.dataStore.edit { it[THEME_KEY]    = mode    } }
}
