package com.bttools.app

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bttools.app.core.ConnectionState
import com.bttools.app.core.LineEnding
import com.bttools.app.core.TerminalEngine
import com.bttools.app.core.TextEncoding
import com.bttools.app.core.TransportType
import com.bttools.app.core.transports.BleTransport
import com.bttools.app.core.transports.SppTransport
import com.bttools.app.core.transports.TcpTransport
import com.bttools.app.core.transports.UdpTransport
import com.bttools.app.ui.AboutScreen
import com.bttools.app.ui.BluetoothAppNav
import com.bttools.app.ui.ConnectScreen
import com.bttools.app.ui.JoystickScreen
import com.bttools.app.ui.LogsScreen
import com.bttools.app.ui.ScanDevice
import com.bttools.app.ui.SettingsScreen
import com.bttools.app.ui.StatusScreen
import com.bttools.app.ui.TerminalScreen
import com.bttools.app.ui.theme.BTToolsTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val REQUEST_PERMISSIONS = 200

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var settingsManager: SettingsManager
    private val engine = TerminalEngine()

    private var discoveryReceiver: BluetoothDiscoveryReceiver? = null
    private var isBluetoothSupported by mutableStateOf(true)

    // SPP 经典扫描状态
    private val pairedDevices = mutableStateListOf<ScanDevice>()
    private val classicDevices = mutableStateListOf<ScanDevice>()
    private var isDiscovering by mutableStateOf(false)

    // BLE 扫描状态
    private val bleDevices = mutableStateListOf<ScanDevice>()
    private var isBleScanning by mutableStateOf(false)
    private var bleTransport by mutableStateOf<BleTransport?>(null)
    private var pendingAction: (() -> Unit)? = null

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            displayPairedDevices()
            toast("蓝牙已开启")
        } else {
            toast("蓝牙未开启")
        }
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { toast("设备已设为可见") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            settingsManager = SettingsManager(this)
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) isBluetoothSupported = false

            enableEdgeToEdge()
            setContent {
                val currentSettings by settingsManager.settings.collectAsState()
                BTToolsTheme(
                    darkTheme = currentSettings.isDarkMode,
                    customPrimaryColor = currentSettings.themeColor
                ) {
                    if (!isBluetoothSupported) {
                        // 蓝牙不可用仍允许使用 TCP/UDP
                        AppScaffold(currentSettings)
                    } else {
                        AppScaffold(currentSettings)
                    }
                }
            }
            if (bluetoothAdapter?.isEnabled == true) displayPairedDevices()
        } catch (e: Exception) {
            e.printStackTrace()
            toast("应用初始化失败: ${e.message}")
        }
    }

    @Composable
    private fun AppScaffold(currentSettings: AppSettings) {
        BluetoothAppNav(
            statusContent = {
                StatusScreen(
                    engine = engine,
                    isBluetoothEnabled = bluetoothAdapter?.isEnabled == true,
                    onEnableBluetooth = ::enableBluetooth,
                    onMakeDiscoverable = ::makeDiscoverable,
                    showDate = currentSettings.showDate,
                    showTime = currentSettings.showTime,
                    showBluetooth = currentSettings.showBluetooth
                )
            },
            connectContent = {
                ConnectScreen(
                    engine = engine,
                    settingsManager = settingsManager,
                    isBluetoothEnabled = bluetoothAdapter?.isEnabled == true,
                    onEnableBluetooth = ::enableBluetooth,
                    pairedDevices = pairedDevices,
                    classicDevices = classicDevices,
                    isDiscovering = isDiscovering,
                    onScanClassic = ::searchDevices,
                    onConnectSpp = ::connectSpp,
                    bleDevices = bleDevices,
                    isBleScanning = isBleScanning,
                    onScanBle = ::startBleScan,
                    onStopBle = ::stopBleScan,
                    onConnectBle = ::connectBle,
                    bleTransport = bleTransport,
                    onConnectTcp = ::connectTcp,
                    onConnectUdp = ::connectUdp,
                    onDisconnect = ::disconnect
                )
            },
            terminalContent = { TerminalScreen(engine = engine, settingsManager = settingsManager) },
            joystickContent = {
                JoystickScreen(
                    isConnected = engine.isConnected,
                    remote = engine.remote,
                    initialCommands = currentSettings.joystickCommands,
                    onCommandsChanged = { settingsManager.saveJoystickCommands(it) },
                    onSendCommand = ::sendCommand
                )
            },
            logsContent = {
                LogsScreen(engine = engine, settingsManager = settingsManager, onExport = ::exportLogs)
            },
            settingsContent = { SettingsScreen(settingsManager = settingsManager) },
            aboutContent = {
                AboutScreen(
                    developerName = currentSettings.developerName,
                    appVersion = currentSettings.appVersion
                )
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopBleScan()
            engine.shutdown()
            discoveryReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---- 发送（摇杆/快捷）----
    private fun sendCommand(command: String) {
        val s = settingsManager.settings.value
        val enc = runCatching { TextEncoding.valueOf(s.defaultEncoding) }.getOrDefault(TextEncoding.UTF8)
        val le = runCatching { LineEnding.valueOf(s.defaultLineEnding) }.getOrDefault(LineEnding.NONE)
        engine.sendText(command, enc, le)
    }

    // ---- 连接 ----
    @SuppressLint("MissingPermission")
    private fun connectSpp(address: String) {
        if (!ensureConnectPermission { connectSpp(address) }) return
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        bleTransport = null
        engine.connect(SppTransport(device, engine))
    }

    @SuppressLint("MissingPermission")
    private fun connectBle(address: String) {
        if (!ensureConnectPermission { connectBle(address) }) return
        stopBleScan()
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        val t = BleTransport(this, device, engine)
        bleTransport = t
        engine.connect(t)
    }

    private fun connectTcp(serverMode: Boolean, host: String, port: Int) {
        if (port <= 0) { toast("端口无效"); return }
        if (!serverMode && host.isBlank()) { toast("请输入目标地址"); return }
        bleTransport = null
        val type = if (serverMode) TransportType.TCP_SERVER else TransportType.TCP_CLIENT
        engine.connect(TcpTransport(type, host, port, engine))
    }

    private fun connectUdp(localPort: Int, remoteHost: String, remotePort: Int) {
        if (remoteHost.isBlank() || remotePort <= 0) { toast("请填写远端地址与端口"); return }
        bleTransport = null
        engine.connect(UdpTransport(localPort, remoteHost, remotePort, engine))
    }

    private fun disconnect() {
        engine.close()
        bleTransport = null
    }

    // ---- 蓝牙开关 / 可见 ----
    private fun enableBluetooth() {
        if (bluetoothAdapter == null) { toast("设备不支持蓝牙"); return }
        if (bluetoothAdapter?.isEnabled == false) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            displayPairedDevices()
        }
    }

    private fun makeDiscoverable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        discoverableLauncher.launch(intent)
    }

    // ---- 经典发现 ----
    @SuppressLint("MissingPermission")
    private fun searchDevices() {
        if (bluetoothAdapter?.isEnabled != true) { toast("请先开启蓝牙"); return }
        if (!ensureScanPermission { searchDevices() }) return

        classicDevices.clear()
        discoveryReceiver?.let { runCatching { unregisterReceiver(it) } }
        discoveryReceiver = BluetoothDiscoveryReceiver(
            onFound = { name, address ->
                if (classicDevices.none { it.address == address }) {
                    classicDevices.add(ScanDevice(name, address))
                }
            },
            onStarted = { isDiscovering = true },
            onFinished = { isDiscovering = false }
        )
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_EXPORTED else 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(discoveryReceiver, BluetoothDiscoveryReceiver.makeFilter(), flags)
        } else {
            registerReceiver(discoveryReceiver, BluetoothDiscoveryReceiver.makeFilter())
        }
        if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter?.cancelDiscovery()
        if (bluetoothAdapter?.startDiscovery() != true) toast("启动扫描失败")
    }

    @SuppressLint("MissingPermission")
    private fun displayPairedDevices() {
        if (!ensureConnectPermission { displayPairedDevices() }) return
        pairedDevices.clear()
        bluetoothAdapter?.bondedDevices?.forEach { d ->
            val name = try { d.name } catch (e: SecurityException) { null } ?: "未知设备"
            pairedDevices.add(ScanDevice(name, d.address))
        }
    }

    // ---- BLE 扫描 ----
    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            val name = try { device.name } catch (e: SecurityException) { null }
                ?: result.scanRecord?.deviceName ?: "未知 BLE"
            val existing = bleDevices.indexOfFirst { it.address == device.address }
            val item = ScanDevice(name, device.address, result.rssi)
            if (existing >= 0) bleDevices[existing] = item else bleDevices.add(item)
        }

        override fun onScanFailed(errorCode: Int) {
            isBleScanning = false
            toast("BLE 扫描失败: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (bluetoothAdapter?.isEnabled != true) { toast("请先开启蓝牙"); return }
        if (!ensureScanPermission { startBleScan() }) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run { toast("不支持 BLE"); return }
        bleDevices.clear()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(null, settings, bleScanCallback)
        isBleScanning = true
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        if (!isBleScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
        } catch (e: Exception) { e.printStackTrace() }
        isBleScanning = false
    }

    // ---- 日志导出 ----
    private fun exportLogs(text: String) {
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = getExternalFilesDir(null) ?: cacheDir
            val file = File(dir, "bttools_log_$stamp.txt")
            file.writeText(text)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "bttools 日志")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(share, "导出日志"))
            toast("已保存到 ${file.absolutePath}")
        } catch (e: Exception) {
            toast("导出失败: ${e.message}")
        }
    }

    // ---- 权限 ----
    private fun ensureConnectPermission(retry: () -> Unit): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                pendingAction = retry
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSIONS)
                return false
            }
        }
        return true
    }

    private fun ensureScanPermission(retry: () -> Unit): Boolean {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) perms.add(android.Manifest.permission.BLUETOOTH_SCAN)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) perms.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (perms.isNotEmpty()) {
            pendingAction = retry
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQUEST_PERMISSIONS)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            val action = pendingAction
            pendingAction = null
            if (granted) action?.invoke() else toast("缺少必要权限")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
