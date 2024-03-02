package kittoku.tc.preference.custom

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getStringPrefValue


internal abstract class StringPreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs), OscPreference {
    protected open val provider = SummaryProvider<Preference> {
        getStringPrefValue(oscPrefKey, it.sharedPreferences!!).ifEmpty { "[No Value Entered]" }
    }

    protected open fun initEditText(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_TEXT
    }

    override fun updateView() {
        text = getStringPrefValue(oscPrefKey, sharedPreferences!!)
    }

    override fun onAttached() {
        setOnBindEditTextListener { initEditText(it) }

        summaryProvider = provider

        initialize()
    }
}

internal class HomeAssignedIPAddressPreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.HOME_ADDRESS
    override val parentKey = null
    override val preferenceTitle = "IPv4 Address to be assigned"

    override fun initEditText(editText: EditText) {
        super.initEditText(editText)

        editText.hint = "169.254.0.1"
    }
}


internal class RouteCustomRoutesPreference(context: Context, attrs: AttributeSet) : StringPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.ROUTE_CUSTOM_ROUTES
    override val parentKey = OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES
    override val preferenceTitle = "Edit Custom Routes"

    override fun updateView() {
        // to avoid the issue reported in https://issuetracker.google.com/issues/37032278
        text = getStringPrefValue(oscPrefKey, sharedPreferences!!).trim()
    }

    override val provider = SummaryProvider<Preference> {
        val currentValue = getStringPrefValue(oscPrefKey, it.sharedPreferences!!)

        if (currentValue.isEmpty()) {
            "[No Value Entered]"
        } else {
            "[Custom Routes Entered]"
        }
    }

    override fun initEditText(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        editText.hint = "192.168.1.0/24\n2001:db8::/32"
    }
}
