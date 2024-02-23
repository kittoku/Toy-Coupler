package kittoku.tc.preference.accessor

import android.content.SharedPreferences
import kittoku.tc.preference.DEFAULT_SET_MAP
import kittoku.tc.preference.OscPrefKey


internal fun getSetPrefValue(key: OscPrefKey, prefs: SharedPreferences): Set<String> {
    return prefs.getStringSet(key.name, DEFAULT_SET_MAP[key]!!)!!
}

internal fun setSetPrefValue(value: Set<String>, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putStringSet(key.name, value)
        it.apply()
    }
}
