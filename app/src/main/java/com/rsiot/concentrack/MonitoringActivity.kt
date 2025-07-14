package com.rsiot.concentrack // Sesuaikan dengan package Anda

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

// Ubah AppCompatActivity menjadi BaseActivity
class MonitoringActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring)

        // 1. Mengatur Toolbar (kode ini spesifik untuk halaman ini)
        val toolbar: Toolbar = findViewById(R.id.toolbar_monitoring)
        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 2. Panggil fungsi dari BaseActivity untuk mengatur navigasi
        //    Gantilah R.id.nav_monitor dengan ID item menu untuk halaman monitoring Anda
        setupBottomNavigation(R.id.bottomNavigationView, R.id.nav_item_2)
    }

    // Metode untuk menu Toolbar (Bluetooth) tetap di sini
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.monitoring_menu, menu)
        return true
    }

    // Metode untuk tombol kembali di Toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish() // Panggil finish() agar animasi terpanggil
        return true
    }

    // Menerapkan animasi transisi saat activity ini ditutup
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    // Di dalam MonitoringActivity.kt

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Cek apakah item yang diklik adalah ikon Bluetooth
        return when (item.itemId) {
            R.id.action_bluetooth -> {
                // Buat Intent untuk membuka ConnectActivity
                val intent = Intent(this, ConnectActivity::class.java)

                // Jalankan Intent untuk berpindah halaman
                startActivity(intent)

                // Beri tahu sistem bahwa klik sudah ditangani
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}