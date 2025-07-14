package com.rsiot.concentrack

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

// Tetap mewarisi dari AppCompatActivity karena tidak ada BottomNavigationView
class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val toolbar: Toolbar = findViewById(R.id.toolbar_info)
        setSupportActionBar(toolbar)

        // Menampilkan tombol kembali (panah kiri) di toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    // Fungsi untuk menangani klik pada tombol kembali di toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // android.R.id.home adalah ID untuk tombol panah kembali
        if (item.itemId == android.R.id.home) {
            // Panggil finish() agar animasi transisi juga terpanggil
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Menerapkan animasi transisi saat activity ini ditutup
    override fun finish() {
        super.finish()
        // Halaman lama masuk dengan pudar, halaman ini keluar ke kiri
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}