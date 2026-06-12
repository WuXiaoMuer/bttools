package com.bttools.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DevicesScreen(
    pairedDevices: List<String>,
    availableDevices: List<String>,
    onSearchDevices: () -> Unit = {},
    isDiscovering: Boolean = false,
    onDeviceClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "设备列表",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onSearchDevices) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("搜索")
            }
        }

        if (isDiscovering) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Text("已配对设备 (${pairedDevices.size})", fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.height(200.dp)
        ) {
            items(pairedDevices) { device ->
                DeviceCard(device = device, onClick = { onDeviceClick(device) })
            }
        }

        Text("可用设备 (${availableDevices.size})", fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.height(200.dp)
        ) {
            items(availableDevices) { device ->
                DeviceCard(device = device, onClick = { onDeviceClick(device) })
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val lines = device.split("\n")
            val title = lines.getOrNull(0) ?: device
            val subtitle = lines.getOrNull(1) ?: ""
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
