package com.bttools.app.core.transports

import com.bttools.app.core.ConnectionState
import com.bttools.app.core.Transport
import com.bttools.app.core.TransportListener
import com.bttools.app.core.TransportType
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * TCP 链路。支持客户端（主动连接 host:port）与服务端（监听 port，接受首个客户端）。
 * 所有阻塞 IO 在独立线程执行。
 */
class TcpTransport(
    override val type: TransportType, // TCP_CLIENT 或 TCP_SERVER
    private val host: String,
    private val port: Int,
    private val listener: TransportListener
) : Transport {

    @Volatile private var state = ConnectionState.IDLE
    @Volatile private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var output: OutputStream? = null
    private var worker: Thread? = null
    private val sendExecutor = Executors.newSingleThreadExecutor()

    override fun isConnected(): Boolean = state == ConnectionState.CONNECTED

    override fun open() {
        worker = Thread {
            if (type == TransportType.TCP_SERVER) runServer() else runClient()
        }.also { it.start() }
    }

    private fun runClient() {
        update(ConnectionState.CONNECTING, "$host:$port")
        try {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), 8000)
            bind(s)
            update(ConnectionState.CONNECTED, "$host:$port")
            readLoop(s)
        } catch (e: Exception) {
            update(ConnectionState.ERROR, null)
            listener.onInfo("TCP 连接失败: ${e.message}")
        }
    }

    private fun runServer() {
        update(ConnectionState.LISTENING, "0.0.0.0:$port")
        try {
            val ss = ServerSocket(port)
            serverSocket = ss
            listener.onInfo("TCP 服务端监听端口 $port")
            val s = ss.accept()
            bind(s)
            update(ConnectionState.CONNECTED, s.remoteSocketAddress?.toString())
            readLoop(s)
        } catch (e: Exception) {
            if (state != ConnectionState.IDLE) {
                update(ConnectionState.ERROR, null)
                listener.onInfo("TCP 服务端错误: ${e.message}")
            }
        }
    }

    private fun bind(s: Socket) {
        socket = s
        output = s.getOutputStream()
    }

    private fun readLoop(s: Socket) {
        val input: InputStream = s.getInputStream()
        val buffer = ByteArray(4096)
        while (!s.isClosed) {
            val n = try { input.read(buffer) } catch (e: Exception) { -1 }
            if (n < 0) break
            if (n > 0) listener.onReceive(buffer.copyOf(n))
        }
        update(ConnectionState.IDLE, null)
        listener.onInfo("TCP 连接已断开")
    }

    override fun send(data: ByteArray) {
        val out = output ?: return
        sendExecutor.execute {
            try {
                out.write(data)
                out.flush()
            } catch (e: Exception) {
                listener.onInfo("TCP 发送失败: ${e.message}")
            }
        }
    }

    override fun close() {
        state = ConnectionState.IDLE
        try { socket?.close() } catch (e: Exception) { }
        try { serverSocket?.close() } catch (e: Exception) { }
        try { sendExecutor.shutdownNow() } catch (e: Exception) { }
        socket = null
        serverSocket = null
        output = null
        listener.onStateChanged(ConnectionState.IDLE, null)
    }

    private fun update(s: ConnectionState, remote: String?) {
        state = s
        listener.onStateChanged(s, remote)
    }
}
