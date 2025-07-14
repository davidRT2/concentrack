package com.rsiot.concentrack

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.widget.Toolbar

// Pastikan HomeActivity mewarisi BaseActivity
class HomeActivity : BaseActivity() {
    // Di dalam class HomeActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Mengatur Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Selamat Datang"

        // 2. Mengatur Navigasi Bawah
        setupBottomNavigation(R.id.bottomNavigationView, R.id.nav_item_1)

        // 3. Listener untuk tombol "Monitoring sekarang" (kode ini sudah benar)
        val monitoringButton: Button = findViewById(R.id.button_monitoring)
        monitoringButton.setOnClickListener {
            val intent = Intent(this, MonitoringActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }

        // 4. Listener untuk tombol "Lihat selengkapnya" (TAMBAHKAN INI)
        val tipsButton: Button = findViewById(R.id.button_tips)
        tipsButton.setOnClickListener {
            val intent = Intent(this, TipsActivity::class.java)
            startActivity(intent)
            // Gunakan animasi slide untuk konsistensi
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
        }
    }
    // Metode untuk menu di Toolbar tetap di sini karena spesifik untuk HomeActivity
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                val intent = Intent(this, InfoActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}