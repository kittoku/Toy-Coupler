package kittoku.tc.preference.custom

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getIntPrefValue


internal abstract class DoublePreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs), OscPreference {
    override fun updateView() {
        text = getIntPrefValue(oscPrefKey, sharedPreferences!!).toString()
    }

    override fun onAttached() {
        summaryProvider = SimpleSummaryProvider.getInstance()

        setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        initialize()
    }
}

internal class HomeThresholdPreference(context: Context, attrs: AttributeSet) : DoublePreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.HOME_THRESHOLD
    override val parentKey: OscPrefKey? = null
    override val preferenceTitle = "Threshold [dB SPL]"
}
