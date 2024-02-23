package kittoku.tc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DropDownPreference
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getStringPrefValue


internal abstract class ModifiedDropDownPreference(context: Context, attrs: AttributeSet) : DropDownPreference(context, attrs), OscPreference {
    protected abstract val values: Array<String>
    protected open val names: Array<String>? = null
    override fun updateView() {
        value = getStringPrefValue(oscPrefKey, sharedPreferences!!)
    }

    override fun onAttached() {
        entryValues = values
        entries = names ?: values
        summaryProvider = SimpleSummaryProvider.getInstance()

        initialize()
    }
}

internal class HomeModePreference(context: Context, attrs: AttributeSet) : ModifiedDropDownPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.HOME_MODE
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Mode"
    override val values = arrayOf("TERMINAL", "CALIBRATOR")
}
