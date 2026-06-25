package com.bttools.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val content: @Composable () -> Unit
)

@Composable
fun BluetoothAppNav(
    statusContent: @Composable () -> Unit,
    connectContent: @Composable () -> Unit,
    terminalContent: @Composable () -> Unit,
    joystickContent: @Composable () -> Unit,
    logsContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    aboutContent: @Composable () -> Unit
) {
    val items = listOf(
        BottomNavItem("状态", Icons.Filled.Info, statusContent),
        BottomNavItem("连接", Icons.Filled.Link, connectContent),
        BottomNavItem("终端", Icons.Filled.Terminal, terminalContent),
        BottomNavItem("摇杆", Icons.Filled.SportsEsports, joystickContent),
        BottomNavItem("日志", Icons.Filled.ListAlt, logsContent),
        BottomNavItem("设置", Icons.Filled.Settings, settingsContent),
        BottomNavItem("关于", Icons.Filled.Info, aboutContent)
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            items[selectedIndex].content()
        }
    }
}
