package com.bttools.app

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bttools.app.ui.theme.BTToolsTheme
import com.bttools.app.ui.AboutScreen
import com.bttools.app.ui.BluetoothAppNav
import com.bttools.app.ui.DevicesScreen
import com.bttools.app.ui.JoystickScreen
import com.bttools.app.ui.LogsScreen
import com.bttools.app.ui.ScanScreen
import com.bttools.app.ui.SettingsScreen
import com.bttools.app.ui.StatusScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val REQUEST_ACCESS_FINE_LOCATION = 101
    private val REQUEST_BLUETOOTH_CONNECT = 102
    private val REQUEST_BLUETOOTH_SCAN = 103
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothService: BluetoothService? = null
    private var connectedDeviceName: String? = null
    private var discoveryReceiver: BluetoothDiscoveryReceiver? = null
    
    private lateinit var settingsManager: SettingsManager

    private val pairedDevices = mutableStateListOf<String>()
    private val availableDevices = mutableStateListOf<String>()
    private var isDiscovering by mutableStateOf(false)
    private val receivedMessages = mutableStateListOf<String>()
    private var connectionStatus by mutableStateOf("未连接")
    private var messageToSend by mutableStateOf("")
    private var isBluetoothSupported by mutableStateOf(true)

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            initializeBluetoothService()
            displayPairedDevices()
            Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "蓝牙已关闭，无法使用应用功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Toast.makeText(this, "设备已设为可见（300秒）", Toast.LENGTH_SHORT).show()
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            try {
                when (msg.what) {
                    BluetoothService.MESSAGE_STATE_CHANGE -> {
                        connectionStatus = when (msg.arg1) {
                            BluetoothService.STATE_CONNECTED -> "已连接到: $connectedDeviceName"
                            BluetoothService.STATE_CONNECTING -> "正在连接..."
                            else -> "未连接"
                        }
                    }
                    BluetoothService.MESSAGE_WRITE -> {
                        val writeBuf = msg.obj as ByteArray
                        val writeMessage = String(writeBuf)
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        receivedMessages.add("$time [发送] $writeMessage")
                    }
                    BluetoothService.MESSAGE_READ -> {
                        val readBuf = msg.obj as ByteArray
                        val readMessage = String(readBuf, 0, msg.arg1)
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        receivedMessages.add("$time [接收] $readMessage")
                    }
                    BluetoothService.MESSAGE_DEVICE_NAME -> {
                        connectedDeviceName = msg.data.getString(BluetoothService.DEVICE_NAME)
                        Toast.makeText(applicationContext, "连接到 $connectedDeviceName", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothService.MESSAGE_TOAST -> {
                        Toast.makeText(applicationContext, msg.data.getString(BluetoothService.TOAST),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "处理消息时出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            settingsManager = SettingsManager(this)
            val settings = settingsManager.settings.value

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                isBluetoothSupported = false
                Toast.makeText(this, "您的设备不支持蓝牙", Toast.LENGTH_LONG).show()
            }

            enableEdgeToEdge()
            setContent {
                val currentSettings by settingsManager.settings.collectAsState()
                
                BTToolsTheme(
                    darkTheme = currentSettings.isDarkMode,
                    customPrimaryColor = currentSettings.themeColor
                ) {
                    if (!isBluetoothSupported) {
                        Scaffold { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("您的设备不支持蓝牙功能，无法使用此应用") }
                        }
                    } else {
                        BluetoothAppNav(
                            statusContent = {
                                StatusScreen(
                                    connectionStatus = connectionStatus,
                                    connectedDeviceName = connectedDeviceName,
                                    isBluetoothEnabled = bluetoothAdapter?.isEnabled == true,
                                    onEnableBluetooth = ::enableBluetooth,
                                    onMakeDiscoverable = ::makeDiscoverable,
                                    showDate = currentSettings.showDate,
                                    showTime = currentSettings.showTime,
                                    showBluetooth = currentSettings.showBluetooth
                                )
                            },
                            scanContent = {
                                ScanScreen {
                                    BluetoothAssistantUI(
                                        modifier = Modifier,
                                        pairedDevices = pairedDevices,
                                        availableDevices = availableDevices,
                                        receivedMessages = receivedMessages,
                                        connectionStatus = connectionStatus,
                                        isDiscovering = isDiscovering,
                                        messageToSend = messageToSend,
                                        onMessageChange = { messageToSend = it },
                                        onEnableBluetooth = ::enableBluetooth,
                                        onMakeDiscoverable = ::makeDiscoverable,
                                        onSearchDevices = ::searchDevices,
                                        onSendMessage = ::sendMessage,
                                        onDeviceClick = ::connectToDevice
                                    )
                                }
                            },
                            devicesContent = {
                                DevicesScreen(
                                    pairedDevices = pairedDevices,
                                    availableDevices = availableDevices,
                                    onSearchDevices = ::searchDevices,
                                    isDiscovering = isDiscovering,
                                    onDeviceClick = ::connectToDevice
                                )
                            },
                            joystickContent = {
                                JoystickScreen(
                                    connectionStatus = connectionStatus,
                                    connectedDeviceName = connectedDeviceName,
                                    onSendCommand = ::sendCommand
                                )
                            },
                            logsContent = {
                                LogsScreen(
                                    messages = receivedMessages,
                                    onClear = { receivedMessages.clear() }
                                )
                            },
                            settingsContent = {
                                SettingsScreen(settingsManager = settingsManager)
                            },
                            aboutContent = {
                                AboutScreen(
                                    developerName = currentSettings.developerName,
                                    appVersion = currentSettings.appVersion
                                )
                            }
                        )
                    }
                }
            }

            if (isBluetoothSupported) {
                initializeBluetoothService()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "应用初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeBluetoothService() {
        try {
            if (bluetoothService == null) {
                bluetoothService = BluetoothService(this, handler)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化蓝牙服务失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            if (isBluetoothSupported && bluetoothAdapter?.isEnabled == true) {
                bluetoothService?.start()
                displayPairedDevices()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "应用启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothService?.stop()
            discoveryReceiver?.let {
                unregisterReceiver(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enableBluetooth() {
        try {
            if (bluetoothAdapter?.isEnabled == false) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtLauncher.launch(intent)
            } else {
                Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show()
                bluetoothService?.start()
                displayPairedDevices()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "开启蓝牙失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeDiscoverable() {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            discoverableLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "设置可见性失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION
                )
                false
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun checkBluetoothConnectPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_CONNECT
                    )
                    return false
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun checkBluetoothScanPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.BLUETOOTH_SCAN),
                        REQUEST_BLUETOOTH_SCAN
                    )
                    return false
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun searchDevices() {
        try {
            if (bluetoothAdapter?.isEnabled != true) {
                Toast.makeText(this, "请先开启蓝牙再搜索", Toast.LENGTH_SHORT).show()
                return
            }

            if (!checkBluetoothScanPermission()) {
                Toast.makeText(this, "需要蓝牙扫描权限才能搜索设备", Toast.LENGTH_SHORT).show()
                return
            }

            if (!checkLocationPermission()) {
                Toast.makeText(this, "需要位置权限才能搜索蓝牙设备", Toast.LENGTH_SHORT).show()
                return
            }

            availableDevices.clear()

            discoveryReceiver?.let {
                try {
                    unregisterReceiver(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            discoveryReceiver = BluetoothDiscoveryReceiver(
                availableDevices,
                onStarted = { isDiscovering = true },
                onFinished = { isDiscovering = false }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(discoveryReceiver, BluetoothDiscoveryReceiver.makeFilter(), Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(discoveryReceiver, BluetoothDiscoveryReceiver.makeFilter())
            }

            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }

            val started = bluetoothAdapter?.startDiscovery() == true
            if (!started) {
                Toast.makeText(this, "启动蓝牙扫描失败", Toast.LENGTH_SHORT).show()
                return
            }
            isDiscovering = true
            Toast.makeText(this, "开始搜索设备...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "搜索设备失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayPairedDevices() {
        try {
            pairedDevices.clear()

            if (!checkBluetoothConnectPermission()) {
                return
            }

            val pairedDevicesSet = bluetoothAdapter?.bondedDevices
            pairedDevicesSet?.let { devices ->
                if (devices.isNotEmpty()) {
                    for (device in devices) {
                        pairedDevices.add("${device.name ?: "未知设备"}\n${device.address}")
                    }
                } else {
                    pairedDevices.add("没有已配对的设备")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "获取已配对设备失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage() {
        try {
            if (messageToSend.isBlank()) return

            if (bluetoothService?.getState() != BluetoothService.STATE_CONNECTED) {
                Toast.makeText(this, "未连接到设备", Toast.LENGTH_SHORT).show()
                return
            }

            val bytes = messageToSend.toByteArray()
            bluetoothService?.write(bytes)
            messageToSend = ""
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "发送消息失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommand(command: String) {
        try {
            if (bluetoothService?.getState() != BluetoothService.STATE_CONNECTED) {
                Toast.makeText(this, "未连接到设备", Toast.LENGTH_SHORT).show()
                return
            }
            val bytes = command.toByteArray()
            bluetoothService?.write(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "发送指令失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(deviceInfo: String) {
        try {
            if (!checkBluetoothConnectPermission()) {
                return
            }

            val address = deviceInfo.substring(deviceInfo.length - 17)
            val device = bluetoothAdapter?.getRemoteDevice(address)

            device?.let {
                bluetoothService?.connect(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "连接设备失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            when (requestCode) {
                REQUEST_ACCESS_FINE_LOCATION -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        searchDevices()
                    } else {
                        Toast.makeText(this, "需要位置权限才能搜索蓝牙设备", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_BLUETOOTH_SCAN -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        searchDevices()
                    } else {
                        Toast.makeText(this, "需要蓝牙扫描权限才能搜索设备", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_BLUETOOTH_CONNECT -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        displayPairedDevices()
                    } else {
                        Toast.makeText(this, "需要蓝牙连接权限才能使用此功能", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun BluetoothAssistantUI(
    modifier: Modifier,
    pairedDevices: List<String>,
    availableDevices: List<String>,
    receivedMessages: List<String>,
    connectionStatus: String,
    isDiscovering: Boolean = false,
    messageToSend: String,
    onMessageChange: (String) -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onSearchDevices: () -> Unit,
    onSendMessage: () -> Unit,
    onDeviceClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onEnableBluetooth, modifier = Modifier.weight(1f)) {
                Text("开启蓝牙")
            }
            Button(onClick = onMakeDiscoverable, modifier = Modifier.weight(1f)) {
                Text("设为可见")
            }
            Button(onClick = onSearchDevices, modifier = Modifier.weight(1f)) {
                Text("搜索设备")
            }
        }

        Text(
            text = connectionStatus,
            color = if (connectionStatus.contains("已连接")) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text("已配对设备 (${pairedDevices.size})", fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            items(pairedDevices) { device ->
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .clickable { onDeviceClick(device) }
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
        }

        Text("可用设备 (${availableDevices.size})", fontWeight = FontWeight.Bold)
        if (isDiscovering) {
            LinearProgressIndicator(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp))
        }
        LazyColumn(
            modifier = Modifier
                .height(240.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            items(availableDevices) { device ->
                Card(modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable { onDeviceClick(device) }
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageToSend,
                onValueChange = onMessageChange,
                label = { Text("输入消息") },
                placeholder = { Text("在此输入要发送的数据…") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onSendMessage,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "发送"
                )
                Text(" 发送")
            }
        }

        Text("消息记录", fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                receivedMessages.forEach { message ->
                    Text(text = message, modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
}
