package com.bttools.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bttools.app.SettingsManager
import com.bttools.app.core.Direction
import com.bttools.app.core.TerminalEngine
import com.bttools.app.core.TextEncoding

@Composable
fun LogsScreen(
    engine: TerminalEngine,
    settingsManager: SettingsManager,
    onExport: (String) -> Unit
) {
    val settings by settingsManager.settings.collectAsStateCompat()
    val encoding = runCatching { TextEncoding.valueOf(settings.defaultEncoding) }.getOrDefault(TextEncoding.UTF8)

    var hexMode by remember { mutableStateOf(settings.hexDisplay) }
    var filter by remember { mutableStateOf<Direction?>(null) } // null=全部

    val filtered = remember(engine.logs.size, filter) {
        if (filter == null) engine.logs.toList() else engine.logs.filter { it.direction == filter || it.direction == Direction.INFO }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filtered.size) {
        if (filtered.isNotEmpty()) listState.animateScrollToItem(filtered.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("消息日志", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = {
                    val text = buildString {
                        appendLine("# bttools 日志导出  RX=${engine.rxBytes} TX=${engine.txBytes}")
                        engine.logs.forEach { appendLine(it.renderForExport(encoding)) }
                    }
                    onExport(text)
                }) { Icon(Icons.Filled.Share, contentDescription = "导出", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { engine.clearLogs() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "清空", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("全部") })
            FilterChip(selected = filter == Direction.RX, onClick = { filter = Direction.RX }, label = { Text("接收") })
            FilterChip(selected = filter == Direction.TX, onClick = { filter = Direction.TX }, label = { Text("发送") })
            FilterChip(selected = hexMode, onClick = { hexMode = !hexMode }, label = { Text("HEX") })
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无消息记录", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(filtered) { entry ->
                    Text(
                        text = entry.render(hexMode, true, encoding),
                        modifier = Modifier.padding(vertical = 1.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = entry.color()
                    )
                }
            }
        }
    }
}
