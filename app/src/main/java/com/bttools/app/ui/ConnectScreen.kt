package com.bttools.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bttools.app.SettingsManager
import com.bttools.app.core.ConnectionState
import com.bttools.app.core.NetworkUtils
import com.bttools.app.core.TerminalEngine
import com.bttools.app.core.transports.BleCharInfo
import com.bttools.app.core.transports.BleTransport

/** 扫描到的设备项。 */
data class ScanDevice(val name: String, val address: String, val rssi: Int? = null)

enum class LinkTab(val title: String) { SPP("蓝牙SPP"), BLE("BLE"), TCP("TCP"), UDP("UDP") }

@Composable
fun ConnectScreen(
    engine: TerminalEngine,
    settingsManager: SettingsManager,
    isBluetoothEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    // SPP
    pairedDevices: List<ScanDevice>,
    classicDevices: List<ScanDevice>,
    isDiscovering: Boolean,
    onScanClassic: () -> Unit,
    onConnectSpp: (String) -> Unit,
    // BLE
    bleDevices: List<ScanDevice>,
    isBleScanning: Boolean,
    onScanBle: () -> Unit,
    onStopBle: () -> Unit,
    onConnectBle: (String) -> Unit,
    bleTransport: BleTransport?,
    // TCP / UDP
    onConnectTcp: (serverMode: Boolean, host: String, port: Int) -> Unit,
    onConnectUdp: (localPort: Int, remoteHost: String, remotePort: Int) -> Unit,
    onDisconnect: () -> Unit
) {
    val settings by settingsManager.settings.collectAsStateCompat()
    var tab by remember { mutableStateOf(LinkTab.SPP) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("连接", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // 当前连接状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = engine.currentType?.displayName ?: "未连接",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connStateText(engine.state, engine.remote),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (engine.isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (engine.state != ConnectionState.IDLE) {
                    OutlinedButton(onClick = onDisconnect) { Text("断开") }
                }
            }
        }

        // 链路类型选择
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinkTab.entries.forEach { t ->
                FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text(t.title) })
            }
        }

        when (tab) {
            LinkTab.SPP -> SppPane(
                isBluetoothEnabled, onEnableBluetooth,
                pairedDevices, classicDevices, isDiscovering, onScanClassic, onConnectSpp
            )
            LinkTab.BLE -> BlePane(
                isBluetoothEnabled, onEnableBluetooth,
                bleDevices, isBleScanning, onScanBle, onStopBle, onConnectBle, bleTransport
            )
            LinkTab.TCP -> TcpPane(settingsManager, settings.tcpHost, settings.tcpPort, settings.tcpServerMode, onConnectTcp)
            LinkTab.UDP -> UdpPane(settingsManager, settings.udpLocalPort, settings.udpRemoteHost, settings.udpRemotePort, onConnectUdp)
        }
    }
}

@Composable
private fun SppPane(
    isBluetoothEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    paired: List<ScanDevice>,
    found: List<ScanDevice>,
    isDiscovering: Boolean,
    onScan: () -> Unit,
    onConnect: (String) -> Unit
) {
    if (!isBluetoothEnabled) {
        Button(onClick = onEnableBluetooth, modifier = Modifier.fillMaxWidth()) { Text("开启蓝牙") }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("已配对 (${paired.size})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Button(onClick = onScan) { Text(if (isDiscovering) "搜索中…" else "搜索设备") }
    }
    if (isDiscovering) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    DeviceList(paired, onConnect)
    Text("发现的设备 (${found.size})", fontWeight = FontWeight.Bold)
    DeviceList(found, onConnect)
}

@Composable
private fun BlePane(
    isBluetoothEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    devices: List<ScanDevice>,
    isScanning: Boolean,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onConnect: (String) -> Unit,
    bleTransport: BleTransport?
) {
    if (!isBluetoothEnabled) {
        Button(onClick = onEnableBluetooth, modifier = Modifier.fillMaxWidth()) { Text("开启蓝牙") }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("BLE 设备 (${devices.size})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Button(onClick = if (isScanning) onStop else onScan) { Text(if (isScanning) "停止" else "扫描") }
    }
    if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    DeviceList(devices, onConnect)

    // 已连接 BLE 的特征选择
    val chars = bleTransport?.characteristics ?: emptyList<BleCharInfo>()
    if (chars.isNotEmpty()) {
        Text("GATT 特征 (${chars.size})", fontWeight = FontWeight.Bold)
        Text(
            "点选「写」用于发送，「通知」用于接收",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        chars.forEach { c ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(shortUuid(c.charUuid.toString()), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                        Text("属性: ${c.props}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (c.canWrite) OutlinedButton(onClick = { bleTransport?.selectWrite(c) }) { Text("写") }
                        if (c.canNotify) OutlinedButton(onClick = { bleTransport?.selectNotify(c) }) { Text("订阅") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TcpPane(
    settingsManager: SettingsManager,
    initHost: String,
    initPort: Int,
    initServerMode: Boolean,
    onConnect: (Boolean, String, Int) -> Unit
) {
    var serverMode by remember { mutableStateOf(initServerMode) }
    var host by remember { mutableStateOf(initHost) }
    var port by remember { mutableStateOf(initPort.toString()) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("服务端模式", modifier = Modifier.weight(1f))
        Switch(checked = serverMode, onCheckedChange = {
            serverMode = it; settingsManager.saveTcpServerMode(it)
        })
    }
    if (serverMode) {
        Text(
            "本机 IP: ${NetworkUtils.getLocalIpv4() ?: "未知"}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    } else {
        OutlinedTextField(
            value = host, onValueChange = { host = it; settingsManager.saveTcpHost(it) },
            label = { Text("目标 IP / 主机") }, modifier = Modifier.fillMaxWidth()
        )
    }
    OutlinedTextField(
        value = port,
        onValueChange = { port = it.filter { c -> c.isDigit() }; port.toIntOrNull()?.let(settingsManager::saveTcpPort) },
        label = { Text(if (serverMode) "监听端口" else "目标端口") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = { onConnect(serverMode, host.trim(), port.toIntOrNull() ?: 0) },
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (serverMode) "启动服务端" else "连接") }
}

@Composable
private fun UdpPane(
    settingsManager: SettingsManager,
    initLocal: Int,
    initRemoteHost: String,
    initRemotePort: Int,
    onConnect: (Int, String, Int) -> Unit
) {
    var localPort by remember { mutableStateOf(initLocal.toString()) }
    var remoteHost by remember { mutableStateOf(initRemoteHost) }
    var remotePort by remember { mutableStateOf(initRemotePort.toString()) }

    Text(
        "本机 IP: ${NetworkUtils.getLocalIpv4() ?: "未知"}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
    OutlinedTextField(
        value = localPort,
        onValueChange = { localPort = it.filter { c -> c.isDigit() }; localPort.toIntOrNull()?.let(settingsManager::saveUdpLocalPort) },
        label = { Text("本地监听端口") }, modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = remoteHost, onValueChange = { remoteHost = it; settingsManager.saveUdpRemoteHost(it) },
        label = { Text("远端 IP（广播用 .255）") }, modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = remotePort,
        onValueChange = { remotePort = it.filter { c -> c.isDigit() }; remotePort.toIntOrNull()?.let(settingsManager::saveUdpRemotePort) },
        label = { Text("远端端口") }, modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = {
            onConnect(localPort.toIntOrNull() ?: 0, remoteHost.trim(), remotePort.toIntOrNull() ?: 0)
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("启动 UDP") }
}

@Composable
private fun DeviceList(devices: List<ScanDevice>, onConnect: (String) -> Unit) {
    if (devices.isEmpty()) {
        Text("（无）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
        items(devices) { d ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onConnect(d.address) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(d.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        d.rssi?.let { Text("${it} dBm", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

private fun connStateText(state: ConnectionState, remote: String?): String = when (state) {
    ConnectionState.CONNECTED -> "已连接: ${remote ?: ""}"
    ConnectionState.CONNECTING -> "连接中…"
    ConnectionState.LISTENING -> "监听中: ${remote ?: ""}"
    ConnectionState.ERROR -> "出错"
    ConnectionState.IDLE -> "空闲"
}

private fun shortUuid(uuid: String): String {
    // 标准 16-bit UUID 显示短格式
    return if (uuid.length == 36 && uuid.startsWith("0000") && uuid.endsWith("-0000-1000-8000-00805f9b34fb")) {
        "0x" + uuid.substring(4, 8).uppercase()
    } else uuid
}
