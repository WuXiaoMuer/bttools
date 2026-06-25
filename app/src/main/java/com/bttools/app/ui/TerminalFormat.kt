package com.bttools.app.ui

import androidx.compose.ui.graphics.Color
import com.bttools.app.core.Direction
import com.bttools.app.core.HexUtils
import com.bttools.app.core.LogEntry
import com.bttools.app.core.TextEncoding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tsFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

/** 把一条收发记录渲染为单行文本。 */
fun LogEntry.render(hexMode: Boolean, showTimestamp: Boolean, encoding: TextEncoding): String {
    val ts = if (showTimestamp) tsFormat.format(Date(timestamp)) + " " else ""
    return when (direction) {
        Direction.INFO -> "$ts[系统] ${info ?: ""}"
        Direction.TX -> "$ts[发送] ${HexUtils.decodeForDisplay(data, hexMode, encoding)}"
        Direction.RX -> "$ts[接收] ${HexUtils.decodeForDisplay(data, hexMode, encoding)}"
    }
}

/** 导出用纯文本（始终带时间戳，附带 HEX 旁注便于离线分析）。 */
fun LogEntry.renderForExport(encoding: TextEncoding): String {
    val ts = tsFormat.format(Date(timestamp))
    return when (direction) {
        Direction.INFO -> "$ts\t[INFO]\t${info ?: ""}"
        Direction.TX -> "$ts\t[TX]\t${HexUtils.decodeForDisplay(data, false, encoding)}\t| ${HexUtils.bytesToHex(data)}"
        Direction.RX -> "$ts\t[RX]\t${HexUtils.decodeForDisplay(data, false, encoding)}\t| ${HexUtils.bytesToHex(data)}"
    }
}

object LogColors {
    val tx = Color(0xFF1E88E5)
    val rx = Color(0xFF43A047)
    val info = Color(0xFF9E9E9E)
}

fun LogEntry.color(): Color = when (direction) {
    Direction.TX -> LogColors.tx
    Direction.RX -> LogColors.rx
    Direction.INFO -> LogColors.info
}
