package com.bttools.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService(private val context: Context, private val handler: Handler) {
    companion object {
        private const val NAME = "BluetoothToolbox"
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5

        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"

        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var state = STATE_NONE

    fun getState(): Int = state

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    @Synchronized
    fun start() {
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }
        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread?.start()
        }
        state = STATE_LISTEN
    }

    @Synchronized
    fun connect(device: BluetoothDevice) {
        if (state == STATE_CONNECTING) {
            connectThread?.cancel()
            connectThread = null
        }
        connectedThread?.cancel()
        connectedThread = null

        connectThread = ConnectThread(device)
        connectThread?.start()
        state = STATE_CONNECTING
    }

    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice) {
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }
        if (acceptThread != null) {
            acceptThread?.cancel()
            acceptThread = null
        }

        connectedThread = ConnectedThread(socket!!)
        connectedThread?.start()

        val msg = handler.obtainMessage(MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(DEVICE_NAME, device.name)
        msg.data = bundle
        handler.sendMessage(msg)

        state = STATE_CONNECTED
    }

    @Synchronized
    fun stop() {
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }
        if (acceptThread != null) {
            acceptThread?.cancel()
            acceptThread = null
        }
        state = STATE_NONE
    }

    fun write(bytes: ByteArray) {
        val r: ConnectedThread?
        synchronized(this) {
            if (state != STATE_CONNECTED) return
            r = connectedThread
        }
        r?.write(bytes)
    }

    private fun connectionFailed() {
        state = STATE_LISTEN
        val msg = handler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "无法连接设备")
        msg.data = bundle
        handler.sendMessage(msg)
    }

    private fun connectionLost() {
        state = STATE_LISTEN
        val msg = handler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "设备连接已断开")
        msg.data = bundle
        handler.sendMessage(msg)
    }

    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            serverSocket = tmp
        }

        override fun run() {
            var socket: BluetoothSocket?
            while (this@BluetoothService.state != STATE_CONNECTED) {
                try {
                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    break
                }
                if (socket != null) {
                    synchronized(this@BluetoothService) {
                        when (this@BluetoothService.state) {
                            STATE_LISTEN, STATE_CONNECTING -> {
                                connected(socket, socket.remoteDevice)
                            }
                            STATE_NONE, STATE_CONNECTED -> {
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket?

        init {
            var tmp: BluetoothSocket? = null
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            socket = tmp
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                socket?.connect()
            } catch (e: IOException) {
                try {
                    socket?.close()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }
                connectionFailed()
                return
            }
            synchronized(this@BluetoothService) {
                connectThread = null
            }
            connected(socket, socket!!.remoteDevice)
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket = socket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = mmInStream?.read(buffer) ?: break
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer.clone())
                        .sendToTarget()
                } catch (e: IOException) {
                    connectionLost()
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer.clone())
                    .sendToTarget()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
