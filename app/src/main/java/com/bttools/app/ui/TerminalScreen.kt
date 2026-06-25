package com.bttools.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.bttools.app.core.ConnectionState
import com.bttools.app.core.LineEnding
import com.bttools.app.core.TerminalEngine
import com.bttools.app.core.TextEncoding

@Composable
fun TerminalScreen(
    engine: TerminalEngine,
    settingsManager: SettingsManager
) {
    val settings by settingsManager.settings.collectAsStateCompat()

    var input by remember { mutableStateOf("") }
    var hexDisplay by remember { mutableStateOf(settings.hexDisplay) }
    var hexSend by remember { mutableStateOf(settings.hexSend) }
    var showTs by remember { mutableStateOf(settings.showTimestamp) }
    var autoScroll by remember { mutableStateOf(settings.autoScroll) }
    var encoding by remember {
        mutableStateOf(runCatching { TextEncoding.valueOf(settings.defaultEncoding) }.getOrDefault(TextEncoding.UTF8))
    }
    var lineEnding by remember {
        mutableStateOf(runCatching { LineEnding.valueOf(settings.defaultLineEnding) }.getOrDefault(LineEnding.NONE))
    }
    var loopMs by remember { mutableStateOf(settings.loopIntervalMs.toString()) }
    var showAddQuick by remember { mutableStateOf(false) }

    // 发送时使用的编码：HEX 发送开关优先
    val sendEncoding = if (hexSend) TextEncoding.HEX else encoding
    val displayEncoding = encoding

    val listState = rememberLazyListState()
    LaunchedEffect(engine.logs.size, autoScroll) {
        if (autoScroll && engine.logs.isNotEmpty()) {
            listState.animateScrollToItem(engine.logs.size - 1)
        }
    }

    val quickCommands = settings.quickCommands.split("||").filter { it.isNotBlank() }

    fun doSend() {
        if (input.isEmpty()) return
        engine.sendText(input, sendEncoding, lineEnding)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // ---- 状态栏 ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = engine.currentType?.displayName ?: "未选择链路",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stateLabel(engine.state, engine.remote),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (engine.isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "RX ${engine.rxBytes}  TX ${engine.txBytes}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ---- 显示工具条 ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = hexDisplay, onClick = {
                hexDisplay = !hexDisplay; settingsManager.saveHexDisplay(hexDisplay)
            }, label = { Text("HEX显示") })
            FilterChip(selected = showTs, onClick = {
                showTs = !showTs; settingsManager.saveShowTimestamp(showTs)
            }, label = { Text("时间戳") })
            FilterChip(selected = autoScroll, onClick = {
                autoScroll = !autoScroll; settingsManager.saveAutoScroll(autoScroll)
            }, label = { Text("自动滚动") })
            Box(modifier = Modifier.weight(1f))
            IconButton(onClick = { engine.clearLogs() }) {
                Icon(Icons.Filled.Delete, contentDescription = "清空", tint = MaterialTheme.colorScheme.error)
            }
        }

        // ---- 收发记录 ----
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (engine.logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无收发数据", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(engine.logs) { entry ->
                        Text(
                            text = entry.render(hexDisplay, showTs, displayEncoding),
                            color = entry.color(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // ---- 快捷命令 ----
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { showAddQuick = true },
                label = { Text("添加") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            quickCommands.forEach { cmd ->
                AssistChip(
                    onClick = { engine.sendText(cmd, sendEncoding, lineEnding) },
                    label = { Text(cmd, maxLines = 1) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // ---- 编码 / 行尾 / 选项 ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnumDropdown(
                label = "编码",
                current = encoding.displayName,
                options = TextEncoding.entries.filter { it != TextEncoding.HEX }.map { it.displayName to it },
                onSelect = { encoding = it; settingsManager.saveEncoding(it.name) },
                modifier = Modifier.weight(1f)
            )
            EnumDropdown(
                label = "行尾",
                current = lineEnding.displayName,
                options = LineEnding.entries.map { it.displayName to it },
                onSelect = { lineEnding = it; settingsManager.saveLineEnding(it.name) },
                modifier = Modifier.weight(1.2f)
            )
            FilterChip(selected = hexSend, onClick = {
                hexSend = !hexSend; settingsManager.saveHexSend(hexSend)
            }, label = { Text("HEX发送") })
        }

        // ---- 输入与发送 ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(if (hexSend) "输入十六进制，如 AB CD 01" else "输入要发送的数据…") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            IconButton(
                onClick = { doSend() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "发送", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // ---- 定时循环发送 ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = loopMs,
                onValueChange = { loopMs = it.filter { c -> c.isDigit() } },
                label = { Text("周期 ms") },
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    if (engine.isLooping) {
                        engine.stopLoop()
                    } else {
                        val ms = loopMs.toLongOrNull() ?: 1000
                        settingsManager.saveLoopInterval(ms)
                        if (input.isNotEmpty()) engine.startLoop(input, sendEncoding, lineEnding, ms)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (engine.isLooping) "停止循环" else "定时发送")
            }
        }
    }

    if (showAddQuick) {
        InputDialog(
            title = "添加快捷命令",
            label = "命令内容",
            onConfirm = { value ->
                if (value.isNotBlank()) {
                    val newList = (quickCommands + value).joinToString("||")
                    settingsManager.saveQuickCommands(newList)
                }
                showAddQuick = false
            },
            onDismiss = { showAddQuick = false }
        )
    }
}

private fun stateLabel(state: ConnectionState, remote: String?): String = when (state) {
    ConnectionState.CONNECTED -> "已连接: ${remote ?: ""}"
    ConnectionState.CONNECTING -> "连接中…"
    ConnectionState.LISTENING -> "监听中: ${remote ?: ""}"
    ConnectionState.ERROR -> "连接出错"
    ConnectionState.IDLE -> "未连接"
}

@Composable
fun <T> EnumDropdown(
    label: String,
    current: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $current", maxLines = 1, fontSize = 12.sp)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (name, value) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onSelect(value); expanded = false
                })
            }
        }
    }
}
