package kittoku.tc.control

import kittoku.tc.SharedBridge
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.setStringPrefValue
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal class SignalObserver(private val bridge: SharedBridge) {
    private var currentLevel = Double.NEGATIVE_INFINITY
    private var lastDetected = "NEVER DETECTED"
    private var lastReceived = "NEVER RECEIVED"

    init {
        wipeStatus()
    }

    internal fun updateLevel(level: Double) {
        currentLevel = level
        updateSummary()
    }

    internal fun updateLastDetected() {
        lastDetected = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        updateSummary()
    }

    internal fun updateLastReceived(packet: ByteBuffer, isChecksumMatched: Boolean) {
        val timeStr = "TIME: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}"
        val lengthStr = "LENGTH: ${packet.remaining()} Byte"
        val checksumStr = if (isChecksumMatched) {
            "CHECKSUM: MATCHED"
        } else {
            "CHECKSUM: NOT MATCHED"
        }

        lastReceived = "$timeStr\n$lengthStr\n$checksumStr\n"
        updateSummary()
    }

    private fun updateSummary() {
        val summary = mutableListOf<String>()

        summary.add("[Level of 1000 Hz]")
        summary.add("%3.1f dB SPL".format(currentLevel))
        summary.add("")

        summary.add("[Last Signal Detected]")
        summary.add(lastDetected)
        summary.add("")

        summary.add("[Last Packet Received]")
        summary.add(lastReceived)
        summary.add("")

        summary.reduce { acc, s ->
            acc + "\n" + s
        }.also {
            setStringPrefValue(it, OscPrefKey.HOME_STATUS, bridge.prefs)
        }
    }

    private fun wipeStatus() {
        setStringPrefValue("", OscPrefKey.HOME_STATUS, bridge.prefs)
    }

    internal fun close() {
        wipeStatus()
    }
}
