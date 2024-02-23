package kittoku.tc.rxtx

import kittoku.tc.extension.toIntAsUByte
import java.nio.ByteBuffer


internal fun verifyIPv4Packet(packet: ByteBuffer, srcIp: ByteArray? = null, dstIp: ByteArray? = null): Boolean {
    val version = packet.get(0).toIntAsUByte()
    if (version and IP_VERSION_MASK != IPv4_VERSION_HEADER) {
        return false
    }


    if (srcIp != null) {
        val srcMatched = packet.array().sliceArray(12 until 16).contentEquals(srcIp)

        if (!srcMatched) {
            return false
        }
    }


    if (dstIp != null) {
        val dstMatched = packet.array().sliceArray(16 until 20).contentEquals(dstIp)

        if (!dstMatched) {
            return false
        }
    }


    return true
}
