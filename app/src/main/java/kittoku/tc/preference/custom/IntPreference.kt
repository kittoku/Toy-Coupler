package kittoku.tc.preference.custom

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import kittoku.tc.preference.accessor.getIntPrefValue


internal abstract class IntPreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs), OscPreference {
    override fun updateView() {
        text = getIntPrefValue(oscPrefKey, sharedPreferences!!).toString()
    }

    override fun onAttached() {
        summaryProvider = SimpleSummaryProvider.getInstance()

        setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        initialize()
    }
}
