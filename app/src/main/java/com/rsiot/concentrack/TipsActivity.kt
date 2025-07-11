package com.rsiot.concentrack // Sesuaikan dengan package Anda

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView

class TipsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips)

        val toolbar: Toolbar = findViewById(R.id.toolbar_tips)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val rvTips: RecyclerView = findViewById(R.id.rv_tips)

        // Siapkan data dummy
        val tipsData = listOf(
            Tip(R.drawable.ic_pomodoro, "Teknik Pomodoro", "Bekerja selama 25 menit, lalu istirahat 5 menit. Setelah 4 siklus, ambil istirahat lebih panjang (15-30 menit)."),
            Tip(R.drawable.ic_meditasi, "Meditasi Fokus", "Lakukan meditasi 10 menit setiap pagi untuk melatih pikiran agar lebih fokus sepanjang hari."),
            Tip(R.drawable.ic_lingkungan, "Lingkungan Kerja yang Baik", "Pastikan tempat kerja Anda rapi, cukup pencahayaan, dan minim gangguan."),
            Tip(R.drawable.ic_hidrasi, "Hidrasi yang Cukup", "Minum air secara teratur karena dehidrasi dapat mengurangi konsentrasi.")
        )

        rvTips.adapter = TipsAdapter(tipsData)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}