package kittoku.tc.service

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getBooleanPrefValue
import kittoku.tc.preference.checkPreferences


internal class AcousticCouplerTileService : TileService() {
    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val listener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == OscPrefKey.ROOT_STATE.name) {
                updateTileState()
            }
        }
    }

    private val rootState: Boolean
        get() = getBooleanPrefValue(OscPrefKey.ROOT_STATE, prefs)

    private val isVpnPrepared: Boolean
        get() = VpnService.prepare(this) == null

    private fun invalidateTileState() {
        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()
    }

    private fun updateTileState() {
        qsTile.state = if (rootState) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }

        qsTile.updateTile()
    }

    private fun flipTileState() {
        qsTile.state = if (qsTile.state == Tile.STATE_ACTIVE) {
            Tile.STATE_INACTIVE
        } else {
            Tile.STATE_ACTIVE
        }

        qsTile.updateTile()
    }

    private fun initializeState() {
        if (isVpnPrepared) {
            updateTileState()
        } else {
            invalidateTileState()
        }
    }

    override fun onTileAdded() {
        requestListeningState(this, ComponentName(this, AcousticCouplerTileService::class.java))
    }

    override fun onStartListening() {
        initializeState()

        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onStopListening() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun startVpnService(action: String) {
        val intent = Intent(this, AcousticCouplerService::class.java).setAction(action)

        if (action == ACTION_VPN_CONNECT) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onClick() {
        if (!isVpnPrepared || checkPreferences(prefs) != null) return

        flipTileState()

        when (qsTile.state) {
            Tile.STATE_ACTIVE -> startVpnService(ACTION_VPN_CONNECT)
            Tile.STATE_INACTIVE -> startVpnService(ACTION_VPN_DISCONNECT)
        }
    }
}
