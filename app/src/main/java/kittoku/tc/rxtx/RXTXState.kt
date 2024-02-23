package kittoku.tc.rxtx

import kittoku.tc.SharedBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random


private const val MIN_INTERVAL = 10L
private const val MAX_INTERVAL = 80L

internal class RXTXState(private val bridge: SharedBridge) {
    private val receiveManager = ReceiveManager(bridge)
    private val transmitManager = TransmitManager(bridge)

    private var job: Job? = null
    private var lastTransmitted = Long.MIN_VALUE

    private suspend fun continueReceive(duration: Long) {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < duration) {
            if (receiveManager.tryReceive()) {
                delay(MIN_INTERVAL) // for the peer switching its state
                break
            }
        }
    }

    private fun calcDuration(): Long {
        val currentTime = System.currentTimeMillis()

        val transmitInterval = currentTime - lastTransmitted
        if (transmitInterval < MAX_INTERVAL) {
            return MAX_INTERVAL
        }

        return Random.nextLong(MIN_INTERVAL, MAX_INTERVAL)
    }

    internal fun initiate() {
        transmitManager.launchJobLoad()

        job = bridge.service.scope.launch(bridge.handler) {
            receiveManager.start()

            while (isActive) {
                continueReceive(calcDuration())

                transmitManager.packetChannel.tryReceive().getOrNull()?.also {
                    transmitManager.transmit(it)
                    lastTransmitted = System.currentTimeMillis()

                    receiveManager.flush()
                }
            }
        }
    }

    internal fun release() {
        job?.cancel()

        receiveManager.cancel()
        transmitManager.cancel()
    }
}
