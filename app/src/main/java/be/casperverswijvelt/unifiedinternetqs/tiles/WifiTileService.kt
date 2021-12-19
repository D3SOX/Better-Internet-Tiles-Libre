package be.casperverswijvelt.unifiedinternetqs.tiles

import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.preference.PreferenceManager
import be.casperverswijvelt.unifiedinternetqs.*
import be.casperverswijvelt.unifiedinternetqs.listeners.NetworkChangeCallback
import be.casperverswijvelt.unifiedinternetqs.listeners.NetworkChangeType
import be.casperverswijvelt.unifiedinternetqs.listeners.WifiChangeListener
import be.casperverswijvelt.unifiedinternetqs.util.*

class WifiTileService : TileService() {

    private companion object {
        const val TAG = "WifiTile"
    }

    private var wifiConnected = false
    private var sharedPreferences: SharedPreferences? = null

    private val runToggleInternet = Runnable {
        toggleWifi()
        syncTile()
    }
    private val networkChangeCallback = object : NetworkChangeCallback {
        override fun handleChange(type: NetworkChangeType?) {
            if (type == NetworkChangeType.NETWORK_LOST) wifiConnected = false
            else if (type == NetworkChangeType.NETWORK_AVAILABLE) wifiConnected = true
            syncTile()
        }
    }

    private var wifiChangeListener: WifiChangeListener? = null

    override fun onCreate() {
        super.onCreate()
        log("Wi-Fi tile service created")

        wifiChangeListener = WifiChangeListener(networkChangeCallback)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()

        setListeners()
        syncTile()
    }


    override fun onStopListening() {
        super.onStopListening()

        removeListeners()
    }

    override fun onTileAdded() {
        super.onTileAdded()

        setListeners()
        syncTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()

        removeListeners()
    }

    override fun onClick() {
        super.onClick()

        if (!hasShellAccess()) {

            // Either root or Shizuku access is needed to enable/disable mobile data and Wi-Fi.
            //  There is currently no other way to do this, so this functionality will not work
            //  without root Shizuku access.
            showDialog(getShellAccessRequiredDialog(applicationContext))
            return
        }

        if (
            sharedPreferences?.getBoolean(
                resources.getString(R.string.require_unlock_key),
                true
            ) == true
        ) {

            unlockAndRun(runToggleInternet)

        } else {

            runToggleInternet.run()
        }
    }

    private fun toggleWifi() {

        val wifiEnabled = getWifiEnabled(applicationContext)

        executeShellCommand(if (wifiEnabled) {
            "svc wifi disable"
        } else {
            "svc wifi enable"
        })
    }

    private fun syncTile() {

        val wifiEnabled = getWifiEnabled(applicationContext)

        if (wifiEnabled) {

            // If Wi-Fi is connected, get Wi-Fi SSID through shell command and regex parsing since app needs access
            //  to fine location to get SSID

            var ssid: String? = null

            if (wifiConnected) {
                ssid = getConnectedWifiSSID()
            }

            // Update tile properties

            qsTile.label = ssid ?: resources.getString(R.string.wifi)
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.icon = getWifiIcon(applicationContext)
            qsTile.subtitle = resources.getString(R.string.on)

        } else {

            qsTile.label = resources.getString(R.string.wifi)
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.icon = Icon.createWithResource(
                this,
                R.drawable.ic_baseline_signal_wifi_0_bar_24
            )
            qsTile.subtitle = resources.getString(R.string.off)
        }

        qsTile.updateTile()
    }

    private fun setListeners() {

        log("Setting listeners")

        wifiConnected = false

        wifiChangeListener?.startListening(applicationContext)
    }

    private fun removeListeners() {

        log("Removing listeners")

        wifiChangeListener?.stopListening(applicationContext)
    }

    private fun log(text: String) {
        Log.d(TAG, text)
    }
}