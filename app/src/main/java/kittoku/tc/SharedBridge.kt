package kittoku.tc

import androidx.preference.PreferenceManager
import kittoku.tc.preference.AppString
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getBooleanPrefValue
import kittoku.tc.preference.accessor.getDoublePrefValue
import kittoku.tc.preference.accessor.getStringPrefValue
import kittoku.tc.preference.getValidAllowedAppInfos
import kittoku.tc.service.AcousticCouplerService
import kittoku.tc.terminal.IPTerminal
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import java.net.Inet4Address


internal enum class Where {
    CONTROL,
    IP,
    ROUTE,
}

internal data class ControlMessage(
    val from: Where,
    val result: Result
)
internal enum class Result {
    PROCEEDED,

    // common errors
    ERR_TIMEOUT,
    ERR_COUNT_EXHAUSTED,
    ERR_UNKNOWN_TYPE, // the data cannot be parsed
    ERR_UNEXPECTED_MESSAGE, // the data can be parsed, but it's received in the wrong time
    ERR_PARSING_FAILED,
    ERR_VERIFICATION_FAILED,

    // for IP
    ERR_INVALID_ADDRESS,

    // for INCOMING
    ERR_INVALID_PACKET_SIZE,
}

internal class SharedBridge(internal val service:
                            AcousticCouplerService) {
    internal val prefs = PreferenceManager.getDefaultSharedPreferences(service)
    internal val builder = service.Builder()
    internal lateinit var handler: CoroutineExceptionHandler

    internal val controlMailbox = Channel<ControlMessage>(Channel.BUFFERED)

    internal var ipTerminal: IPTerminal? = null

    internal val assignedAddress = Inet4Address.getByName(
        getStringPrefValue(OscPrefKey.HOME_ADDRESS, prefs)
    )

    internal val thresholdLevel: Double
        get() = getDoublePrefValue(OscPrefKey.HOME_THRESHOLD, prefs)

    internal val allowedApps: List<AppString> = mutableListOf<AppString>().also {
        if (getBooleanPrefValue(OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE, prefs)) {
            getValidAllowedAppInfos(prefs, service.packageManager).forEach { info ->
                it.add(
                    AppString(
                        info.packageName,
                        service.packageManager.getApplicationLabel(info).toString()
                    )
                )
            }
        }
    }

    internal fun attachIPTerminal() {
        ipTerminal = IPTerminal(this)
    }
}
