package kittoku.tc.preference

import android.net.Uri


internal enum class OscPrefKey {
    ROOT_STATE,
    HOME_MODE,
    HOME_ADDRESS,
    HOME_THRESHOLD,
    HOME_CONNECTOR,
    HOME_STATUS,
    ROUTE_DO_ADD_DEFAULT_ROUTE,
    ROUTE_DO_ROUTE_PRIVATE_ADDRESSES,
    ROUTE_DO_ADD_CUSTOM_ROUTES,
    ROUTE_CUSTOM_ROUTES,
    ROUTE_DO_ENABLE_APP_BASED_RULE,
    ROUTE_ALLOWED_APPS,
    LOG_DO_SAVE_LOG,
    LOG_DIR,
}


internal val DEFAULT_BOOLEAN_MAP = mapOf(
    OscPrefKey.ROOT_STATE to false,
    OscPrefKey.HOME_CONNECTOR to false,
    OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE to true,
    OscPrefKey.ROUTE_DO_ROUTE_PRIVATE_ADDRESSES to false,
    OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES to false,
    OscPrefKey.ROUTE_DO_ENABLE_APP_BASED_RULE to false,
    OscPrefKey.LOG_DO_SAVE_LOG to false
)

internal val DEFAULT_INT_MAP = mapOf(
    OscPrefKey.HOME_THRESHOLD to 50,
)

internal val DEFAULT_DOUBLE_MAP = mapOf(
    OscPrefKey.HOME_THRESHOLD to 50.0,
)

private const val EMPTY_TEXT = ""

internal val DEFAULT_STRING_MAP = mapOf(
    OscPrefKey.HOME_MODE to "TERMINAL",
    OscPrefKey.HOME_ADDRESS to "169.254.0.1",
    OscPrefKey.HOME_STATUS to EMPTY_TEXT,
    OscPrefKey.ROUTE_CUSTOM_ROUTES to EMPTY_TEXT,
)

private val EMPTY_SET = setOf<String>()

internal val DEFAULT_SET_MAP = mapOf(
    OscPrefKey.ROUTE_ALLOWED_APPS to EMPTY_SET,
)

internal val DEFAULT_URI_MAP = mapOf<OscPrefKey, Uri?>(
    OscPrefKey.LOG_DIR to null,
)


internal const val TEMP_KEY_HEADER = "_"
