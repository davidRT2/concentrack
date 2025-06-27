package com.rsiot.concentrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.choosemuse.libmuse.*

class MainActivity : ComponentActivity() {

    private val tag = "MuseEEG"
    private lateinit var museManager: MuseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Muse SDK
//        MuseManagerAndroid.setContext(applicationContext)
        museManager = MuseManagerAndroid.getInstance()

        // Request lokasi & bluetooth (untuk Android 12+)
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            1
        )

        // Mulai koneksi Muse
        connectToMuse()
    }

    override fun onResume() {
        super.onResume()
        museManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        museManager.stopListening()
    }

    private fun connectToMuse() {
        val muses = museManager.muses
        if (muses.isNotEmpty()) {
            val muse = muses[0]
            muse.registerConnectionListener(connectionListener)
            muse.registerDataListener(dataListener, MuseDataPacketType.EEG)
            muse.runAsynchronously()
        } else {
            Log.e(tag, "No Muse device found.")
        }
    }

    private val connectionListener = object : MuseConnectionListener() {
        override fun receiveMuseConnectionPacket(p: MuseConnectionPacket?, m: Muse?) {
            val state = p?.currentConnectionState
            when (state) {
                ConnectionState.CONNECTED -> Log.i(tag, "Connected to Muse")
                ConnectionState.CONNECTING -> Log.i(tag, "Connecting to Muse...")
                ConnectionState.DISCONNECTED -> Log.w(tag, "Muse disconnected")
                else -> Log.d(tag, "Connection state: $state")
            }
        }
    }

    private val dataListener = object : MuseDataListener() {
        override fun receiveMuseDataPacket(p: MuseDataPacket?, muse: Muse?) {
//            val eeg = p?.values
//            if (eeg != null) {
//                Log.i(tag, "EEG: ${eeg.joinToString(", ")}")
//            }
        }

        override fun receiveMuseArtifactPacket(p: MuseArtifactPacket?, muse: Muse?) {
            // Tidak dipakai sekarang
        }
    }
}
