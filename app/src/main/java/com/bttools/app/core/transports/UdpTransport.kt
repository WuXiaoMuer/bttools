package com.bttools.app.core.transports

import com.bttools.app.core.ConnectionState
import com.bttools.app.core.Transport
import com.bttools.app.core.TransportListener
import com.bttools.app.core.TransportType
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * UDP 链路。绑定本地端口接收（含广播），向远端 host:port 发送。
 * UDP 无连接，open() 后即视为"已连接"（可收发）。
 */
class UdpTransport(
    private val localPort: Int,
    private val remoteHost: String,
    private val remotePort: Int,
    private val listener: TransportListener
) : Transport {

    override val type = TransportType.UDP

    @Volatile private var state = ConnectionState.IDLE
    private var socket: DatagramSocket? = null
    private var worker: Thread? = null
    private val sendExecutor = Executors.newSingleThreadExecutor()

    override fun isConnected(): Boolean = state == ConnectionState.CONNECTED

    override fun open() {
        worker = Thread {
            try {
                val s = if (localPort > 0) DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(localPort))
                } else DatagramSocket().apply { broadcast = true }
                socket = s
                update(ConnectionState.CONNECTED, "本地:$localPort → $remoteHost:$remotePort")
                listener.onInfo("UDP 就绪 (监听 $localPort)")
                receiveLoop(s)
            } catch (e: Exception) {
                update(ConnectionState.ERROR, null)
                listener.onInfo("UDP 启动失败: ${e.message}")
            }
        }.also { it.start() }
    }

    private fun receiveLoop(s: DatagramSocket) {
        val buffer = ByteArray(65507)
        while (!s.isClosed) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                s.receive(packet)
                if (packet.length > 0) listener.onReceive(buffer.copyOf(packet.length))
            } catch (e: Exception) {
                break
            }
        }
        update(ConnectionState.IDLE, null)
    }

    override fun send(data: ByteArray) {
        val s = socket ?: return
        sendExecutor.execute {
            try {
                val addr = InetAddress.getByName(remoteHost)
                s.send(DatagramPacket(data, data.size, addr, remotePort))
            } catch (e: Exception) {
                listener.onInfo("UDP 发送失败: ${e.message}")
            }
        }
    }

    override fun close() {
        state = ConnectionState.IDLE
        try { socket?.close() } catch (e: Exception) { }
        try { sendExecutor.shutdownNow() } catch (e: Exception) { }
        socket = null
        listener.onStateChanged(ConnectionState.IDLE, null)
    }

    private fun update(s: ConnectionState, remote: String?) {
        state = s
        listener.onStateChanged(s, remote)
    }
}
