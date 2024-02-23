package kittoku.tc.rxtx

import kittoku.tc.SharedBridge
import kittoku.tc.rxtx.modem.Modulator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


internal class TransmitManager(private val bridge: SharedBridge) {
    private var jobLoad: Job? = null

    internal val packetChannel = Channel<ByteBuffer>(10, BufferOverflow.DROP_OLDEST)

    private val modulator = Modulator()

    internal suspend fun transmit(buffer: ByteBuffer) {
        modulator.transmitTones(buffer)
    }

    internal fun launchJobLoad() {
        jobLoad = bridge.service.scope.launch(bridge.handler) {
            while (isActive) {
                val packet = bridge.ipTerminal!!.readPacket()
                if (verifyIPv4Packet(packet, srcIp = bridge.assignedAddress.address)) {
                    packetChannel.trySend(packet)
                }
            }
        }
    }

    internal fun cancel() {
        jobLoad?.cancel()
        packetChannel.close()
    }
}
