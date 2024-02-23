package kittoku.tc.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.VpnService
import android.service.quicksettings.TileService
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import kittoku.tc.R
import kittoku.tc.SharedBridge
import kittoku.tc.control.Controller
import kittoku.tc.control.LogWriter
import kittoku.tc.preference.OscPrefKey
import kittoku.tc.preference.accessor.getBooleanPrefValue
import kittoku.tc.preference.accessor.getURIPrefValue
import kittoku.tc.preference.accessor.setBooleanPrefValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal const val ACTION_VPN_CONNECT = "kittoku.osc.connect"
internal const val ACTION_VPN_DISCONNECT = "kittoku.osc.disconnect"

internal const val NOTIFICATION_CHANNEL_NAME = "kittoku.osc.notification.channel"
internal const val NOTIFICATION_ERROR_ID = 1
internal const val NOTIFICATION_DISCONNECT_ID = 2

internal class AcousticCouplerService : VpnService() {
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private lateinit var notificationManager: NotificationManagerCompat
    internal lateinit var scope: CoroutineScope

    internal var logWriter: LogWriter? = null
    private var controller: Controller?  = null

    private var jobReconnect: Job? = null

    private fun setRootState(state: Boolean) {
        setBooleanPrefValue(state, OscPrefKey.ROOT_STATE, prefs)
    }

    private fun requestTileListening() {
        TileService.requestListeningState(this,
            ComponentName(this, AcousticCouplerTileService::class.java)
        )
    }

    override fun onCreate() {
        notificationManager = NotificationManagerCompat.from(this)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == OscPrefKey.ROOT_STATE.name) {
                val newState = getBooleanPrefValue(OscPrefKey.ROOT_STATE, prefs)

                setBooleanPrefValue(newState, OscPrefKey.HOME_CONNECTOR, prefs)
                requestTileListening()
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_VPN_CONNECT -> {
                controller?.kill(null)

                beForegrounded()
                if (getBooleanPrefValue(OscPrefKey.LOG_DO_SAVE_LOG, prefs)) {
                    prepareLogWriter()
                }

                logWriter?.write("Establish VPN connection")

                initializeClient()

                setRootState(true)

                Service.START_STICKY
            }

            else -> {
                // ensure that reconnection has been completely canceled or done
                runBlocking { jobReconnect?.cancelAndJoin() }

                controller?.disconnect()
                controller = null

                close()

                Service.START_NOT_STICKY
            }
        }
    }

    private fun initializeClient() {
        controller = Controller(SharedBridge(this)).also {
            it.launchJobMain()
        }
    }

    private fun prepareLogWriter() {
        val currentDateTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val filename = "log_osc_${currentDateTime}.txt"

        val prefURI = getURIPrefValue(OscPrefKey.LOG_DIR, prefs)
        if (prefURI == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_PREFERENCE")
            return
        }

        val dirURI = DocumentFile.fromTreeUri(this, prefURI)
        if (dirURI == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_DIRECTORY")
            return
        }

        val fileURI = dirURI.createFile("text/plain", filename)
        if (fileURI == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_FILE")
            return
        }

        val stream = contentResolver.openOutputStream(fileURI.uri, "wa")
        if (stream == null) {
            makeNotification(NOTIFICATION_ERROR_ID, "LOG: ERR_NULL_STREAM")
            return
        }

        logWriter = LogWriter(stream)
    }

    private fun beForegrounded() {
        NotificationChannel(
            NOTIFICATION_CHANNEL_NAME,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).also {
            notificationManager.createNotificationChannel(it)
        }

        val pendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, AcousticCouplerService::class.java).setAction(ACTION_VPN_DISCONNECT),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_NAME).also {
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            it.setAutoCancel(true)
            it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            it.addAction(R.drawable.ic_baseline_close_24, "DISCONNECT", pendingIntent)
        }

        startForeground(NOTIFICATION_DISCONNECT_ID, builder.build())
    }

    internal fun makeNotification(id: Int, message: String) {
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_NAME).also {
            it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            it.setContentText(message)
            it.priority = NotificationCompat.PRIORITY_DEFAULT
            it.setAutoCancel(true)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(id, it.build())
            }
        }
    }

    internal fun close() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        logWriter?.write("Terminate VPN connection")
        logWriter?.close()
        logWriter = null

        controller?.kill(null)
        controller = null

        scope.cancel()

        setRootState(false)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
