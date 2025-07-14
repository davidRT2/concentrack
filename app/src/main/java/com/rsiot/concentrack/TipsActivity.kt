package com.rsiot.concentrack // Sesuaikan dengan package Anda

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView

// 1. Ubah AppCompatActivity menjadi BaseActivity
class TipsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips)

        // Mengatur Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar_tips)
        setSupportActionBar(toolbar)
        // Tombol kembali tidak lagi diperlukan karena navigasi ditangani oleh BottomNav
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Mengatur RecyclerView (kode ini tetap sama)
        val rvTips: RecyclerView = findViewById(R.id.rv_tips)
        setupRecyclerView(rvTips)

        // 2. Panggil fungsi dari BaseActivity untuk mengatur navigasi
        //    Gantilah R.id.nav_tips dengan ID item menu untuk halaman tips Anda
        setupBottomNavigation(R.id.bottomNavigationView, R.id.nav_item_3)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        // Siapkan data dummy
        val tipsData = listOf(
            Tip(R.drawable.ic_pomodoro, "Teknik Pomodoro", "Bekerja selama 25 menit, lalu istirahat 5 menit. Setelah 4 siklus, ambil istirahat lebih panjang (15-30 menit)."),
            Tip(R.drawable.ic_meditasi, "Meditasi Fokus", "Lakukan meditasi 10 menit setiap pagi untuk melatih pikiran agar lebih fokus sepanjang hari."),
            Tip(R.drawable.ic_lingkungan, "Lingkungan Kerja yang Baik", "Pastikan tempat kerja Anda rapi, cukup pencahayaan, dan minim gangguan."),
            Tip(R.drawable.ic_hidrasi, "Hidrasi yang Cukup", "Minum air secara teratur karena dehidrasi dapat mengurangi konsentrasi.")
        )
        recyclerView.adapter = TipsAdapter(tipsData)
    }

    // Metode onSupportNavigateUp() tidak lagi diperlukan karena sudah ditangani
    // oleh BaseActivity atau tidak relevan lagi dengan adanya BottomNav.

    // Opsional: Tambahkan ini jika Anda ingin animasi keluar yang konsisten
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}