package com.bttools.app.core

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    /** 返回本机第一个非回环 IPv4 地址，用于 TCP/UDP 服务端提示。 */
    fun getLocalIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
