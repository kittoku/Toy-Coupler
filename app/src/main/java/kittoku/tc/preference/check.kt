package kittoku.tc.preference

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import kittoku.tc.preference.accessor.getBooleanPrefValue
import kittoku.tc.preference.accessor.getURIPrefValue


internal fun toastInvalidSetting(message: String, context: Context) {
    Toast.makeText(context, "INVALID SETTING: $message", Toast.LENGTH_LONG).show()
}

internal fun checkPreferences(prefs: SharedPreferences): String? {
    val doSaveLog = getBooleanPrefValue(OscPrefKey.LOG_DO_SAVE_LOG, prefs)
    val logDir = getURIPrefValue(OscPrefKey.LOG_DIR, prefs)
    if (doSaveLog && logDir == null) return "No log directory was selected"


    return null
}
