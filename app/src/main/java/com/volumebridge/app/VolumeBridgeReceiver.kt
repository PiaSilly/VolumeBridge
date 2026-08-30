package com.volumebridge.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * MacroDroid Action: "Send Intent"
 *   Intent Action:  com.volumebridge.app.SET_VOLUME
 *   Extra "package" (String) -> z.B. com.google.android.youtube
 *   Extra "volume"  (Float)  -> 0.0 (stumm) bis 1.0 (normal)
 */
class VolumeBridgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("package")
        val volume = intent.getFloatExtra("volume", -1f)

        if (packageName.isNullOrBlank() || volume < 0f) return

        // BroadcastReceiver.onReceive muss schnell zurückkehren; da das Binden
        // an Shizuku und der eigentliche Aufruf asynchron ablaufen, halten wir
        // den Receiver mit goAsync() kurz am Leben und probieren es notfalls
        // ein paar Mal, falls der Service gerade erst verbindet.
        val pending = goAsync()

        ShizukuVolumeManager.ensureBound()

        Thread {
            var success = false
            var attempts = 0
            while (attempts < 15 && !success) {
                success = ShizukuVolumeManager.setAppVolume(packageName, volume)
                if (!success) {
                    Thread.sleep(150)
                }
                attempts++
            }
            pending.finish()
        }.start()
    }
}
