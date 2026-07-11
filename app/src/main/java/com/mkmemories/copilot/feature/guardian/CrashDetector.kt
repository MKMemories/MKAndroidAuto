package com.mkmemories.copilot.feature.guardian

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Pack Ange gardien — détection d'accident, 100 % on-device.
 *
 * Surveille l'accélération linéaire : une décélération brutale au-delà de
 * [CRASH_THRESHOLD_MS2] (~6 g, caractéristique d'un choc, inatteignable en
 * conduite normale) déclenche [onPossibleCrash], qui lance l'escalade SOS
 * ("Tout va bien ?" + compte à rebours, voir [SosManager]).
 */
class CrashDetector(
    context: Context,
    private val onPossibleCrash: (accelerationMs2: Float) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude >= CRASH_THRESHOLD_MS2) {
            onPossibleCrash(magnitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        // TODO v1.1 : affiner avec la vitesse GPS (un choc à l'arrêt = téléphone tombé,
        // pas un accident) et une fenêtre glissante pour éviter les faux positifs.
        const val CRASH_THRESHOLD_MS2 = 60f
    }
}
