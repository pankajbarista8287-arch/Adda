package com.example.localization

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.addaDataStore by preferencesDataStore(name = "adda_prefs")

class LanguageStore(private val context: Context) {
    private val keyLang = stringPreferencesKey("app_lang")
    private val keySoundFx = booleanPreferencesKey("sound_fx_enabled")

    // Default language: Hindi ("hi")
    val languageFlow: Flow<AppLang> = context.addaDataStore.data.map { prefs ->
        when (prefs[keyLang]) {
            AppLang.EN.code -> AppLang.EN
            else -> AppLang.HI
        }
    }

    val soundFxFlow: Flow<Boolean> = context.addaDataStore.data.map { prefs ->
        prefs[keySoundFx] ?: true
    }

    suspend fun setLanguage(lang: AppLang) {
        context.addaDataStore.edit { prefs ->
            prefs[keyLang] = lang.code
        }
    }

    suspend fun setSoundFx(enabled: Boolean) {
        context.addaDataStore.edit { prefs ->
            prefs[keySoundFx] = enabled
        }
    }
}
