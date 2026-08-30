package com.volumebridge.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val grantButton = findViewById<Button>(R.id.grantButton)

        grantButton.setOnClickListener {
            when {
                !Shizuku.pingBinder() ->
                    statusText.text = "Shizuku läuft nicht. Bitte zuerst Shizuku starten."
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                    statusText.text = "Berechtigung bereits erteilt."
                    ShizukuVolumeManager.ensureBound()
                }
                else -> Shizuku.requestPermission(1)
            }
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        statusText.text = when {
            !Shizuku.pingBinder() -> "Status: Shizuku läuft nicht"
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                "Status: Shizuku läuft, Berechtigung fehlt noch"
            ShizukuVolumeManager.isReady() -> "Status: Bereit ✔\n\nBroadcast-Action:\ncom.volumebridge.app.SET_VOLUME\n\nExtras:\npackage (String)\nvolume (Float, 0.0-1.0)"
            else -> "Status: Shizuku bereit, verbinde Volume-Service…"
        }

        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuVolumeManager.ensureBound()
        }
    }
}
