package com.rsiot.concentrack

// InfoActivity.kt
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

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

    // Fungsi untuk menangani klik pada tombol kembali
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Kembali ke activity sebelumnya
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}