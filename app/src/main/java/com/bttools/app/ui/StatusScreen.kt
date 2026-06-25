package com.bttools.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bttools.app.core.ConnectionState
import com.bttools.app.core.NetworkUtils
import com.bttools.app.core.TerminalEngine
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(
    engine: TerminalEngine,
    isBluetoothEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    showDate: Boolean = true,
    showTime: Boolean = true,
    showBluetooth: Boolean = true
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE).format(now)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("状态", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // 当前链路总览
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lan, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text("当前链路", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoLine("类型", engine.currentType?.displayName ?: "未选择")
                InfoLine("状态", connText(engine.state, engine.remote))
                InfoLine("收 / 发", "${engine.rxBytes} / ${engine.txBytes} 字节")
            }
        }

        if (showDate) {
            StatusCard(Icons.Filled.CalendarMonth, "日期", currentDate, MaterialTheme.colorScheme.primary)
        }
        if (showTime) {
            StatusCard(Icons.Filled.Schedule, "时间", currentTime, MaterialTheme.colorScheme.secondary, largeText = true)
        }

        if (showBluetooth) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.Bluetooth, contentDescription = null,
                            tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("蓝牙状态", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoLine("蓝牙", if (isBluetoothEnabled) "已开启" else "已关闭")
                    InfoLine("本机 IP", NetworkUtils.getLocalIpv4() ?: "无网络")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEnableBluetooth, modifier = Modifier.weight(1f)) {
                    Text(if (isBluetoothEnabled) "蓝牙已开启" else "开启蓝牙")
                }
                Button(onClick = onMakeDiscoverable, modifier = Modifier.weight(1f)) { Text("设为可见") }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    content: String,
    iconTint: androidx.compose.ui.graphics.Color,
    largeText: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    content,
                    style = if (largeText) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
        }
    }
}

private fun connText(state: ConnectionState, remote: String?): String = when (state) {
    ConnectionState.CONNECTED -> "已连接: ${remote ?: ""}"
    ConnectionState.CONNECTING -> "连接中…"
    ConnectionState.LISTENING -> "监听中"
    ConnectionState.ERROR -> "出错"
    ConnectionState.IDLE -> "未连接"
}
