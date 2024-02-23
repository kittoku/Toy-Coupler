package kittoku.tc.rxtx.modem

import kittoku.tc.SharedBridge
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.setStringPrefValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


internal class Calibrator(private val bridge: SharedBridge) {
    private val modulator = Modulator()
    private val demodulator = Demodulator(bridge)

    private var jobModulate: Job? = null
    private var jobDemodulate: Job? = null

    internal fun launchJobCalibrate() {
        jobModulate = bridge.service.scope.launch(bridge.handler) {
            val pseudoPacket = ByteBuffer.wrap("KNOCK-KNOCK".toByteArray(Charsets.US_ASCII))

            while (isActive) {
                modulator.transmitTones(pseudoPacket)
                pseudoPacket.flip()
                delay(50L)
            }
        }

        jobDemodulate = bridge.service.scope.launch(bridge.handler) {
            demodulator.startRecord()

            while (isActive) {
                demodulator.retrievePacket()
            }
        }
    }

    internal fun cancel() {
        jobModulate?.cancel()
        jobDemodulate?.cancel()

        modulator.release()
        demodulator.release()

        setStringPrefValue("", OscPrefKey.HOME_STATUS, bridge.prefs)
    }
}
