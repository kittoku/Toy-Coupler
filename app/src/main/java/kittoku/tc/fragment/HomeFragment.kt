package kittoku.tc.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kittoku.tc.R
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.checkPreferences
import kittoku.tc.preference.custom.HomeConnectorPreference
import kittoku.tc.preference.toastInvalidSetting
import kittoku.tc.service.ACTION_VPN_CONNECT
import kittoku.tc.service.ACTION_VPN_DISCONNECT
import kittoku.tc.service.AcousticCouplerService


class HomeFragment : PreferenceFragmentCompat() {
    private val preparationLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(ACTION_VPN_CONNECT)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            activity?.finish()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home, rootKey)

        requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO).also {
            if (it != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attachConnectorListener()
    }

    private fun startVpnService(action: String) {
        val intent = Intent(requireContext(), AcousticCouplerService::class.java).setAction(action)

        if (action == ACTION_VPN_CONNECT) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun attachConnectorListener() {
        findPreference<HomeConnectorPreference>(OscPrefKey.HOME_CONNECTOR.name)!!.also {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newState ->
                if (newState == true) {
                    checkPreferences(preferenceManager.sharedPreferences!!)?.also { message ->
                        toastInvalidSetting(message, requireContext())
                        return@OnPreferenceChangeListener false
                    }

                    VpnService.prepare(requireContext())?.also { intent ->
                        preparationLauncher.launch(intent)
                    } ?: startVpnService(ACTION_VPN_CONNECT)
                } else {
                    startVpnService(ACTION_VPN_DISCONNECT)
                }

                true
            }
        }
    }
}
