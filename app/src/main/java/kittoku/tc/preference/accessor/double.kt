package kittoku.tc.preference.accessor

import android.content.SharedPreferences
import kittoku.tc.preference.DEFAULT_DOUBLE_MAP
import kittoku.tc.preference.OscPrefKey


internal fun getDoublePrefValue(key: OscPrefKey, prefs: SharedPreferences): Double {
    return prefs.getString(key.name, null)?.toDoubleOrNull() ?: DEFAULT_DOUBLE_MAP[key]!!
}

internal fun setDoublePrefValue(value: Double, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putString(key.name, value.toString())
        it.apply()
    }
}
