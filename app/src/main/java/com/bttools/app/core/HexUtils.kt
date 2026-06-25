package com.bttools.app.core

import java.nio.charset.Charset

/**
 * 编码方式：决定发送时文本如何转字节、接收时字节如何转文本。
 */
enum class TextEncoding(val displayName: String) {
    UTF8("UTF-8"),
    ASCII("ASCII"),
    GBK("GBK"),
    HEX("HEX");

    fun charset(): Charset = when (this) {
        UTF8 -> Charsets.UTF_8
        ASCII -> Charsets.US_ASCII
        GBK -> try { Charset.forName("GBK") } catch (e: Exception) { Charsets.UTF_8 }
        HEX -> Charsets.ISO_8859_1
    }
}

/**
 * 行尾（追加到发送内容末尾）。
 */
enum class LineEnding(val displayName: String, val bytes: ByteArray) {
    NONE("无", ByteArray(0)),
    CR("CR (\\r)", byteArrayOf('\r'.code.toByte())),
    LF("LF (\\n)", byteArrayOf('\n'.code.toByte())),
    CRLF("CRLF (\\r\\n)", byteArrayOf('\r'.code.toByte(), '\n'.code.toByte()))
}

/**
 * HEX 与字节、文本与字节的转换工具。容错处理空白与非法字符。
 */
object HexUtils {

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    /** 字节数组 → "AB CD EF"（大写，空格分隔）。 */
    fun bytesToHex(bytes: ByteArray, separator: String = " "): String {
        if (bytes.isEmpty()) return ""
        val sb = StringBuilder(bytes.size * 3)
        for ((index, b) in bytes.withIndex()) {
            if (index > 0 && separator.isNotEmpty()) sb.append(separator)
            val v = b.toInt() and 0xFF
            sb.append(HEX_CHARS[v ushr 4])
            sb.append(HEX_CHARS[v and 0x0F])
        }
        return sb.toString()
    }

    /**
     * "AB CD" / "abcd" / "0xAB,0xCD" → 字节数组。
     * 忽略所有非十六进制字符；奇数个有效字符时丢弃最后一个半字节。
     */
    fun hexStringToBytes(hex: String): ByteArray {
        // 先去掉 "0x"/"0X" 前缀，避免前缀里的 '0' 被当作有效半字节
        val pre = hex.replace("0x", " ", ignoreCase = true)
        val clean = StringBuilder(pre.length)
        for (c in pre) {
            if ((c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')) {
                clean.append(c)
            }
        }
        val length = clean.length / 2
        val result = ByteArray(length)
        for (i in 0 until length) {
            val hi = Character.digit(clean[i * 2], 16)
            val lo = Character.digit(clean[i * 2 + 1], 16)
            result[i] = ((hi shl 4) or lo).toByte()
        }
        return result
    }

    /** 判断字符串是否为合法（至少包含一个字节）的 HEX。 */
    fun isValidHex(hex: String): Boolean {
        var count = 0
        for (c in hex) {
            if ((c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')) count++
            else if (!c.isWhitespace() && c != ',' && c != 'x' && c != 'X') return false
        }
        return count >= 2
    }

    /**
     * 把要发送的字符串按编码与行尾组装成字节。
     * HEX 编码时忽略行尾（按原始字节发送）。
     */
    fun encodeForSend(text: String, encoding: TextEncoding, lineEnding: LineEnding): ByteArray {
        return if (encoding == TextEncoding.HEX) {
            hexStringToBytes(text)
        } else {
            val body = text.toByteArray(encoding.charset())
            if (lineEnding.bytes.isEmpty()) body else body + lineEnding.bytes
        }
    }

    /** 把接收到的字节按显示模式渲染为字符串。 */
    fun decodeForDisplay(bytes: ByteArray, hexMode: Boolean, encoding: TextEncoding): String {
        return if (hexMode) {
            bytesToHex(bytes)
        } else {
            val cs = if (encoding == TextEncoding.HEX) Charsets.UTF_8 else encoding.charset()
            String(bytes, cs)
        }
    }
}
