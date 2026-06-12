package com.bttools.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    var themeColor: Long = 0xFF1976D2,
    var isDarkMode: Boolean = false,
    var showDate: Boolean = true,
    var showTime: Boolean = true,
    var showBluetooth: Boolean = true,
    var developerName: String = "wu",
    var appVersion: String = "1.0.0"
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            themeColor = prefs.getLong(KEY_THEME_COLOR, 0xFF1976D2),
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
            showTime = prefs.getBoolean(KEY_SHOW_TIME, true),
            showBluetooth = prefs.getBoolean(KEY_SHOW_BLUETOOTH, true),
            developerName = prefs.getString(KEY_DEV_NAME, "wu") ?: "wu",
            appVersion = prefs.getString(KEY_VERSION, "1.0.0") ?: "1.0.0"
        )
    }

    fun saveThemeColor(color: Long) {
        prefs.edit().putLong(KEY_THEME_COLOR, color).apply()
        _settings.value = _settings.value.copy(themeColor = color)
    }

    fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _settings.value = _settings.value.copy(isDarkMode = enabled)
    }

    fun saveShowDate(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DATE, show).apply()
        _settings.value = _settings.value.copy(showDate = show)
    }

    fun saveShowTime(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TIME, show).apply()
        _settings.value = _settings.value.copy(showTime = show)
    }

    fun saveShowBluetooth(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BLUETOOTH, show).apply()
        _settings.value = _settings.value.copy(showBluetooth = show)
    }

    fun saveDeveloperName(name: String) {
        prefs.edit().putString(KEY_DEV_NAME, name).apply()
        _settings.value = _settings.value.copy(developerName = name)
    }

    fun saveAll(settings: AppSettings) {
        prefs.edit().apply {
            putLong(KEY_THEME_COLOR, settings.themeColor)
            putBoolean(KEY_DARK_MODE, settings.isDarkMode)
            putBoolean(KEY_SHOW_DATE, settings.showDate)
            putBoolean(KEY_SHOW_TIME, settings.showTime)
            putBoolean(KEY_SHOW_BLUETOOTH, settings.showBluetooth)
            putString(KEY_DEV_NAME, settings.developerName)
            putString(KEY_VERSION, settings.appVersion)
            apply()
        }
        _settings.value = settings
    }

    companion object {
        private const val PREFS_NAME = "bttools_settings"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_TIME = "show_time"
        private const val KEY_SHOW_BLUETOOTH = "show_bluetooth"
        private const val KEY_DEV_NAME = "dev_name"
        private const val KEY_VERSION = "app_version"
    }
}
