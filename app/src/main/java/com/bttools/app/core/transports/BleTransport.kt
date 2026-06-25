package com.bttools.app.core.transports

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import com.bttools.app.core.ConnectionState
import com.bttools.app.core.Transport
import com.bttools.app.core.TransportListener
import com.bttools.app.core.TransportType
import java.util.UUID

/** 一个可选的 BLE 特征描述（供 UI 列表展示与手动选择）。 */
data class BleCharInfo(
    val serviceUuid: UUID,
    val charUuid: UUID,
    val canWrite: Boolean,
    val canNotify: Boolean
) {
    val props: String
        get() = buildList {
            if (canWrite) add("写")
            if (canNotify) add("通知")
        }.joinToString("/").ifEmpty { "读" }
}

/**
 * BLE (GATT) 链路。连接后发现服务，自动选择首个可写特征用于发送、
 * 首个 notify/indicate 特征用于接收，并把全部特征暴露给 UI 以便手动切换。
 */
@SuppressLint("MissingPermission")
class BleTransport(
    private val context: Context,
    private val device: BluetoothDevice,
    private val listener: TransportListener
) : Transport {

    override val type = TransportType.BLE

    private var gatt: BluetoothGatt? = null
    @Volatile private var state = ConnectionState.IDLE

    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    /** UI 可观察的特征列表。 */
    val characteristics = mutableStateListOf<BleCharInfo>()

    override fun isConnected(): Boolean = state == ConnectionState.CONNECTED

    override fun open() {
        state = ConnectionState.CONNECTING
        listener.onStateChanged(state, deviceLabel())
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, callback)
        }
    }

    override fun send(data: ByteArray) {
        val g = gatt ?: return
        val c = writeChar ?: run { listener.onInfo("未选择可写特征"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val type = if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            g.writeCharacteristic(c, data, type)
        } else {
            @Suppress("DEPRECATION")
            c.value = data
            @Suppress("DEPRECATION")
            g.writeCharacteristic(c)
        }
    }

    override fun close() {
        try { gatt?.disconnect() } catch (e: Exception) { }
        try { gatt?.close() } catch (e: Exception) { }
        gatt = null
        writeChar = null
        notifyChar = null
        characteristics.clear()
        state = ConnectionState.IDLE
        listener.onStateChanged(state, null)
    }

    /** 手动选择发送特征。 */
    fun selectWrite(info: BleCharInfo) {
        findChar(info)?.let { writeChar = it; listener.onInfo("发送特征: ${info.charUuid}") }
    }

    /** 手动选择并启用通知特征。 */
    fun selectNotify(info: BleCharInfo) {
        findChar(info)?.let { enableNotify(it); listener.onInfo("通知特征: ${info.charUuid}") }
    }

    private fun findChar(info: BleCharInfo): BluetoothGattCharacteristic? =
        gatt?.getService(info.serviceUuid)?.getCharacteristic(info.charUuid)

    private fun enableNotify(c: BluetoothGattCharacteristic) {
        val g = gatt ?: return
        g.setCharacteristicNotification(c, true)
        val cccd = c.getDescriptor(CCCD_UUID) ?: return
        val value = if ((c.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(cccd, value)
        } else {
            @Suppress("DEPRECATION")
            cccd.value = value
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }
        notifyChar = c
    }

    private fun deviceLabel(): String =
        try { "${device.name ?: "BLE 设备"} (${device.address})" } catch (e: SecurityException) { device.address }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    listener.onInfo("已连接，正在发现服务…")
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    state = ConnectionState.IDLE
                    listener.onStateChanged(state, null)
                    listener.onInfo("BLE 已断开")
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                listener.onInfo("服务发现失败 ($status)")
                return
            }
            characteristics.clear()
            for (service in g.services) {
                for (c in service.characteristics) {
                    val canWrite = (c.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                    val canNotify = (c.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                            BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                    characteristics.add(BleCharInfo(service.uuid, c.uuid, canWrite, canNotify))
                    if (canWrite && writeChar == null) writeChar = c
                    if (canNotify && notifyChar == null) enableNotify(c)
                }
            }
            state = ConnectionState.CONNECTED
            listener.onStateChanged(state, deviceLabel())
            listener.onInfo("发现 ${characteristics.size} 个特征" +
                    (if (writeChar != null) "，已自动选择发送特征" else "，未找到可写特征"))
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            c.value?.let { listener.onReceive(it) }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            listener.onReceive(value)
        }
    }

    companion object {
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
