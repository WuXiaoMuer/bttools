package com.bttools.app.core

/** 链路类型。 */
enum class TransportType(val displayName: String) {
    SPP("蓝牙经典 SPP"),
    BLE("BLE 低功耗"),
    TCP_CLIENT("TCP 客户端"),
    TCP_SERVER("TCP 服务端"),
    UDP("UDP")
}

/** 连接状态。 */
enum class ConnectionState {
    IDLE,        // 空闲/未连接
    LISTENING,   // 监听中（服务端/可被连接）
    CONNECTING,  // 连接中
    CONNECTED,   // 已连接
    ERROR        // 出错
}

/** 数据方向。 */
enum class Direction { RX, TX, INFO }

/**
 * 传输层向上层（TerminalEngine）回调的监听接口。
 * 实现方需自行确保线程安全（TerminalEngine 会切回主线程更新 UI）。
 */
interface TransportListener {
    /** 状态变化；[remote] 为对端描述（设备名/IP）。 */
    fun onStateChanged(state: ConnectionState, remote: String?)
    /** 收到原始字节。 */
    fun onReceive(data: ByteArray)
    /** 信息/提示（连接失败、断开等）。 */
    fun onInfo(message: String)
}

/**
 * 统一传输抽象。SPP / BLE / TCP / UDP 各自实现。
 * 所有阻塞 IO 必须在后台线程执行。
 */
interface Transport {
    val type: TransportType

    /** 建立连接 / 开始监听。 */
    fun open()

    /** 发送原始字节。 */
    fun send(data: ByteArray)

    /** 关闭并释放资源。 */
    fun close()

    /** 当前是否已连接。 */
    fun isConnected(): Boolean
}
