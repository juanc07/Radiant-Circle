package com.thinkblox.radiantrush.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/** Registers the accelerometer only while the Circle screen is visible. */
@Composable
fun ShakeToDiscoverEffect(
    enabled: Boolean,
    onShake: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnShake = rememberUpdatedState(onShake)

    DisposableEffect(context, enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose { }
        }

        var lastTriggerAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrElse(0) { 0f }
                val y = event.values.getOrElse(1) { 0f }
                val z = event.values.getOrElse(2) { 0f }
                val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                val now = SystemClock.elapsedRealtime()
                if (gForce >= SHAKE_G_FORCE && now - lastTriggerAt >= SHAKE_COOLDOWN_MILLIS) {
                    lastTriggerAt = now
                    currentOnShake.value.invoke()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME,
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

private const val SHAKE_G_FORCE = 2.35
private const val SHAKE_COOLDOWN_MILLIS = 1_800L
