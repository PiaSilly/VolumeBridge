package com.volumebridge.app

import android.app.Application
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class VolumeBridgeApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Falls Shizuku schon läuft, wenn diese App startet
        Shizuku.addBinderReceivedListenerSticky {
            ShizukuVolumeManager.ensureBound()
        }

        // Falls der Nutzer die Berechtigung gerade erst erteilt
        Shizuku.addRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuVolumeManager.ensureBound()
            }
        }
    }
}
