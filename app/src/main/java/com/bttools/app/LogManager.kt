package com.bttools.app

import android.util.Log

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}

object LogManager {
    private const val TAG = "BluetoothToolbox"

    fun log(level: LogLevel, category: String, message: String) {
        val fullMessage = "[$category] $message"
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, fullMessage)
            LogLevel.INFO -> Log.i(TAG, fullMessage)
            LogLevel.WARNING -> Log.w(TAG, fullMessage)
            LogLevel.ERROR -> Log.e(TAG, fullMessage)
        }
    }
}
