package kittoku.tc.fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kittoku.tc.R
import kittoku.tc.activity.BLANK_ACTIVITY_TYPE_APPS
import kittoku.tc.activity.BlankActivity
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.setURIPrefValue
import kittoku.tc.preference.custom.DirectoryPreference


internal class SettingFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences

    private lateinit var logDirPref: DirectoryPreference

    private val logDirLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) result.data?.data?.also {
            requireContext().contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } else null

        setURIPrefValue(uri, OscPrefKey.LOG_DIR, prefs)

        logDirPref.updateView()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        prefs = preferenceManager.sharedPreferences!!

        logDirPref = findPreference(OscPrefKey.LOG_DIR.name)!!

        setLogDirListener()
        setAllowedAppsListener()
    }

    private fun setLogDirListener() {
        logDirPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).also { intent ->
                intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                logDirLauncher.launch(intent)
            }

            true
        }
    }

    private fun setAllowedAppsListener() {
        findPreference<Preference>(OscPrefKey.ROUTE_ALLOWED_APPS.name)!!.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(requireContext(), BlankActivity::class.java).putExtra(
                    "type",
                    BLANK_ACTIVITY_TYPE_APPS
                ))

                true
            }
        }
    }
}
