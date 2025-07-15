package com.rsiot.concentrack // Sesuaikan dengan package Anda

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.choosemuse.libmuse.ConnectionState
import com.choosemuse.libmuse.Muse

@SuppressLint("MissingPermission")
class ConnectActivity : AppCompatActivity(), MuseConnectionManager.ConnectionStateListener, MuseConnectionManager.MuseListCallbackListener {

    private enum class UiState { IDLE, SCANNING, CONNECTING, CONNECTED, FAILED }

    // Komponen UI
    private lateinit var progressBar: ProgressBar
    private lateinit var cardMuse: View
    private lateinit var cardConcentrack: View
    private var targetCardView: View? = null

    // Adapter untuk dialog pemilihan perangkat
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val handler = Handler(Looper.getMainLooper())

    // --- ActivityResultLaunchers ---
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            startBleScan() // Coba lagi setelah izin diberikan
        } else {
            Toast.makeText(this, "Izin dibutuhkan untuk menghubungkan perangkat", Toast.LENGTH_LONG).show()
            updateUiState(UiState.IDLE)
        }
    }

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!isBluetoothEnabled()) {
            Toast.makeText(this, "Bluetooth wajib diaktifkan", Toast.LENGTH_SHORT).show()
            updateUiState(UiState.IDLE)
        } else {
            startBleScan()
        }
    }

    // --- Lifecycle Methods ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        // Inisialisasi Singleton Manager. Gunakan applicationContext agar tidak terikat pada Activity.
        MuseConnectionManager.init(applicationContext)

        initializeUi()
        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        // Daftarkan activity ini sebagai listener saat aktif
        MuseConnectionManager.setConnectionListener(this)
        MuseConnectionManager.setMuseListListener(this)

        // Perbarui UI berdasarkan status koneksi saat ini dari manager
        val currentMuse = MuseConnectionManager.muse
        if (currentMuse != null && currentMuse.connectionState == ConnectionState.CONNECTED) {
            updateUiState(UiState.CONNECTED)
            updateCardUi(true)
        } else {
            updateUiState(UiState.IDLE)
            updateCardUi(false)
        }
    }

    override fun onPause() {
        super.onPause()
        // Hapus listener saat activity tidak aktif untuk mencegah memory leak
        MuseConnectionManager.setConnectionListener(null)
        MuseConnectionManager.setMuseListListener(null)
        handler.removeCallbacksAndMessages(null) // Hentikan timer jika ada
    }

    // --- Inisialisasi ---
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_connect)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sambungkan Perangkat"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initializeUi() {
        progressBar = findViewById(R.id.progressBar)
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)

        cardMuse = findViewById(R.id.card_muse)
        cardConcentrack = findViewById(R.id.card_concentrack)

        setupDeviceCard(cardMuse, "MUSE 2", R.drawable.muse2)
        setupDeviceCard(cardConcentrack, "Concentrack", R.drawable.concentrack)
    }

    private fun setupDeviceCard(cardView: View, name: String, imageResId: Int) {
        cardView.findViewById<TextView>(R.id.tv_device_name).text = name
        cardView.findViewById<ImageView>(R.id.iv_device_image).setImageResource(imageResId)
        cardView.setOnClickListener {
            if (name.contains("MUSE", true)) {
                val currentMuse = MuseConnectionManager.muse
                if (currentMuse != null && currentMuse.connectionState == ConnectionState.CONNECTED) {
                    Toast.makeText(this, "Memutuskan sambungan...", Toast.LENGTH_SHORT).show()
                    MuseConnectionManager.disconnect()
                } else {
                    targetCardView = it
                    startBleScan()
                }
            } else {
                Toast.makeText(this, "Fungsionalitas untuk $name belum tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Alur Utama & Logika Koneksi ---
    private fun startBleScan() {
        if (!checkPrerequisites()) return

        updateUiState(UiState.SCANNING)
        Toast.makeText(this, "Memindai perangkat Muse...", Toast.LENGTH_SHORT).show()

        MuseConnectionManager.startScan()

        // Hentikan pemindaian setelah 10 detik dan tampilkan hasilnya
        handler.postDelayed({
            MuseConnectionManager.stopScan()
            // Pengecekan dilakukan di onMuseListChanged, tapi kita perlu handle jika tidak ada hasil sama sekali
            if (spinnerAdapter.isEmpty) {
                Toast.makeText(this, "Tidak ada perangkat Muse ditemukan", Toast.LENGTH_SHORT).show()
                updateUiState(UiState.IDLE)
            } else {
                showDeviceListPopup()
            }
        }, 10000)
    }

    private fun showDeviceListPopup() {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("Pilih Perangkat Muse")
            .setAdapter(spinnerAdapter) { dialog, which ->
                val muses = MuseConnectionManager.getMuses()
                if (which < muses.size) {
                    MuseConnectionManager.connect(muses[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                updateUiState(UiState.IDLE)
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    // --- Manajemen State UI ---
    private fun updateUiState(state: UiState) {
        runOnUiThread {
            progressBar.visibility = if (state == UiState.SCANNING || state == UiState.CONNECTING) View.VISIBLE else View.GONE

            cardMuse.isEnabled = state != UiState.SCANNING && state != UiState.CONNECTING
            cardConcentrack.isEnabled = false

            if (state == UiState.CONNECTED) {
                Toast.makeText(this, "Berhasil terhubung!", Toast.LENGTH_LONG).show()
                // Navigasi bisa ditambahkan di sini jika diperlukan, misal ke halaman monitoring
                // startActivity(Intent(this, MonitoringActivity::class.java))
            } else if (state == UiState.FAILED) {
                Toast.makeText(this, "Gagal terhubung ke perangkat.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateCardUi(isConnected: Boolean) {
        // Karena targetCardView bisa null saat kembali ke halaman ini,
        // kita update kartu Muse secara eksplisit.
        val cardToUpdate = targetCardView ?: findViewById(R.id.card_muse)
        cardToUpdate?.let { card ->
            val checkmark = card.findViewById<ImageView>(R.id.iv_checkmark)
            val status = card.findViewById<TextView>(R.id.tv_connection_status)
            checkmark.visibility = if (isConnected) View.VISIBLE else View.GONE
            status.visibility = if (isConnected) View.VISIBLE else View.GONE
        }
    }

    // --- Prerequisite & Permission Checks ---
    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled ?: false
    }

    private fun checkPrerequisites(): Boolean {
        if (!isBluetoothEnabled()) {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return false
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            return false
        }
        return true
    }

    // --- Listener Callbacks dari MuseConnectionManager ---
    override fun onMuseListChanged(muses: List<Muse>) {
        runOnUiThread {
            spinnerAdapter.clear()
            for (m in muses) {
                spinnerAdapter.add(m.name + " - " + m.macAddress)
            }
        }
    }

    override fun onConnectionStateChanged(state: ConnectionState) {
        runOnUiThread {
            when (state) {
                ConnectionState.CONNECTED -> {
                    updateUiState(UiState.CONNECTED)
                    updateCardUi(true)
                }
                ConnectionState.DISCONNECTED -> {
                    Toast.makeText(this, "Sambungan terputus.", Toast.LENGTH_SHORT).show()
                    updateUiState(UiState.IDLE)
                    updateCardUi(false)
                }
                ConnectionState.CONNECTING -> updateUiState(UiState.CONNECTING)
                else -> updateUiState(UiState.FAILED)
            }
        }
    }
}
