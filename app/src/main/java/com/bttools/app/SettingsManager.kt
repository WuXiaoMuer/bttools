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
    var appVersion: String = "2.0.0",
    // ---- 终端调试偏好 ----
    var defaultEncoding: String = "UTF8",     // TextEncoding 枚举名
    var defaultLineEnding: String = "NONE",   // LineEnding 枚举名
    var hexDisplay: Boolean = false,          // 接收以 HEX 显示
    var hexSend: Boolean = false,             // 发送框按 HEX 解析
    var autoScroll: Boolean = true,
    var showTimestamp: Boolean = true,
    var loopIntervalMs: Long = 1000,
    // ---- 链路配置（记忆上次输入）----
    var tcpHost: String = "192.168.4.1",
    var tcpPort: Int = 8080,
    var tcpServerMode: Boolean = false,
    var udpLocalPort: Int = 9000,
    var udpRemoteHost: String = "192.168.4.255",
    var udpRemotePort: Int = 9000,
    // ---- 可序列化集合（"||" 分隔）----
    var quickCommands: String = "AT||AT+VERSION||AT+RESET||AT+NAME?",
    var joystickCommands: String = "" // "id=cmd||id=cmd"，空表示用默认
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
            appVersion = prefs.getString(KEY_VERSION, "2.0.0") ?: "2.0.0",
            defaultEncoding = prefs.getString(KEY_ENCODING, "UTF8") ?: "UTF8",
            defaultLineEnding = prefs.getString(KEY_LINE_ENDING, "NONE") ?: "NONE",
            hexDisplay = prefs.getBoolean(KEY_HEX_DISPLAY, false),
            hexSend = prefs.getBoolean(KEY_HEX_SEND, false),
            autoScroll = prefs.getBoolean(KEY_AUTO_SCROLL, true),
            showTimestamp = prefs.getBoolean(KEY_SHOW_TS, true),
            loopIntervalMs = prefs.getLong(KEY_LOOP_MS, 1000),
            tcpHost = prefs.getString(KEY_TCP_HOST, "192.168.4.1") ?: "192.168.4.1",
            tcpPort = prefs.getInt(KEY_TCP_PORT, 8080),
            tcpServerMode = prefs.getBoolean(KEY_TCP_SERVER, false),
            udpLocalPort = prefs.getInt(KEY_UDP_LOCAL, 9000),
            udpRemoteHost = prefs.getString(KEY_UDP_RHOST, "192.168.4.255") ?: "192.168.4.255",
            udpRemotePort = prefs.getInt(KEY_UDP_RPORT, 9000),
            quickCommands = prefs.getString(KEY_QUICK_CMDS, "AT||AT+VERSION||AT+RESET||AT+NAME?")
                ?: "AT||AT+VERSION||AT+RESET||AT+NAME?",
            joystickCommands = prefs.getString(KEY_JOY_CMDS, "") ?: ""
        )
    }

    fun saveThemeColor(color: Long) = update(KEY_THEME_COLOR, color) { copy(themeColor = color) }
    fun saveDarkMode(v: Boolean) = update(KEY_DARK_MODE, v) { copy(isDarkMode = v) }
    fun saveShowDate(v: Boolean) = update(KEY_SHOW_DATE, v) { copy(showDate = v) }
    fun saveShowTime(v: Boolean) = update(KEY_SHOW_TIME, v) { copy(showTime = v) }
    fun saveShowBluetooth(v: Boolean) = update(KEY_SHOW_BLUETOOTH, v) { copy(showBluetooth = v) }
    fun saveDeveloperName(v: String) = update(KEY_DEV_NAME, v) { copy(developerName = v) }

    fun saveEncoding(v: String) = update(KEY_ENCODING, v) { copy(defaultEncoding = v) }
    fun saveLineEnding(v: String) = update(KEY_LINE_ENDING, v) { copy(defaultLineEnding = v) }
    fun saveHexDisplay(v: Boolean) = update(KEY_HEX_DISPLAY, v) { copy(hexDisplay = v) }
    fun saveHexSend(v: Boolean) = update(KEY_HEX_SEND, v) { copy(hexSend = v) }
    fun saveAutoScroll(v: Boolean) = update(KEY_AUTO_SCROLL, v) { copy(autoScroll = v) }
    fun saveShowTimestamp(v: Boolean) = update(KEY_SHOW_TS, v) { copy(showTimestamp = v) }
    fun saveLoopInterval(v: Long) = update(KEY_LOOP_MS, v) { copy(loopIntervalMs = v) }

    fun saveTcpHost(v: String) = update(KEY_TCP_HOST, v) { copy(tcpHost = v) }
    fun saveTcpPort(v: Int) = update(KEY_TCP_PORT, v) { copy(tcpPort = v) }
    fun saveTcpServerMode(v: Boolean) = update(KEY_TCP_SERVER, v) { copy(tcpServerMode = v) }
    fun saveUdpLocalPort(v: Int) = update(KEY_UDP_LOCAL, v) { copy(udpLocalPort = v) }
    fun saveUdpRemoteHost(v: String) = update(KEY_UDP_RHOST, v) { copy(udpRemoteHost = v) }
    fun saveUdpRemotePort(v: Int) = update(KEY_UDP_RPORT, v) { copy(udpRemotePort = v) }

    fun saveQuickCommands(v: String) = update(KEY_QUICK_CMDS, v) { copy(quickCommands = v) }
    fun saveJoystickCommands(v: String) = update(KEY_JOY_CMDS, v) { copy(joystickCommands = v) }

    private inline fun update(key: String, value: Any, transform: AppSettings.() -> AppSettings) {
        prefs.edit().apply {
            when (value) {
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
            }
            apply()
        }
        _settings.value = _settings.value.transform()
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
        private const val KEY_ENCODING = "term_encoding"
        private const val KEY_LINE_ENDING = "term_line_ending"
        private const val KEY_HEX_DISPLAY = "term_hex_display"
        private const val KEY_HEX_SEND = "term_hex_send"
        private const val KEY_AUTO_SCROLL = "term_auto_scroll"
        private const val KEY_SHOW_TS = "term_show_ts"
        private const val KEY_LOOP_MS = "term_loop_ms"
        private const val KEY_TCP_HOST = "tcp_host"
        private const val KEY_TCP_PORT = "tcp_port"
        private const val KEY_TCP_SERVER = "tcp_server_mode"
        private const val KEY_UDP_LOCAL = "udp_local_port"
        private const val KEY_UDP_RHOST = "udp_remote_host"
        private const val KEY_UDP_RPORT = "udp_remote_port"
        private const val KEY_QUICK_CMDS = "quick_commands"
        private const val KEY_JOY_CMDS = "joystick_commands"
    }
}
