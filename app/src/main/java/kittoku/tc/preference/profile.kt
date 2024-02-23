package kittoku.tc.preference

import android.content.SharedPreferences
import kittoku.tc.extension.toUri
import kittoku.tc.preference.accessor.setBooleanPrefValue
import kittoku.tc.preference.accessor.setIntPrefValue
import kittoku.tc.preference.accessor.setSetPrefValue
import kittoku.tc.preference.accessor.setStringPrefValue
import kittoku.tc.preference.accessor.setURIPrefValue


private const val RECORD_SEPARATOR = 0x1E.toChar().toString()
private const val UNIT_SEPARATOR = 0x1F.toChar().toString()

private val EXCLUDED_BOOLEAN_PREFERENCES = arrayOf(
    OscPrefKey.ROOT_STATE,
    OscPrefKey.HOME_CONNECTOR,
    OscPrefKey.HOME_STATUS,
)

private val EXCLUDED_STRING_PREFERENCES = arrayOf(
    OscPrefKey.HOME_STATUS,
)

internal fun importProfile(profile: String?, prefs: SharedPreferences) {
    val profileMap = profile?.split(RECORD_SEPARATOR)?.filter { it.isNotEmpty() }?.associate {
        val index = it.indexOf(UNIT_SEPARATOR)
        val key = it.substring(0, index)
        val value = it.substring(index + 1)

        key to value
    } ?: mapOf()

    DEFAULT_BOOLEAN_MAP.keys.filter { it !in EXCLUDED_BOOLEAN_PREFERENCES }.forEach {
        val value = profileMap[it.name]?.toBooleanStrict() ?: DEFAULT_BOOLEAN_MAP.getValue(it)
        setBooleanPrefValue(value, it, prefs)
    }

    DEFAULT_INT_MAP.keys.forEach {
        val value = profileMap[it.name]?.toInt() ?: DEFAULT_INT_MAP.getValue(it)
        setIntPrefValue(value, it, prefs)
    }

    DEFAULT_STRING_MAP.keys.filter { it !in EXCLUDED_STRING_PREFERENCES }.forEach {
        val value = profileMap[it.name] ?: DEFAULT_STRING_MAP.getValue(it)
        setStringPrefValue(value, it, prefs)
    }

    DEFAULT_SET_MAP.keys.forEach { key ->
        val value = profileMap[key.name]?.split(UNIT_SEPARATOR)?.filter { it.isNotEmpty() }?.toSet() ?: DEFAULT_SET_MAP.getValue(key)

        setSetPrefValue(value, key, prefs)
    }

    DEFAULT_URI_MAP.keys.forEach {
        val value = profileMap[it.name]?.toUri() ?: DEFAULT_URI_MAP.getValue(it)
        setURIPrefValue(value, it, prefs)
    }
}
