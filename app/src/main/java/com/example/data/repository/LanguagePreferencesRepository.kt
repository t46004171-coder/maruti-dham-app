package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.marutiDataStore: DataStore<Preferences> by preferencesDataStore(name = "maruti_dham_prefs")

class LanguagePreferencesRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val keyLanguage = stringPreferencesKey("preferred_language")

    val languageFlow: Flow<String> = appContext.marutiDataStore.data.map { prefs ->
        val raw = prefs[keyLanguage] ?: "en"
        if (raw == "hi") "hi" else "en"
    }

    suspend fun setLanguage(languageCode: String) {
        val safeCode = if (languageCode == "hi") "hi" else "en"
        appContext.marutiDataStore.edit { prefs ->
            prefs[keyLanguage] = safeCode
        }
    }
}
