package com.bttools.app.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HexUtilsTest {

    @Test
    fun bytesToHex_basic() {
        assertEquals("AB CD 01", HexUtils.bytesToHex(byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0x01)))
        assertEquals("", HexUtils.bytesToHex(ByteArray(0)))
        assertEquals("ABCD", HexUtils.bytesToHex(byteArrayOf(0xAB.toByte(), 0xCD.toByte()), separator = ""))
    }

    @Test
    fun hexStringToBytes_tolerant() {
        assertArrayEquals(byteArrayOf(0xAB.toByte(), 0xCD.toByte()), HexUtils.hexStringToBytes("AB CD"))
        assertArrayEquals(byteArrayOf(0xAB.toByte(), 0xCD.toByte()), HexUtils.hexStringToBytes("0xAB,0xCD"))
        assertArrayEquals(byteArrayOf(0xAB.toByte()), HexUtils.hexStringToBytes("ABC")) // 奇数位丢弃
        assertArrayEquals(ByteArray(0), HexUtils.hexStringToBytes("zz"))
    }

    @Test
    fun roundTrip() {
        val original = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())
        assertArrayEquals(original, HexUtils.hexStringToBytes(HexUtils.bytesToHex(original)))
    }

    @Test
    fun isValidHex() {
        assertTrue(HexUtils.isValidHex("AB CD"))
        assertTrue(HexUtils.isValidHex("0xAB"))
        assertFalse(HexUtils.isValidHex("A")) // 不足一个字节
        assertFalse(HexUtils.isValidHex("hello"))
    }

    @Test
    fun encodeForSend_textWithLineEnding() {
        assertArrayEquals(
            "AT\r\n".toByteArray(Charsets.UTF_8),
            HexUtils.encodeForSend("AT", TextEncoding.UTF8, LineEnding.CRLF)
        )
        assertArrayEquals(
            "AT".toByteArray(Charsets.UTF_8),
            HexUtils.encodeForSend("AT", TextEncoding.UTF8, LineEnding.NONE)
        )
    }

    @Test
    fun encodeForSend_hexIgnoresLineEnding() {
        assertArrayEquals(
            byteArrayOf(0xAB.toByte(), 0xCD.toByte()),
            HexUtils.encodeForSend("AB CD", TextEncoding.HEX, LineEnding.CRLF)
        )
    }

    @Test
    fun decodeForDisplay() {
        val bytes = "Hi".toByteArray()
        assertEquals("48 69", HexUtils.decodeForDisplay(bytes, hexMode = true, TextEncoding.UTF8))
        assertEquals("Hi", HexUtils.decodeForDisplay(bytes, hexMode = false, TextEncoding.UTF8))
    }
}
