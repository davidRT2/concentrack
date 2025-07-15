package com.rsiot.concentrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.choosemuse.libmuse.ConnectionState
import com.choosemuse.libmuse.Eeg
import com.choosemuse.libmuse.Muse
import com.choosemuse.libmuse.MuseArtifactPacket
import com.choosemuse.libmuse.MuseDataListener
import com.choosemuse.libmuse.MuseDataPacket
import com.choosemuse.libmuse.MuseDataPacketType
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.concurrent.TimeUnit

// Ubah AppCompatActivity menjadi BaseActivity jika Anda menggunakannya
class MonitoringActivity : BaseActivity() {

    // Deklarasi komponen UI
    private lateinit var lineChart: LineChart
    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private lateinit var statusLabel: TextView

    // Variabel untuk state management
    private var dataListener: MuseDataListener? = null
    private var isMonitoring = false

    // --- PERBAIKAN: Menggunakan Batch Update yang Stabil ---
    private val uiHandler = Handler(Looper.getMainLooper())
    private var monitoringSeconds = 0
    // Buffer untuk menampung nilai Y (EEG value) saja
    private val eegBuffers = Array(4) { mutableListOf<Float>() }
    // Interval untuk memperbarui grafik (dalam milidetik)
    private val chartUpdateInterval = 100L // Update chart setiap 100ms

    // Runnable untuk memperbarui grafik secara berkala
    private val chartUpdater = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            updateChartWithBatchData()
            uiHandler.postDelayed(this, chartUpdateInterval)
        }
    }

    // Runnable untuk counter
    private val timerUpdater = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            monitoringSeconds++
            val minutes = monitoringSeconds / 60
            val seconds = monitoringSeconds % 60
            timerText.text = String.format("%02d:%02d", minutes, seconds)
            uiHandler.postDelayed(this, 1000L)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring)

        // 1. Inisialisasi UI
        lineChart = findViewById(R.id.view_brainwave_placeholder)
        timerText = findViewById(R.id.tv_durasi_value)
        startButton = findViewById(R.id.btn_mulai_monitoring)
        statusText = findViewById(R.id.tv_keterangan_value)
        statusLabel = findViewById(R.id.tv_keterangan_label)


        // 2. Mengatur Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar_monitoring)
        setSupportActionBar(toolbar)

        // 3. Panggil fungsi dari BaseActivity untuk mengatur navigasi
        setupBottomNavigation(R.id.bottomNavigationView, R.id.nav_item_2)

        // 4. Setup Grafik dan Tombol
        setupChart()
        setupButtons()
        timerText.text = "00:00" // Tampilan awal counter
    }

    override fun onResume() {
        super.onResume()
        // Cek status koneksi saat kembali ke halaman ini
        val muse = MuseConnectionManager.muse
        if (muse != null && muse.connectionState == ConnectionState.CONNECTED) {
            statusLabel.text = "Status Perangkat:"
            statusText.text = "Terhubung (${muse.name})"
            startButton.isEnabled = !isMonitoring // Aktifkan tombol jika monitoring belum berjalan
        } else {
            statusLabel.text = "Status Perangkat:"
            statusText.text = "Tidak Terhubung"
            startButton.isEnabled = false
            Toast.makeText(this, "Silakan sambungkan perangkat Muse terlebih dahulu", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Hentikan listener dan timer saat activity dijeda untuk menghemat baterai
        if (isMonitoring) {
            stopMonitoring()
        }
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            if (isMonitoring) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        monitoringSeconds = 0
        timerText.text = "00:00"
        startButton.text = "Hentikan Monitoring"
        startButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        statusLabel.text = "Status Monitoring:"
        statusText.text = "Sedang Berjalan..."

        // Bersihkan grafik dan buffer
        lineChart.data?.dataSets?.forEach { (it as LineDataSet).clear() }
        eegBuffers.forEach { it.clear() }
        lineChart.invalidate()

        // Mulai Counter dan Chart Updater
        uiHandler.post(timerUpdater)
        uiHandler.post(chartUpdater)

        // Mulai Mendengarkan Data EEG
        listenToMuseData()
    }

    private fun stopMonitoring() {
        isMonitoring = false
        // Hentikan semua runnable yang dijadwalkan
        uiHandler.removeCallbacks(timerUpdater)
        uiHandler.removeCallbacks(chartUpdater)

        startButton.text = "Mulai Monitoring"
        startButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.textColorSecondary) // Ganti R.color.grey dengan warna asli Anda
        statusLabel.text = "Status Perangkat:"
        statusText.text = "Terhubung (${MuseConnectionManager.muse?.name ?: "..."})"


        // Hentikan listener data
        dataListener?.let {
            MuseConnectionManager.unregisterDataListener(it, MuseDataPacketType.EEG)
        }
    }

    private fun listenToMuseData() {
        dataListener = object : MuseDataListener() {
            override fun receiveMuseDataPacket(p: MuseDataPacket, muse: Muse) {
                if (p.packetType() == MuseDataPacketType.EEG) {
                    // Cukup tambahkan data ke buffer, jangan update UI di sini
                    addEegToBuffer(p)
                }
            }
            override fun receiveMuseArtifactPacket(p: MuseArtifactPacket, muse: Muse) {
                // Handle artifacts jika perlu
            }
        }
        MuseConnectionManager.registerDataListener(dataListener!!, MuseDataPacketType.EEG)
    }

    private fun addEegToBuffer(p: MuseDataPacket) {
        // Fungsi ini berjalan di background thread, jadi perlu disinkronkan
        // untuk menghindari masalah saat buffer diakses dari UI thread
        synchronized(eegBuffers) {
            // PERBAIKAN: Hanya simpan nilai Y (float) ke buffer
            eegBuffers[0].add(p.getEegChannelValue(Eeg.EEG1).toFloat())
            eegBuffers[1].add(p.getEegChannelValue(Eeg.EEG2).toFloat())
            eegBuffers[2].add(p.getEegChannelValue(Eeg.EEG3).toFloat())
            eegBuffers[3].add(p.getEegChannelValue(Eeg.EEG4).toFloat())
        }
    }

    private fun updateChartWithBatchData() {
        synchronized(eegBuffers) {
            if (eegBuffers.any { it.isNotEmpty() }) {
                val data = lineChart.data
                if (data != null) {
                    for (i in eegBuffers.indices) {
                        val set = data.getDataSetByIndex(i)
                        if (set != null) {
                            // PERBAIKAN: Buat Entry baru dengan X sekuensial
                            for (value in eegBuffers[i]) {
                                data.addEntry(Entry(set.entryCount.toFloat(), value), i)
                            }
                        }
                    }
                    // Hapus data yang sudah digambar dari buffer
                    eegBuffers.forEach { it.clear() }

                    // Refresh chart setelah semua data ditambahkan
                    data.notifyDataChanged()
                    lineChart.notifyDataSetChanged()
                    lineChart.setVisibleXRangeMaximum(300f) // Tampilkan 300 data point terakhir
                    lineChart.moveViewToX(data.xMax) // Geser view ke data terbaru
                }
            }
        }
    }


    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)
        lineChart.setPinchZoom(true)
        lineChart.setBackgroundColor(Color.TRANSPARENT)

        val dataSets = mutableListOf<ILineDataSet>()
        for (i in 0..3) {
            dataSets.add(createSet(i))
        }
        lineChart.data = LineData(dataSets)
        lineChart.invalidate()

        // Atur legenda
        val legend = lineChart.legend
        legend.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
        legend.textColor = Color.BLACK

        // Atur sumbu X dan Y
        lineChart.xAxis.textColor = Color.BLACK
        lineChart.axisLeft.textColor = Color.BLACK
        lineChart.axisRight.isEnabled = false
    }

    private fun createSet(index: Int): LineDataSet {
        // Palet warna baru yang lebih kontras
        val (label, color) = when (index) {
            0 -> "EEG 1 (TP9)" to Color.rgb(217, 83, 79)   // Merah
            1 -> "EEG 2 (AF7)" to Color.rgb(91, 192, 222)  // Biru Muda
            2 -> "EEG 3 (AF8)" to Color.rgb(92, 184, 92)   // Hijau
            else -> "EEG 4 (TP10)" to Color.rgb(240, 173, 78) // Oranye
        }
        val set = LineDataSet(null, label)
        set.axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
        set.color = color
        set.lineWidth = 1.5f // Garis sedikit lebih tipis untuk performa

        // Menghilangkan titik/lingkaran sepenuhnya
        set.setDrawCircles(false)

        set.setDrawValues(false)
        // Menghapus area fill di bawah garis agar lebih bersih
        set.setDrawFilled(false)
        return set
    }

    // --- Menu Toolbar ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.monitoring_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_bluetooth -> {
                val intent = Intent(this, ConnectActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
