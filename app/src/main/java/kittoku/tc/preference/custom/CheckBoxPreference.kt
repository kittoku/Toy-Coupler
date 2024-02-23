package kittoku.tc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getBooleanPrefValue


internal abstract class ModifiedCheckBoxPreference(context: Context, attrs: AttributeSet) : CheckBoxPreference(context, attrs), OscPreference {
    override fun updateView() {
        isChecked = getBooleanPrefValue(oscPrefKey, sharedPreferences!!)
    }

    override fun onAttached() {
        initialize()
    }
}

internal class RouteDoAddDefaultRoutePreference(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Add Default Route"
}

internal class RouteDoRoutePrivateAddresses(context: Context, attrs: AttributeSet) : ModifiedCheckBoxPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_DO_ROUTE_PRIVATE_ADDRESSES
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Route Private/Unique-Local Addresses"
}
