package kittoku.tc.rxtx

import kittoku.tc.SharedBridge
import kittoku.tc.rxtx.modem.Demodulator
import java.nio.ByteBuffer


internal class ReceiveManager(private val bridge: SharedBridge) {
    private val demodulator = Demodulator(bridge)

    internal fun start() {
        demodulator.startRecord()
    }

    internal fun flush() {
        demodulator.discardFrames()
    }

    internal fun tryReceive(): Boolean {
        val packet = demodulator.retrievePacket() ?: return false

        tryWrite(packet)

        return true
    }

    private fun tryWrite(packet: ByteBuffer) {
        if (!verifyIPv4Packet(packet, dstIp = bridge.assignedAddress.address)) {
            return
        }

        bridge.ipTerminal!!.writePacket(0, packet.limit(), packet)
    }

    internal fun cancel() {
        demodulator.release()
    }
}
