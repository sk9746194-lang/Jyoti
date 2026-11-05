package com.myra.assistant.voice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

/**
 * Handles Bluetooth SCO (Synchronous Connection-Oriented) Audio routing
 * for hands-free wireless earbuds, car audio systems, and Bluetooth headsets.
 */
class BluetoothAudioManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isScoStarted = false
    private var bluetoothHeadset: BluetoothHeadset? = null

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            Log.d("MYRA_BT", "Bluetooth SCO audio state changed to: $state")
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                audioManager.isBluetoothScoOn = true
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                audioManager.isBluetoothScoOn = false
            }
        }
    }

    fun startBluetoothSco() {
        try {
            context.registerReceiver(
                scoReceiver,
                IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            )
            audioManager.startBluetoothSco()
            isScoStarted = true
            Log.d("MYRA_BT", "Started Bluetooth SCO")
        } catch (e: Exception) {
            Log.e("MYRA_BT", "Error starting Bluetooth SCO", e)
        }
    }

    fun stopBluetoothSco() {
        try {
            if (isScoStarted) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                context.unregisterReceiver(scoReceiver)
                isScoStarted = false
                Log.d("MYRA_BT", "Stopped Bluetooth SCO")
            }
        } catch (e: Exception) {
            Log.e("MYRA_BT", "Error stopping Bluetooth SCO", e)
        }
    }
}
