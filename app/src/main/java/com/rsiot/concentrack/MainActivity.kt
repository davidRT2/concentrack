package com.rsiot.concentrack

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.choosemuse.libmuse.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var connectionTextView: TextView
    private lateinit var connectButton: Button
    private lateinit var eegChart: LineChart
    private lateinit var resetBluetoothButton: Button

    private lateinit var manager: MuseManagerAndroid
    var muse: Muse? = null

    private var connectionListener: ConnectionListener? = null
    private var dataListener: DataListener? = null
    private var isAttemptingToConnect = false
    private var isDisconnecting = false

    private var disconnectTimeoutHandler: Handler? = null
    private var disconnectTimeoutRunnable: Runnable? = null

    companion object {
        private const val TAG = "MuseEEGApp"
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val SCAN_TIMEOUT_MS = 10000L
        private const val DISCONNECT_RESET_DELAY_MS = 1000L
        private const val DISCONNECT_TIMEOUT_MS = 5000L
        private const val CONNECT_SCAN_DELAY_MS = 1500L
        private const val POST_DISCONNECT_SCAN_READY_DELAY_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        connectionTextView = findViewById(R.id.connectionTextView)
        connectButton = findViewById(R.id.connectButton)
        eegChart = findViewById(R.id.eegChart)
        resetBluetoothButton = findViewById(R.id.resetBluetoothButton)

        setupChart()

        manager = MuseManagerAndroid.getInstance()
        manager.setContext(this)

        manager.setMuseListener(object : MuseListener() {
            override fun museListChanged() {
                handleMuseListChanged()
            }
        })

        connectionListener = ConnectionListener(WeakReference(this))
        dataListener = DataListener(WeakReference(this))

        checkAndRequestPermissions()

        connectButton.setOnClickListener {
            // Check Bluetooth and Location status before attempting to scan
            if (!isBluetoothEnabled() || !isLocationEnabled()) {
                Toast.makeText(this, "Bluetooth dan Lokasi harus aktif untuk mencari Muse.", Toast.LENGTH_LONG).show()
                return@setOnClickListener // Stop here
            }

            if (muse == null || muse?.connectionState == ConnectionState.DISCONNECTED) {
                if (!isAttemptingToConnect && !isDisconnecting) {
                    startScan()
                } else {
                    Log.d(TAG, "Already attempting to connect or disconnecting, ignoring button press.")
                }
            } else {
                disconnectFromMuse()
            }
        }

        resetBluetoothButton.setOnClickListener {
            resetAndroidBluetooth()
        }

        updateUIState(false)
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    private fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
            locationManager.isLocationEnabled
        } else {
            // For older Android versions, check location mode
            val mode = android.provider.Settings.Secure.getInt(contentResolver, android.provider.Settings.Secure.LOCATION_MODE, android.provider.Settings.Secure.LOCATION_MODE_OFF)
            mode != android.provider.Settings.Secure.LOCATION_MODE_OFF
        }
    }

    private fun setupChart() {
        eegChart.description.isEnabled = false
        eegChart.setTouchEnabled(false)
        eegChart.isDragEnabled = false
        eegChart.setScaleEnabled(false)
        eegChart.setDrawGridBackground(false)
        eegChart.setPinchZoom(false)
        eegChart.setBackgroundColor(Color.TRANSPARENT)

        val data = LineData()
        data.addDataSet(createSet("EEG 1", Color.BLUE))
        data.addDataSet(createSet("EEG 2", Color.GREEN))
        data.addDataSet(createSet("EEG 3", Color.RED))
        data.addDataSet(createSet("EEG 4", Color.MAGENTA))
        eegChart.data = data

        eegChart.legend.form = Legend.LegendForm.LINE
        eegChart.legend.textColor = Color.DKGRAY

        val xl: XAxis = eegChart.xAxis
        xl.textColor = Color.DKGRAY
        xl.setDrawGridLines(false)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = true

        val leftAxis: YAxis = eegChart.axisLeft
        leftAxis.textColor = Color.DKGRAY
        leftAxis.axisMaximum = 2000.0f
        leftAxis.axisMinimum = -2000.0f
        leftAxis.setDrawGridLines(true)

        eegChart.axisRight.isEnabled = false
    }

    private fun createSet(label: String, color: Int): LineDataSet {
        return LineDataSet(null, label).apply {
            axisDependency = YAxis.AxisDependency.LEFT
            this.color = color
            setDrawCircles(false)
            lineWidth = 1.5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (isAttemptingToConnect) {
            Log.d(TAG, "Scan already in progress or connecting.")
            return
        }
        isAttemptingToConnect = true
        statusTextView.text = "Menunggu untuk memulai scan..."
        connectButton.isEnabled = false
        resetBluetoothButton.isEnabled = false

        Handler(Looper.getMainLooper()).postDelayed({
            statusTextView.text = "Mencari perangkat Muse..."
            manager.stopListening() // Ensure any previous scan is stopped
            manager.startListening()

            Handler(Looper.getMainLooper()).postDelayed({
                if (isAttemptingToConnect) {
                    manager.stopListening() // Stop listening after timeout
                    isAttemptingToConnect = false
                    if (muse == null || muse?.connectionState == ConnectionState.DISCONNECTED) {
                        statusTextView.text = "Perangkat Muse tidak ditemukan."
                        Toast.makeText(this, "Perangkat Muse tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                    connectButton.isEnabled = true
                    resetBluetoothButton.isEnabled = true
                }
            }, SCAN_TIMEOUT_MS)
        }, CONNECT_SCAN_DELAY_MS)
    }

    private fun handleMuseListChanged() {
        val muses = manager.muses
        if (isAttemptingToConnect && muses.isNotEmpty()) {
            isAttemptingToConnect = false
            manager.stopListening()
            runOnUiThread {
                statusTextView.text = "Perangkat ditemukan, menghubungkan..."
                connectToMuse(muses[0])
            }
        }
    }

    private fun connectToMuse(selectedMuse: Muse) {
        if (muse != null && muse?.connectionState == ConnectionState.CONNECTED) {
            Log.d(TAG, "Already connected to a Muse. Disconnecting current one first.")
            disconnectFromMuse()
            // Delay connection attempt to ensure old connection is fully cleaned up
            Handler(Looper.getMainLooper()).postDelayed({
                // Re-check after potential reset to ensure previous disconnection completed
                if (muse == null || muse?.connectionState == ConnectionState.DISCONNECTED) {
                    this.muse = selectedMuse
                    setupMuseListenersAndConnect(selectedMuse)
                } else {
                    Log.w(TAG, "Still connected to old Muse or in transition, cannot connect new one immediately.")
                    Toast.makeText(this, "Masih dalam transisi koneksi, coba lagi sebentar.", Toast.LENGTH_SHORT).show()
                    resetApplicationState() // Force reset if still stuck
                }
            }, DISCONNECT_RESET_DELAY_MS + 500L) // Add extra buffer
        } else {
            this.muse = selectedMuse
            setupMuseListenersAndConnect(selectedMuse)
        }
    }

    private fun setupMuseListenersAndConnect(museToConnect: Muse) {
        museToConnect.apply {
            unregisterAllListeners()
            setPreset(MusePreset.PRESET_14) // Ensure correct preset is set
            enableException(true)
            enableDataTransmission(true)
            registerConnectionListener(connectionListener)
            registerDataListener(dataListener, MuseDataPacketType.EEG)
            runAsynchronously()
        }
        updateUIState(false)
    }

    private fun disconnectFromMuse() {
        if (muse == null || isDisconnecting) {
            Log.d(TAG, "Muse is already null or disconnect process is ongoing.")
            return
        }

        isDisconnecting = true
        updateUIState(false)
        statusTextView.text = "Memutus koneksi dari Muse..."
        connectButton.isEnabled = false
        resetBluetoothButton.isEnabled = false

        disconnectTimeoutHandler = Handler(Looper.getMainLooper())
        disconnectTimeoutRunnable = Runnable {
            Log.w(TAG, "Disconnect timeout reached. Forcibly resetting state.")
            resetApplicationState()
            Toast.makeText(this, "Pemutusan koneksi terpaksa direset karena timeout.", Toast.LENGTH_LONG).show()
        }
        disconnectTimeoutHandler?.postDelayed(disconnectTimeoutRunnable!!, DISCONNECT_TIMEOUT_MS)

        muse?.apply {
            try {
                Log.d(TAG, "Memanggil disconnect(), menunggu event DISCONNECTED...")
                // Documentation says if you don't want event, unregister first.
                // We DO want the event, so unregistering all listeners *after* disconnect() is safe,
                // but we might want to unregister immediately to prevent new data callbacks during disconnect.
                // However, current setup where unregisterAllListeners() is in resetApplicationState() after delay is fine
                // as it ensures we get the DISCONNECTED callback.
                enableDataTransmission(false) // Stop data transmission
                disconnect() // Initiate disconnect
            } catch (e: Exception) {
                Log.e(TAG, "Error saat disconnect: ${e.message}")
                cancelDisconnectTimeout()
                resetApplicationState()
            }
        } ?: run {
            Log.e(TAG, "Muse object was null during disconnect attempt, resetting state.")
            cancelDisconnectTimeout()
            resetApplicationState()
        }
    }

    private fun resetApplicationState() {
        runOnUiThread {
            Log.d(TAG, "Resetting application state.")
            // Ensure all listeners are unregistered from the *current* Muse object immediately
            muse?.unregisterAllListeners()
            muse = null // Set muse to null early
            manager.stopListening() // Ensure manager stops listening for any lingering issues
            isAttemptingToConnect = false
            isDisconnecting = false
            cancelDisconnectTimeout()

            connectionTextView.text = "Status: Terputus"
            eegChart.visibility = View.INVISIBLE
            eegChart.data?.clearValues()
            eegChart.invalidate()

            statusTextView.text = "Perangkat sedang bersiap untuk disambungkan kembali..."
            connectButton.isEnabled = false
            resetBluetoothButton.isEnabled = false

            Handler(Looper.getMainLooper()).postDelayed({
                if (!isAttemptingToConnect && !isDisconnecting) {
                    statusTextView.text = "Tekan tombol untuk mulai."
                    connectButton.isEnabled = true
                    resetBluetoothButton.isEnabled = true
                    Toast.makeText(this, "Siap untuk memindai kembali.", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Application state reset complete. Scan button re-enabled.")
                } else {
                    Log.d(TAG, "Scan button not re-enabled by reset as another process started.")
                }
            }, POST_DISCONNECT_SCAN_READY_DELAY_MS)
        }
    }

    private fun cancelDisconnectTimeout() {
        disconnectTimeoutHandler?.removeCallbacks(disconnectTimeoutRunnable ?: return)
        disconnectTimeoutHandler = null
        disconnectTimeoutRunnable = null
        Log.d(TAG, "Disconnect timeout cancelled.")
    }

    @SuppressLint("MissingPermission")
    private fun resetAndroidBluetooth() {
        Log.d(TAG, "Attempting to reset Android Bluetooth adapter.")
        Toast.makeText(this, "Mereset Bluetooth Android...", Toast.LENGTH_SHORT).show()

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Perangkat tidak mendukung Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }

        connectButton.isEnabled = false
        resetBluetoothButton.isEnabled = false
        statusTextView.text = "Mereset Bluetooth sistem..."

        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
            Handler(Looper.getMainLooper()).postDelayed({
                if (!bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.enable()
                    Toast.makeText(this, "Bluetooth direset dan diaktifkan kembali. Coba sambungkan ulang Muse.", Toast.LENGTH_LONG).show()
                    statusTextView.text = "Bluetooth sistem aktif. Siap untuk memindai."
                    connectButton.isEnabled = true
                    resetBluetoothButton.isEnabled = true
                } else {
                    Toast.makeText(this, "Gagal mematikan Bluetooth, coba manual.", Toast.LENGTH_LONG).show()
                    statusTextView.text = "Gagal mereset Bluetooth."
                    connectButton.isEnabled = true
                    resetBluetoothButton.isEnabled = true
                }
            }, 2000)
        } else {
            bluetoothAdapter.enable()
            Toast.makeText(this, "Bluetooth diaktifkan kembali. Coba sambungkan ulang Muse.", Toast.LENGTH_LONG).show()
            statusTextView.text = "Bluetooth sistem aktif. Siap untuk memindai."
            connectButton.isEnabled = true
            resetBluetoothButton.isEnabled = true
        }
    }

    fun updateUIState(isConnected: Boolean) {
        runOnUiThread {
            connectionTextView.text = if (isConnected) "Status: Terhubung" else "Status: Terputus"
            connectButton.text = if (isConnected) "Putuskan Koneksi" else "Cari & Hubungkan"

            if (isConnected) {
                statusTextView.text = "Menerima data EEG..."
                eegChart.visibility = View.VISIBLE
                connectButton.isEnabled = true
                resetBluetoothButton.isEnabled = true
            } else {
                eegChart.visibility = View.INVISIBLE
                eegChart.data?.clearValues()
                eegChart.invalidate()

                if (isAttemptingToConnect) {
                    statusTextView.text = "Mencari perangkat Muse..."
                    connectButton.isEnabled = false
                    resetBluetoothButton.isEnabled = false
                } else if (isDisconnecting) {
                    statusTextView.text = "Memutus koneksi..."
                    connectButton.isEnabled = false
                    resetBluetoothButton.isEnabled = false
                } else {
                    if (muse == null && !isAttemptingToConnect && !isDisconnecting) {
                        statusTextView.text = "Tekan tombol untuk mulai."
                        connectButton.isEnabled = true
                        resetBluetoothButton.isEnabled = true
                    }
                }
            }
        }
    }

    fun receiveMuseData(packet: MuseDataPacket) {
        if (packet.packetType() == MuseDataPacketType.EEG) {
            val eeg1 = packet.getEegChannelValue(Eeg.EEG1)
            val eeg2 = packet.getEegChannelValue(Eeg.EEG2)
            val eeg3 = packet.getEegChannelValue(Eeg.EEG3)
            val eeg4 = packet.getEegChannelValue(Eeg.EEG4)
            Log.v(TAG, "EEG: $eeg1, $eeg2, $eeg3, $eeg4")
            updateChart(eeg1, eeg2, eeg3, eeg4)
        }
    }

    private fun updateChart(eeg1: Double, eeg2: Double, eeg3: Double, eeg4: Double) {
        val data = eegChart.data ?: return
        val values = listOf(eeg1, eeg2, eeg3, eeg4)

        for (i in values.indices) {
            var set = data.getDataSetByIndex(i)
            if (set == null) {
                val label = "EEG ${i + 1}"
                val color = when (i) {
                    0 -> Color.BLUE
                    1 -> Color.GREEN
                    2 -> Color.RED
                    3 -> Color.MAGENTA
                    else -> Color.BLACK
                }
                set = createSet(label, color)
                data.addDataSet(set)
            }
            data.addEntry(Entry(set.entryCount.toFloat(), values[i].toFloat()), i)
        }

        data.notifyDataChanged()
        eegChart.notifyDataSetChanged()
        eegChart.setVisibleXRangeMaximum(150f)
        eegChart.moveViewToX(data.entryCount.toFloat())
    }

    internal class ConnectionListener(private val activityRef: WeakReference<MainActivity>) : MuseConnectionListener() {
        override fun receiveMuseConnectionPacket(p: MuseConnectionPacket, muse: Muse) {
            val activity = activityRef.get() ?: return
            val state = p.currentConnectionState
            val previous = p.previousConnectionState
            Log.d(TAG, "Connection changed: $previous → $state")

            activity.runOnUiThread {
                activity.updateUIState(state == ConnectionState.CONNECTED)
            }

            if (state == ConnectionState.CONNECTED) {
                Log.d(TAG, "Muse Connected.")
                activity.cancelDisconnectTimeout()
            } else if (state == ConnectionState.DISCONNECTED) {
                Log.d(TAG, "Muse Disconnected. Reason: ${p.previousConnectionState}")

                Handler(Looper.getMainLooper()).postDelayed({
                    activity.resetApplicationState()
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Muse terputus.", Toast.LENGTH_SHORT).show()
                    }
                }, DISCONNECT_RESET_DELAY_MS)
            }
        }
    }

    internal class DataListener(private val activityRef: WeakReference<MainActivity>) : MuseDataListener() {
        override fun receiveMuseDataPacket(p: MuseDataPacket, muse: Muse) {
            activityRef.get()?.receiveMuseData(p)
        }
        override fun receiveMuseArtifactPacket(p: MuseArtifactPacket, muse: Muse) {}
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissionsApi31()
        } else {
            requestLegacyBluetoothPermissions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissionsApi31() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestLegacyBluetoothPermissions() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (results.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Izin Bluetooth diberikan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin Bluetooth diperlukan agar aplikasi berfungsi", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetApplicationState() // Ensure all states are reset when Activity is destroyed
        manager.setMuseListener(null) // Unregister manager listener as well
        // Ensure that any Muse object is also fully cleaned up by explicitly unregistering listeners
        muse?.unregisterAllListeners()
    }
}