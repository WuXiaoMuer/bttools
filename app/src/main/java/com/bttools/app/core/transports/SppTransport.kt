package com.bttools.app.core.transports

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.bttools.app.core.ConnectionState
import com.bttools.app.core.Transport
import com.bttools.app.core.TransportListener
import com.bttools.app.core.TransportType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * 蓝牙经典 SPP/RFCOMM 链路。
 * device 非空 → 主动连接；device 为空 → 作为服务端监听等待连接。
 * 迁移自原 BluetoothService 的 Accept/Connect/Connected 三线程模型。
 */
@SuppressLint("MissingPermission")
class SppTransport(
    private val device: BluetoothDevice?,
    private val listener: TransportListener
) : Transport {

    override val type = TransportType.SPP

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    @Volatile private var state = ConnectionState.IDLE

    override fun isConnected(): Boolean = state == ConnectionState.CONNECTED

    @Synchronized
    override fun open() {
        if (device != null) {
            updateState(ConnectionState.CONNECTING, deviceLabel(device))
            connectThread = ConnectThread(device).also { it.start() }
        } else {
            updateState(ConnectionState.LISTENING, "等待连接…")
            acceptThread = AcceptThread().also { it.start() }
        }
    }

    @Synchronized
    override fun send(data: ByteArray) {
        if (state != ConnectionState.CONNECTED) return
        connectedThread?.write(data)
    }

    @Synchronized
    override fun close() {
        connectThread?.cancel(); connectThread = null
        connectedThread?.cancel(); connectedThread = null
        acceptThread?.cancel(); acceptThread = null
        updateState(ConnectionState.IDLE, null)
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket, remote: BluetoothDevice) {
        connectThread?.cancel(); connectThread = null
        acceptThread?.cancel(); acceptThread = null
        connectedThread?.cancel()
        connectedThread = ConnectedThread(socket).also { it.start() }
        updateState(ConnectionState.CONNECTED, deviceLabel(remote))
    }

    private fun updateState(s: ConnectionState, remote: String?) {
        state = s
        listener.onStateChanged(s, remote)
    }

    private fun deviceLabel(d: BluetoothDevice): String =
        try { "${d.name ?: "未知设备"} (${d.address})" } catch (e: SecurityException) { d.address }

    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? = try {
            adapter?.listenUsingRfcommWithServiceRecord(NAME, SPP_UUID)
        } catch (e: IOException) { null }

        override fun run() {
            while (this@SppTransport.state != ConnectionState.CONNECTED) {
                val socket = try {
                    serverSocket?.accept()
                } catch (e: IOException) { break } ?: continue
                synchronized(this@SppTransport) {
                    if (this@SppTransport.state == ConnectionState.LISTENING) {
                        connected(socket, socket.remoteDevice)
                    } else {
                        try { socket.close() } catch (e: IOException) { }
                    }
                }
            }
        }

        fun cancel() { try { serverSocket?.close() } catch (e: IOException) { } }
    }

    private inner class ConnectThread(private val target: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? = try {
            target.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: IOException) { null }

        override fun run() {
            try { adapter?.cancelDiscovery() } catch (e: SecurityException) { }
            try {
                socket?.connect()
            } catch (e: IOException) {
                try { socket?.close() } catch (e2: IOException) { }
                updateState(ConnectionState.ERROR, null)
                listener.onInfo("无法连接设备")
                return
            }
            synchronized(this@SppTransport) { connectThread = null }
            if (socket != null) connected(socket, socket.remoteDevice)
        }

        fun cancel() { try { socket?.close() } catch (e: IOException) { } }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val input: InputStream? = try { socket.inputStream } catch (e: IOException) { null }
        private val output: OutputStream? = try { socket.outputStream } catch (e: IOException) { null }

        override fun run() {
            val buffer = ByteArray(4096)
            while (true) {
                try {
                    val n = input?.read(buffer) ?: break
                    if (n > 0) listener.onReceive(buffer.copyOf(n))
                } catch (e: IOException) {
                    updateState(ConnectionState.IDLE, null)
                    listener.onInfo("设备连接已断开")
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try { output?.write(bytes); output?.flush() }
            catch (e: IOException) { listener.onInfo("写入失败: ${e.message}") }
        }

        fun cancel() { try { socket.close() } catch (e: IOException) { } }
    }

    companion object {
        private const val NAME = "BluetoothToolbox"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
