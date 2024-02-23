package kittoku.tc.rxtx.modem

import java.nio.ByteBuffer


private const val MASK = 0b1111_1111

private fun sumBytes(value: Int): Int {
    var shifted = value
    var sum = 0

    repeat(Int.SIZE_BYTES) {
        sum += shifted and MASK
        shifted = shifted.ushr(Byte.SIZE_BITS)
    }
    
    return sum and MASK
}

internal fun calcChecksum(packet: ByteBuffer): Int {
    var checksum = 0

    checksum += sumBytes(PREAMBLE)
    checksum += sumBytes(packet.remaining())

    (packet.position() until packet.limit()).forEach {
        checksum += packet.get(it)
    }

    return checksum and MASK
}
