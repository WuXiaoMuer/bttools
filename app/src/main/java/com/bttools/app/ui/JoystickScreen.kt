package com.bttools.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JoyButtonConfig(
    val id: String,
    val label: String,
    var command: String
)

@Composable
fun JoystickScreen(
    connectionStatus: String,
    connectedDeviceName: String?,
    onSendCommand: (String) -> Unit
) {
    val commands = remember {
        mutableStateListOf(
            JoyButtonConfig("up", "↑", "↑"),
            JoyButtonConfig("down", "↓", "↓"),
            JoyButtonConfig("left", "←", "←"),
            JoyButtonConfig("right", "→", "→"),
            JoyButtonConfig("up-left", "↖", "↖"),
            JoyButtonConfig("up-right", "↗", "↗"),
            JoyButtonConfig("down-left", "↙", "↙"),
            JoyButtonConfig("down-right", "↘", "↘"),
            JoyButtonConfig("stop", "●", "Z"),
            JoyButtonConfig("a", "A", "A"),
            JoyButtonConfig("b", "B", "B"),
            JoyButtonConfig("x", "X", "X"),
            JoyButtonConfig("y", "Y", "Y")
        )
    }

    val sentLog = remember { mutableStateListOf<String>() }
    var lastCommand by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<JoyButtonConfig?>(null) }
    var editText by remember { mutableStateOf("") }

    fun doSend(cmd: String) {
        onSendCommand(cmd)
        lastCommand = cmd
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        sentLog.add(0, "$time 发送: $cmd")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("蓝牙摇杆控制器", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { editMode = !editMode }) {
                Text(if (editMode) "完成" else "配置")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when {
                connectionStatus.contains("已连接") -> "已连接到: ${connectedDeviceName ?: "未知设备"}"
                connectionStatus.contains("正在连接") -> "正在连接..."
                else -> "未连接"
            },
            color = if (connectionStatus.contains("已连接")) MaterialTheme.colorScheme.primary
                    else if (connectionStatus.contains("正在连接")) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnalogJoystick(
                    commands = commands,
                    editMode = editMode,
                    onSend = { doSend(it) },
                    onEdit = { config ->
                        editingConfig = config
                        editText = config.command
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                JoyActionButtons(
                    commands = commands,
                    editMode = editMode,
                    onSend = { doSend(it) },
                    onEdit = { config ->
                        editingConfig = config
                        editText = config.command
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("发送记录", fontWeight = FontWeight.Bold)
                if (lastCommand.isNotEmpty()) {
                    Text(
                        text = "上次发送: $lastCommand",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (sentLog.isEmpty()) {
                    Text(
                        text = "暂无发送记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                sentLog.take(8).forEach { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    editingConfig?.let { config ->
        AlertDialog(
            onDismissRequest = { editingConfig = null },
            title = { Text("配置按钮: ${config.label}") },
            text = {
                Column {
                    Text("输入发送的指令内容：", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("指令") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    config.command = editText
                    editingConfig = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { editingConfig = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AnalogJoystick(
    commands: List<JoyButtonConfig>,
    editMode: Boolean,
    onSend: (String) -> Unit,
    onEdit: (JoyButtonConfig) -> Unit
) {
    val baseRadiusPx = 140f
    val thumbRadiusPx = 48f
    val sendIntervalMs = 150L

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var currentDirection by remember { mutableStateOf("stop") }
    var isDragging by remember { mutableStateOf(false) }
    var sendJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val stopConfig = commands.find { it.id == "stop" }

    fun angleToDirection(angleDeg: Float): String {
        val a = (angleDeg + 360) % 360
        return when {
            a < 22.5f || a >= 337.5f -> "right"
            a < 67.5f -> "down-right"
            a < 112.5f -> "down"
            a < 157.5f -> "down-left"
            a < 202.5f -> "left"
            a < 247.5f -> "up-left"
            a < 292.5f -> "up"
            a < 337.5f -> "up-right"
            else -> "stop"
        }
    }

    fun getDirectionFromOffset(off: Offset): String {
        return angleToDirection(
            Math.toDegrees(
                atan2(off.y.toDouble(), off.x.toDouble())
            ).toFloat()
        )
    }

    fun startSending(dir: String) {
        sendJob?.cancel()
        sendJob = scope.launch {
            val cfg = commands.find { it.id == dir }
            if (cfg != null) {
                onSend(cfg.command)
            }
            while (true) {
                delay(sendIntervalMs)
                val d = currentDirection
                if (d != "stop") {
                    commands.find { it.id == d }?.let { onSend(it.command) }
                }
            }
        }
    }

    fun stopSending() {
        sendJob?.cancel()
        sendJob = null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size((baseRadiusPx * 2 + 40).dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size((baseRadiusPx * 2).dp)
                    .pointerInput(editMode) {
                        if (editMode) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val delta = offset - center
                                val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
                                thumbOffset = if (dist > baseRadiusPx) {
                                    delta / dist * baseRadiusPx
                                } else {
                                    delta
                                }
                                isDragging = true
                                currentDirection = getDirectionFromOffset(thumbOffset)
                                startSending(currentDirection)
                            },
                            onDrag = { change, dragAmount ->
                                val newPos = thumbOffset + dragAmount
                                val dist = sqrt(newPos.x * newPos.x + newPos.y * newPos.y)
                                thumbOffset = if (dist > baseRadiusPx) {
                                    newPos / dist * baseRadiusPx
                                } else {
                                    newPos
                                }
                                change.consume()
                                val newDir = getDirectionFromOffset(thumbOffset)
                                if (newDir != currentDirection) {
                                    currentDirection = newDir
                                    startSending(newDir)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                thumbOffset = Offset.Zero
                                stopSending()
                                stopConfig?.let { onSend(it.command) }
                                currentDirection = "stop"
                            },
                            onDragCancel = {
                                isDragging = false
                                thumbOffset = Offset.Zero
                                stopSending()
                                currentDirection = "stop"
                            }
                        )
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(color = surfaceVariant, radius = baseRadiusPx)
                drawCircle(
                    color = primary.copy(alpha = 0.15f),
                    radius = baseRadiusPx,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.1f),
                    radius = baseRadiusPx * 0.5f,
                    style = Stroke(
                        width = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                )

                val cross = primary.copy(alpha = 0.2f)
                drawLine(cross, Offset(center.x - baseRadiusPx, center.y), Offset(center.x + baseRadiusPx, center.y), 1f)
                drawLine(cross, Offset(center.x, center.y - baseRadiusPx), Offset(center.x, center.y + baseRadiusPx), 1f)

                val thumbPos = center + thumbOffset
                val thumbAlpha = if (isDragging) 1f else 0.6f
                drawCircle(
                    color = primary.copy(alpha = thumbAlpha * 0.2f),
                    radius = thumbRadiusPx + 8f,
                    center = thumbPos
                )
                drawCircle(
                    color = primary,
                    radius = thumbRadiusPx,
                    center = thumbPos
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = thumbRadiusPx * 0.5f,
                    center = thumbPos + Offset(-thumbRadiusPx * 0.2f, -thumbRadiusPx * 0.2f)
                )
            }
        }

        if (isDragging && currentDirection != "stop") {
            val cmd = commands.find { it.id == currentDirection }
            val label = cmd?.command ?: currentDirection
            Text(
                text = "当前方向: $label",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (editMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "方向配置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            val dirIds = listOf(
                listOf("up-left", "up", "up-right"),
                listOf("left", "stop", "right"),
                listOf("down-left", "down", "down-right")
            )
            dirIds.forEach { rowIds ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    rowIds.forEach { id ->
                        commands.find { it.id == id }?.let { cfg ->
                            ConfigChip(
                                label = cfg.command,
                                onClick = { onEdit(cfg) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoyActionButtons(
    commands: List<JoyButtonConfig>,
    editMode: Boolean,
    onSend: (String) -> Unit,
    onEdit: (JoyButtonConfig) -> Unit
) {
    val aBtn = commands.find { it.id == "a" } ?: return
    val bBtn = commands.find { it.id == "b" } ?: return
    val xBtn = commands.find { it.id == "x" } ?: return
    val yBtn = commands.find { it.id == "y" } ?: return

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCircleButton(
                config = xBtn,
                editMode = editMode,
                onClick = { if (editMode) onEdit(xBtn) else onSend(xBtn.command) },
                color = MaterialTheme.colorScheme.tertiary
            )
            ActionCircleButton(
                config = aBtn,
                editMode = editMode,
                onClick = { if (editMode) onEdit(aBtn) else onSend(aBtn.command) },
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCircleButton(
                config = yBtn,
                editMode = editMode,
                onClick = { if (editMode) onEdit(yBtn) else onSend(yBtn.command) },
                color = MaterialTheme.colorScheme.secondary
            )
            ActionCircleButton(
                config = bBtn,
                editMode = editMode,
                onClick = { if (editMode) onEdit(bBtn) else onSend(bBtn.command) },
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActionCircleButton(
    config: JoyButtonConfig,
    editMode: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .then(if (editMode) Modifier.border(2.dp, color, CircleShape) else Modifier)
            .background(if (editMode) color.copy(alpha = 0.2f) else color, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = config.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (editMode) color else Color.White
            )
            if (editMode && config.command.isNotEmpty() && config.command != config.label) {
                Text(
                    text = config.command,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (editMode) color else Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ConfigChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
