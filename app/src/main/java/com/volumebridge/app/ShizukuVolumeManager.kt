package com.volumebridge.app

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

object ShizukuVolumeManager {

    @Volatile
    private var service: IVolumeService? = null

    @Volatile
    private var binding = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            "com.volumebridge.app",
            VolumeUserService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("volumeservice")
        .debuggable(false)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = if (binder != null && binder.pingBinder()) {
                IVolumeService.Stub.asInterface(binder)
            } else {
                null
            }
            binding = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun ensureBound() {
        if (service != null || binding) return
        if (!Shizuku.pingBinder()) return
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return

        binding = true
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (e: Exception) {
            binding = false
        }
    }

    fun isReady(): Boolean = service != null

    /**
     * @param packageName z.B. "com.google.android.youtube"
     * @param volume 0.0 (stumm) bis 1.0 (normale Lautstärke)
     * @return true wenn die App gerade aktiv Audio abspielt und die Lautstärke gesetzt wurde
     */
    fun setAppVolume(packageName: String, volume: Float): Boolean {
        val s = service ?: return false
        return try {
            s.setAppVolume(packageName, volume)
        } catch (e: Exception) {
            false
        }
    }
}
