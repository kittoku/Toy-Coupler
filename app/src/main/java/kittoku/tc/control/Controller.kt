package kittoku.tc.control

import kittoku.tc.ControlMessage
import kittoku.tc.Result
import kittoku.tc.SharedBridge
import kittoku.tc.Where
import kittoku.tc.debug.assertAlways
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getStringPrefValue
import kittoku.tc.rxtx.RXTXState
import kittoku.tc.rxtx.modem.Calibrator
import kittoku.tc.service.NOTIFICATION_ERROR_ID
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull


internal class Controller(private val bridge: SharedBridge) {
    private var calibrator: Calibrator? = null

    private var rxtxState: RXTXState? = null

    private var jobCalibrate: Job? = null
    private var jobTerminal: Job? = null

    private var jobMain: Job? = null

    private val mutex = Mutex()

    private fun attachHandler() {
        bridge.handler = CoroutineExceptionHandler { _, throwable ->
            kill {
                val header = "OSC: ERR_UNEXPECTED"
                bridge.service.logWriter?.report(header + "\n" + throwable.stackTraceToString())
                bridge.service.makeNotification(NOTIFICATION_ERROR_ID, header)
            }
        }
    }

    internal fun launchJobMain() {
        attachHandler()

        when (getStringPrefValue(OscPrefKey.HOME_MODE, bridge.prefs)) {
            "TERMINAL" -> launchJobTerminal()
            else -> launchJobCalibrate()
        }
    }

    private fun launchJobCalibrate() {
        jobCalibrate = bridge.service.scope.launch(bridge.handler) {
            calibrator = Calibrator(bridge).also {
                it.launchJobCalibrate()
            }

            expectProceeded(Where.CONTROL, null) // wait ERR_ message until disconnection
        }
    }

    private fun launchJobTerminal() {
        jobTerminal = bridge.service.scope.launch(bridge.handler) {
            bridge.attachIPTerminal()

            bridge.ipTerminal!!.initialize()
            if (!expectProceeded(Where.IP, null)) {
                return@launch
            }

            rxtxState = RXTXState(bridge).also {
                it.initiate()
            }


            expectProceeded(Where.CONTROL, null) // wait ERR_ message until disconnection
        }
    }

    private suspend fun expectProceeded(where: Where, timeout: Long?): Boolean {
        val received = if (timeout != null) {
            withTimeoutOrNull(timeout) {
                bridge.controlMailbox.receive()
            } ?: ControlMessage(where, Result.ERR_TIMEOUT)
        } else {
            bridge.controlMailbox.receive()
        }

        if (received.result == Result.PROCEEDED) {
            assertAlways(received.from == where)

            return true
        }

        kill {
            val message = "${received.from.name}: ${received.result.name}"
            bridge.service.logWriter?.report(message)
            bridge.service.makeNotification(NOTIFICATION_ERROR_ID, message)
        }

        return false
    }

    internal fun disconnect() { // use if the user want to normally disconnect
        kill { }
    }

    internal fun kill(cleanup: (suspend () -> Unit)?) {
        if (!mutex.tryLock()) return

        bridge.service.scope.launch {
            jobMain?.cancel()
            cancelClients()

            cleanup?.invoke()

            closeTerminals()

            bridge.service.close()
        }
    }

    private fun cancelClients() {
        calibrator?.cancel()
        rxtxState?.release()
    }

    private fun closeTerminals() {
        bridge.ipTerminal?.close()
    }
}
