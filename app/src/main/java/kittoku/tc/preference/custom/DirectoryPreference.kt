package kittoku.tc.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getURIPrefValue


internal abstract class DirectoryPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs), OscPreference {
    override fun updateView() {
        summary = getURIPrefValue(oscPrefKey, sharedPreferences!!)?.path ?: "[No Directory Selected]"
    }

    override fun onAttached() {
        initialize()
    }
}

internal class LogDirPreference(context: Context, attrs: AttributeSet) : DirectoryPreference(context, attrs) {
    override val oscPrefKey = OscPrefKey.LOG_DIR
    override val preferenceTitle = "Select Log Directory"
    override val parentKey = OscPrefKey.LOG_DO_SAVE_LOG
}
