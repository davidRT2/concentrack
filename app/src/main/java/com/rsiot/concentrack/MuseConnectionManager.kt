package com.rsiot.concentrack

import android.content.Context
import com.choosemuse.libmuse.ConnectionState
import com.choosemuse.libmuse.Muse
import com.choosemuse.libmuse.MuseConnectionListener
import com.choosemuse.libmuse.MuseConnectionPacket
import com.choosemuse.libmuse.MuseDataListener
import com.choosemuse.libmuse.MuseListener
import com.choosemuse.libmuse.MuseManagerAndroid
import java.lang.ref.WeakReference

/**
 * Singleton object to manage the Muse connection throughout the app's lifecycle.
 * This allows the connection to persist across different Activities.
 */
object MuseConnectionManager {

    var muse: Muse? = null
        private set

    private lateinit var manager: MuseManagerAndroid

    // Listeners to allow Activities to react to changes
    private var connectionListener: WeakReference<ConnectionStateListener>? = null
    private var museListListener: WeakReference<MuseListCallbackListener>? = null

    fun init(context: Context) {
        manager = MuseManagerAndroid.getInstance()
        manager.setContext(context)
        manager.setMuseListener(object : MuseListener() {
            override fun museListChanged() {
                museListListener?.get()?.onMuseListChanged(manager.muses)
            }
        })
    }

    fun startScan() {
        manager.stopListening()
        manager.startListening()
    }

    fun stopScan() {
        manager.stopListening()
    }

    fun getMuses(): List<Muse> = manager.muses

    fun connect(museToConnect: Muse) {
        // Stop scanning when we try to connect
        stopScan()

        this.muse = museToConnect
        muse?.unregisterAllListeners()
        muse?.registerConnectionListener(object : MuseConnectionListener() {
            override fun receiveMuseConnectionPacket(p: MuseConnectionPacket, m: Muse) {
                val currentState = p.currentConnectionState
                connectionListener?.get()?.onConnectionStateChanged(currentState)

                // If disconnected, clear the muse object
                if (currentState == ConnectionState.DISCONNECTED || currentState == ConnectionState.UNKNOWN) {
                    muse = null
                }
            }
        })
        muse?.runAsynchronously()
    }

    fun disconnect() {
        muse?.disconnect()
    }

    // Use this method in Activities that need to receive data (e.g., MonitoringActivity)
    fun registerDataListener(listener: MuseDataListener, type: com.choosemuse.libmuse.MuseDataPacketType) {
        muse?.registerDataListener(listener, type)
    }

    fun unregisterDataListener(listener: MuseDataListener, type: com.choosemuse.libmuse.MuseDataPacketType) {
        muse?.unregisterDataListener(listener, type)
    }

    // --- Listener Management ---
    fun setConnectionListener(listener: ConnectionStateListener?) {
        this.connectionListener = if (listener != null) WeakReference(listener) else null
    }

    fun setMuseListListener(listener: MuseListCallbackListener?) {
        this.museListListener = if (listener != null) WeakReference(listener) else null
    }

    interface ConnectionStateListener {
        fun onConnectionStateChanged(state: ConnectionState)
    }

    interface MuseListCallbackListener {
        fun onMuseListChanged(muses: List<Muse>)
    }
}
