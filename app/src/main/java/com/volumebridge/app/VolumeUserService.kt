package com.volumebridge.app

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * IMPORTANT: This class is not started normally by Android.
 * Shizuku spawns it in its own process (running as the "shell" UID),
 * via a plain no-argument constructor, using app_process.
 * That elevated identity is what lets it read other apps' audio
 * sessions and change their volume - something a regular app process
 * is not allowed to do.
 */
class VolumeUserService : IVolumeService.Stub() {

    // A normal app gets its Context handed to it by the Android framework.
    // This process is not a "real" launched app, so we build a system-level
    // Context by hand via ActivityThread. This is a well known trick used by
    // essentially all Shizuku-based per-app-volume tools (VolumeManager, Mixer(1), etc).
    private fun systemContext(): Context {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val activityThread = activityThreadClass.getMethod("systemMain").invoke(null)
        return activityThreadClass.getMethod("getSystemContext").invoke(activityThread) as Context
    }

    override fun setAppVolume(packageName: String, volume: Float): Boolean {
        return try {
            val clamped = volume.coerceIn(0f, 1f)
            val context = systemContext()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val pm = context.packageManager

            val targetUid = try {
                pm.getPackageUid(packageName, 0)
            } catch (e: Exception) {
                return false
            }

            // Public API since Android 8 (API 26) - lists every app currently
            // playing audio on the device.
            val configs: List<AudioPlaybackConfiguration> = audioManager.activePlaybackConfigurations

            var applied = false

            for (config in configs) {
                // getClientUid() is @hide - reflection needed.
                val uid = HiddenApiBypass.invoke(
                    AudioPlaybackConfiguration::class.java,
                    config,
                    "getClientUid"
                ) as Int

                if (uid != targetUid) continue

                // getPlayerProxy() is @hide - returns an internal PlayerProxy object.
                val playerProxy = HiddenApiBypass.invoke(
                    AudioPlaybackConfiguration::class.java,
                    config,
                    "getPlayerProxy"
                ) ?: continue

                // setVolume(float) on that proxy is @hide too. This is the exact
                // mechanism MIUI's own "Adjust media sound in multiple apps" uses.
                HiddenApiBypass.invoke(
                    playerProxy.javaClass,
                    playerProxy,
                    "setVolume",
                    clamped
                )
                applied = true
            }

            applied
        } catch (e: Throwable) {
            false
        }
    }

    override fun destroy() {
        // Nothing to clean up; Shizuku manages this process's lifetime.
    }
}
