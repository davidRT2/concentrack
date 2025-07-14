package com.rsiot.concentrack // Sesuaikan dengan package Anda

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar

class ConnectActivity : AppCompatActivity() {

    // 1. Menyiapkan Activity Result Launcher untuk menangani permintaan aktivasi Bluetooth
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Pengguna telah mengaktifkan Bluetooth
            Toast.makeText(this, "Bluetooth telah diaktifkan.", Toast.LENGTH_SHORT).show()
        } else {
            // Pengguna tidak mengaktifkan Bluetooth
            Toast.makeText(this, "Bluetooth diperlukan untuk menyambungkan perangkat.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        val toolbar: Toolbar = findViewById(R.id.toolbar_connect)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Setup kartu MUSE
        val cardMuse: View = findViewById(R.id.card_muse)
        val tvMuseName: TextView = cardMuse.findViewById(R.id.tv_device_name)
        val ivMuseImage: ImageView = cardMuse.findViewById(R.id.iv_device_image)
        tvMuseName.text = "MUSE 2"
        ivMuseImage.setImageResource(R.drawable.muse2) // Pastikan Anda punya drawable ini
        cardMuse.setOnClickListener {
            // Panggil pengecekan sebelum melanjutkan
            if (checkPrerequisites()) {
                // Jika semua sudah aktif, lanjutkan proses koneksi
                Toast.makeText(this, "Mencari perangkat MUSE 2...", Toast.LENGTH_SHORT).show()
                // TODO: Tambahkan logika untuk memulai pemindaian Bluetooth untuk MUSE 2
            }
        }

        // Setup kartu Concentrack (gelang)
        val cardConcentrack: View = findViewById(R.id.card_concentrack)
        val tvConcentrackName: TextView = cardConcentrack.findViewById(R.id.tv_device_name)
        val ivConcentrackImage: ImageView = cardConcentrack.findViewById(R.id.iv_device_image)
        tvConcentrackName.text = "Concentrack"
        ivConcentrackImage.setImageResource(R.drawable.concentrack) // Pastikan Anda punya drawable ini
        cardConcentrack.setOnClickListener {
            // Panggil pengecekan sebelum melanjutkan
            if (checkPrerequisites()) {
                // Jika semua sudah aktif, lanjutkan proses koneksi
                Toast.makeText(this, "Mencari gelang alerting...", Toast.LENGTH_SHORT).show()
                // TODO: Tambahkan logika untuk memulai pemindaian Bluetooth untuk gelang
            }
        }
    }

    // 2. Fungsi untuk memeriksa semua prasyarat (Bluetooth & Lokasi)
    private fun checkPrerequisites(): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Cek 1: Apakah perangkat mendukung Bluetooth?
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth.", Toast.LENGTH_LONG).show()
            return false
        }

        // Cek 2: Apakah Bluetooth aktif? Jika tidak, minta untuk diaktifkan.
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
            return false
        }

        // Cek 3: Apakah Lokasi aktif?
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Mohon aktifkan Lokasi untuk pemindaian perangkat.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return false
        }

        // Jika semua prasyarat terpenuhi
        return true
    }

    // Fungsi untuk menangani klik pada tombol kembali di toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}