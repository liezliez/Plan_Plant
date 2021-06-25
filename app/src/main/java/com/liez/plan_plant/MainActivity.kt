package com.liez.plan_plant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import kotlin.jvm.functions.FunctionN

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var brightness: Sensor? = null
    private lateinit var text: TextView
    private var sensorValue: Int = 0
    private lateinit var textResult: TextView
    private lateinit var textButton: TextView
    private lateinit var pb: CircularProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        text = findViewById(R.id.tv_text)
        textResult = findViewById(R.id.tv_text2)
        textButton = findViewById(R.id.button_capture)
        pb = findViewById(R.id.circularProgressBar)
        pb.apply { progressMax = 5000f }

        persiapanSensor()

        var status = true
        val buttonCapture = findViewById(R.id.button_capture) as Button
        // set on-click listener
        buttonCapture.setOnClickListener {
            // Ketika di Klik
            if (status == false) {
                status = true
                onResume()
                textButton.text = "Sensor On"
            } else {
                onPause()
                status = false
                textButton.text = "Sensor Off"
            }
        }
    }

    private fun persiapanSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        brightness = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    // Deklarasi Variabel untuk sensorChanged

    private var i: Int = 0
    private var banyakSample: Int = 4
    private var oldInput: Float = 0F
    private var avgNow: Float = 0F
    private var sensorDibulatkan: Double = 0.0

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val light1 = event.values[0]
            // MVA
            i++
            oldInput = oldInput + light1

            // Simpel Logic untuk MVA pada setiap 5 buah sample, lalu update Text pada aplikasi

            if ((i % banyakSample) == 0) {

                // Menghitung nilai sensor dengan metode MVA
                avgNow = average(oldInput, banyakSample)

                // Ganti Value Text
                text.text = "Sensor: ${bulatkan(avgNow)} lux\n${brightness(avgNow)}"
                textResult.text = category(avgNow)
                pb.setProgressWithAnimation(avgNow)

                // Send Data ke Firebase
                sensorDibulatkan = bulatkan(avgNow).toDouble()
                sendData(sensorDibulatkan)

                // reset sehabis 5 input
                oldInput = 0F
            }
        }
    }

    // Pembulatan nilai Sensor

    private fun bulatkan(angka: Float): Float {
        val number: Float = angka
        val number3digits: Double = String.format("%.3f", number).toDouble()
        val number2digits: Double = String.format("%.2f", number3digits).toDouble()
        val solution: Double = String.format("%.1f", number2digits).toDouble()
        return solution.toFloat()
    }

    private fun average(oldInput: Float, banyakSample: Int): Float {
        return oldInput / banyakSample
    }

    // Klasifikasi Result berdasarkan Lux Sensor Cahaya

    private fun brightness(brightness: Float): String {

        return when (brightness.toInt()) {
            0 -> "Sangat Gelap"
            in 1..10 -> "Gelap"
            in 11..50 -> "Lumayan Gelap"
            in 51..5000 -> "Normal"
            in 5001..25000 -> "Sangat Terang"
            else -> "Terlalu Terang"
        }
    }

    private fun category(brightness: Float): String {

        return when (brightness.toInt()) {
            0 -> "Tanaman tanpa klorofil bisa tumbuh disini"
            in 1..10 -> "Jamur bisa tumbuh disini"
            in 11..50 -> "Tanaman kecil bisa tumbuh disini"
            in 51..5000 -> "Tanaman Hias dan Sayuran bisa tumbuh disini"
            in 5001..25000 -> "Pepohonan bisa tumbuh disini"
            else -> "Tanaman bisa kering jika ditanam disini"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onResume() {
        super.onResume()
        // Register a listener for the sensor.
        sensorManager.registerListener(this, brightness, 1000000, 1000000)
    }


    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // Kirim Data Ke Firebase

    private fun sendData(nilaiSnsr: Double) {
        // Deklarasi Referensi Database Firebase
        val ref = FirebaseDatabase.getInstance().getReference("Nilai Sensor")

        // make object using the data
        val snsr = SensorCahaya(nilaiSnsr)

        // Send the Data
        ref.setValue(snsr).addOnCompleteListener{}
    }

}