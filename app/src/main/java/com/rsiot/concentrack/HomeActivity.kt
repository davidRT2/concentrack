package com.rsiot.concentrack

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Mengatur Toolbar sebagai App Bar utama
        // Pastikan di activity_home.xml ada Toolbar dengan android:id="@+id/toolbar"
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Opsional: Mengganti judul toolbar dari kode
        supportActionBar?.title = "Selamat Datang"

        // 2. Mengatur BottomNavigationView
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        setupBottomNav(bottomNavigationView)
    }

    private fun setupBottomNav(bottomNavView: BottomNavigationView) {
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_item_1 -> {
                    // Anda sudah di halaman home
                    true
                }
                R.id.nav_item_2 -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }R.id.nav_item_3 -> {
                    val intent = Intent(this, TipsActivity::class.java)
                    startActivity(intent)
                    true
                }
                // Anda bisa menambahkan case untuk item lain di sini
                // R.id.nav_item_3 -> { ... ; true }
                // R.id.nav_item_4 -> { ... ; true }
                else -> false
            }
        }
    }

    // 3. Metode untuk menampilkan menu (ikon) di Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    // 4. Metode untuk menangani klik pada item menu di Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                // Buka InfoActivity saat ikon info diklik
                val intent = Intent(this, InfoActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}