package com.bttools.app.core

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/** 一条收发记录。 */
data class LogEntry(
    val timestamp: Long,
    val direction: Direction,
    val data: ByteArray,
    val info: String? = null
)

/**
 * 终端状态中枢：连接所有 UI 与具体 Transport。
 * 由 MainActivity 创建并 remember 注入 Compose。线程安全：所有状态更新切回主线程。
 */
class TerminalEngine : TransportListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var transport: Transport? = null
    private var loopJob: Job? = null

    // ---- Compose 可观察状态 ----
    val logs = mutableStateListOf<LogEntry>()
    var state by mutableStateOf(ConnectionState.IDLE)
        private set
    var remote by mutableStateOf<String?>(null)
        private set
    var rxBytes by mutableStateOf(0L)
        private set
    var txBytes by mutableStateOf(0L)
        private set
    var currentType by mutableStateOf<TransportType?>(null)
        private set

    private val maxLogs = 5000

    val isConnected: Boolean get() = state == ConnectionState.CONNECTED

    // ---- 连接管理 ----

    /** 使用给定 transport 打开连接（先关闭旧的）。 */
    fun connect(newTransport: Transport) {
        close()
        transport = newTransport
        currentType = newTransport.type
        newTransport.open()
    }

    /** 关闭当前连接。 */
    fun close() {
        stopLoop()
        transport?.let {
            try { it.close() } catch (e: Exception) { e.printStackTrace() }
        }
        transport = null
    }

    // ---- 发送 ----

    /** 发送已组装好的原始字节。 */
    fun sendRaw(data: ByteArray) {
        val t = transport
        if (t == null || !t.isConnected()) {
            postInfo("未连接，无法发送")
            return
        }
        try {
            t.send(data)
            onSent(data)
        } catch (e: Exception) {
            postInfo("发送失败: ${e.message}")
        }
    }

    /** 按编码与行尾组装后发送文本（encoding=HEX 时按十六进制解析）。 */
    fun sendText(text: String, encoding: TextEncoding, lineEnding: LineEnding) {
        if (text.isEmpty()) return
        val bytes = HexUtils.encodeForSend(text, encoding, lineEnding)
        if (bytes.isEmpty()) {
            postInfo("待发送内容为空或 HEX 非法")
            return
        }
        sendRaw(bytes)
    }

    /** 启动周期发送；intervalMs 最小 10ms。 */
    fun startLoop(text: String, encoding: TextEncoding, lineEnding: LineEnding, intervalMs: Long) {
        stopLoop()
        val period = intervalMs.coerceAtLeast(10L)
        loopJob = scope.launch {
            while (true) {
                sendText(text, encoding, lineEnding)
                delay(period)
            }
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    val isLooping: Boolean get() = loopJob?.isActive == true

    // ---- 日志 ----

    fun clearLogs() {
        logs.clear()
        rxBytes = 0
        txBytes = 0
    }

    private fun addLog(entry: LogEntry) {
        logs.add(entry)
        if (logs.size > maxLogs) {
            // 批量删除头部，避免频繁单条移除
            repeat(logs.size - maxLogs) { if (logs.isNotEmpty()) logs.removeAt(0) }
        }
    }

    private fun postInfo(message: String) {
        mainHandler.post {
            addLog(LogEntry(System.currentTimeMillis(), Direction.INFO, ByteArray(0), message))
        }
    }

    fun shutdown() {
        close()
        scope.cancel()
    }

    // ---- TransportListener（可能在后台线程被调用）----

    override fun onStateChanged(state: ConnectionState, remote: String?) {
        mainHandler.post {
            this.state = state
            if (remote != null) this.remote = remote
            if (state == ConnectionState.IDLE || state == ConnectionState.ERROR) {
                if (state == ConnectionState.IDLE) this.remote = null
            }
        }
    }

    override fun onReceive(data: ByteArray) {
        if (data.isEmpty()) return
        val copy = data.copyOf()
        mainHandler.post {
            rxBytes += copy.size
            addLog(LogEntry(System.currentTimeMillis(), Direction.RX, copy))
        }
    }

    fun onSent(data: ByteArray) {
        val copy = data.copyOf()
        mainHandler.post {
            txBytes += copy.size
            addLog(LogEntry(System.currentTimeMillis(), Direction.TX, copy))
        }
    }

    override fun onInfo(message: String) {
        postInfo(message)
    }
}
