package com.rsiot.concentrack // Sesuaikan dengan package Anda

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

// Gunakan 'abstract' agar class ini tidak bisa dijalankan sendiri, hanya untuk diwariskan
abstract class BaseActivity : AppCompatActivity() {

    // Fungsi ini bisa dipanggil oleh semua activity turunan
    protected fun setupBottomNavigation(navViewId: Int, currentItemId: Int) {
        val bottomNavigationView: BottomNavigationView = findViewById(navViewId)

        // Menandai item mana yang sedang aktif
        bottomNavigationView.selectedItemId = currentItemId

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == currentItemId) {
                // Jika item yang sama diklik lagi, jangan lakukan apa-apa
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.nav_item_1 -> {
                    startActivityWithAnimation(Intent(this, HomeActivity::class.java))
                }
                R.id.nav_item_2 -> {
                    startActivityWithAnimation(Intent(this, MonitoringActivity::class.java))
                }
                R.id.nav_item_3 -> {
                    startActivityWithAnimation(Intent(this, TipsActivity::class.java))
                }
            }
            true
        }
    }

    private fun startActivityWithAnimation(intent: Intent) {
        startActivity(intent)
        // Menerapkan animasi fade untuk semua perpindahan dari navbar
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish() // Menutup activity saat ini agar tidak menumpuk
    }
}