package com.karsa.browser.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.karsa.browser.browser.RenderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "karsa_settings")

object SettingsKeys {
    val ZOOM = intPreferencesKey("zoom_percent")
    val FORCE_ZOOM = booleanPreferencesKey("force_zoom")
    val FORCE_DARK = booleanPreferencesKey("force_dark")
    val AD_BLOCK = booleanPreferencesKey("ad_block")
    val ANTI_REDIRECT = booleanPreferencesKey("anti_redirect")
    val WALLET_WHITELIST = booleanPreferencesKey("wallet_whitelist")
    val PULL_TO_REFRESH_OFF = booleanPreferencesKey("ptr_off_per_site")
    val TRANSLATE = booleanPreferencesKey("translate")
    val TRANSLATE_TARGET = stringPreferencesKey("translate_target")
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val HOME_URL = stringPreferencesKey("home_url")
}

class SettingsStore(private val context: Context) {
    val settings: Flow<RenderSettings> = context.dataStore.data.map { p ->
        RenderSettings(
            zoomPercent = p[SettingsKeys.ZOOM] ?: 100,
            forceZoom = p[SettingsKeys.FORCE_ZOOM] ?: true,
            forceDarkMode = p[SettingsKeys.FORCE_DARK] ?: false,
            adBlockEnabled = p[SettingsKeys.AD_BLOCK] ?: true,
            antiRedirectEnabled = p[SettingsKeys.ANTI_REDIRECT] ?: true,
            walletWhitelistEnabled = p[SettingsKeys.WALLET_WHITELIST] ?: true,
            disablePullToRefreshPerSite = p[SettingsKeys.PULL_TO_REFRESH_OFF] ?: true,
            translateEnabled = p[SettingsKeys.TRANSLATE] ?: false,
            translateTarget = p[SettingsKeys.TRANSLATE_TARGET] ?: "id"
        )
    }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.DARK_THEME] ?: false }

    suspend fun update(transform: (RenderSettings) -> RenderSettings, current: RenderSettings) {
        val next = transform(current)
        context.dataStore.edit { p ->
            p[SettingsKeys.ZOOM] = next.zoomPercent
            p[SettingsKeys.FORCE_ZOOM] = next.forceZoom
            p[SettingsKeys.FORCE_DARK] = next.forceDarkMode
            p[SettingsKeys.AD_BLOCK] = next.adBlockEnabled
            p[SettingsKeys.ANTI_REDIRECT] = next.antiRedirectEnabled
            p[SettingsKeys.WALLET_WHITELIST] = next.walletWhitelistEnabled
            p[SettingsKeys.PULL_TO_REFRESH_OFF] = next.disablePullToRefreshPerSite
            p[SettingsKeys.TRANSLATE] = next.translateEnabled
            p[SettingsKeys.TRANSLATE_TARGET] = next.translateTarget
        }
    }

    suspend fun setDarkTheme(v: Boolean) {
        context.dataStore.edit { it[SettingsKeys.DARK_THEME] = v }
    }
}
