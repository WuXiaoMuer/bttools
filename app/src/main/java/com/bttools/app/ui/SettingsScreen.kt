package com.bttools.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bttools.app.AppSettings
import com.bttools.app.SettingsManager
import com.bttools.app.core.LineEnding
import com.bttools.app.core.TextEncoding

data class ThemeColor(
    val name: String,
    val color: Long
)

val themeColors = listOf(
    ThemeColor("经典蓝", 0xFF1976D2),
    ThemeColor("深海蓝", 0xFF0D47A1),
    ThemeColor("天蓝色", 0xFF03A9F4),
    ThemeColor("青蓝色", 0xFF00BCD4),
    ThemeColor("紫色", 0xFF9C27B0),
    ThemeColor("粉红色", 0xFFE91E63),
    ThemeColor("橙红色", 0xFFFF5722),
    ThemeColor("绿色", 0xFF4CAF50),
    ThemeColor("深绿色", 0xFF00796B),
    ThemeColor("灰色", 0xFF607D8B)
)

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager? = null
) {
    val settings by if (settingsManager != null) {
        settingsManager.settings.collectAsState()
    } else {
        remember { mutableStateOf(AppSettings()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("主题颜色", fontWeight = FontWeight.Bold)
                Text(
                    text = "选择应用主题颜色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themeColors.take(5).forEach { themeColor ->
                        ColorOption(
                            color = themeColor.color,
                            isSelected = settings.themeColor == themeColor.color,
                            onClick = { settingsManager?.saveThemeColor(themeColor.color) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themeColors.drop(5).forEach { themeColor ->
                        ColorOption(
                            color = themeColor.color,
                            isSelected = settings.themeColor == themeColor.color,
                            onClick = { settingsManager?.saveThemeColor(themeColor.color) }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("显示设置", fontWeight = FontWeight.Bold)
                
                SwitchSetting(
                    title = "深色模式",
                    description = "切换深色/浅色主题",
                    checked = settings.isDarkMode,
                    onCheckedChange = { settingsManager?.saveDarkMode(it) }
                )
                
                SwitchSetting(
                    title = "显示日期模块",
                    description = "在状态页面显示当前日期",
                    checked = settings.showDate,
                    onCheckedChange = { settingsManager?.saveShowDate(it) }
                )
                
                SwitchSetting(
                    title = "显示时间模块",
                    description = "在状态页面显示当前时间",
                    checked = settings.showTime,
                    onCheckedChange = { settingsManager?.saveShowTime(it) }
                )
                
                SwitchSetting(
                    title = "显示蓝牙模块",
                    description = "在状态页面显示蓝牙状态",
                    checked = settings.showBluetooth,
                    onCheckedChange = { settingsManager?.saveShowBluetooth(it) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("终端默认设置", fontWeight = FontWeight.Bold)
                Text(
                    text = "新连接终端的默认收发参数",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                val enc = runCatching { TextEncoding.valueOf(settings.defaultEncoding) }.getOrDefault(TextEncoding.UTF8)
                val le = runCatching { LineEnding.valueOf(settings.defaultLineEnding) }.getOrDefault(LineEnding.NONE)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnumDropdown(
                        label = "编码",
                        current = enc.displayName,
                        options = TextEncoding.entries.filter { it != TextEncoding.HEX }.map { it.displayName to it },
                        onSelect = { settingsManager?.saveEncoding(it.name) },
                        modifier = Modifier.weight(1f)
                    )
                    EnumDropdown(
                        label = "行尾",
                        current = le.displayName,
                        options = LineEnding.entries.map { it.displayName to it },
                        onSelect = { settingsManager?.saveLineEnding(it.name) },
                        modifier = Modifier.weight(1f)
                    )
                }

                SwitchSetting(
                    title = "默认 HEX 显示",
                    description = "接收数据默认以十六进制显示",
                    checked = settings.hexDisplay,
                    onCheckedChange = { settingsManager?.saveHexDisplay(it) }
                )
                SwitchSetting(
                    title = "默认 HEX 发送",
                    description = "发送框默认按十六进制解析",
                    checked = settings.hexSend,
                    onCheckedChange = { settingsManager?.saveHexSend(it) }
                )
                SwitchSetting(
                    title = "自动滚动",
                    description = "终端自动滚动到最新数据",
                    checked = settings.autoScroll,
                    onCheckedChange = { settingsManager?.saveAutoScroll(it) }
                )
                SwitchSetting(
                    title = "显示时间戳",
                    description = "每条收发记录显示时间",
                    checked = settings.showTimestamp,
                    onCheckedChange = { settingsManager?.saveShowTimestamp(it) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", fontWeight = FontWeight.Bold)
                Text(
                    text = "开发者: ${settings.developerName}",
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "版本: ${settings.appVersion}",
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "许可证: GPLv3",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color.toInt()))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}
