package com.luntik.snake

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Lightweight haptic feedback. Sound hooks are prepared but silent until assets are added.
 */
object Feedback {
    fun vibrate(context: Context, enabled: Boolean, ms: Long = 30) {
        if (!enabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        } catch (_: Exception) {
            // ignore devices without vibrator
        }
    }

    // Placeholder for future sound assets
    fun playEat(enabled: Boolean) { /* TODO: SoundPool when assets added */ }
    fun playCrash(enabled: Boolean) { /* TODO */ }
    fun playClick(enabled: Boolean) { /* TODO */ }
}
