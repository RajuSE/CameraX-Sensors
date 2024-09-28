package com.rajuse.camex_sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import androidx.lifecycle.ViewModel

class MainViewModel:ViewModel() {

    private var lastAcceleration = 0f
    private var accelerationThreshold = 8f //for fast movement detection
    fun onSensorChanged(event: SensorEvent?, onMoveFastDetect: ()->Unit){
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate the current acceleration
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val deltaAcceleration = acceleration - lastAcceleration
            lastAcceleration = acceleration

            // Check if movement exceeds the threshold
            println("deltaAcceleration:$deltaAcceleration")
            if (deltaAcceleration > accelerationThreshold) {
               onMoveFastDetect()
            }
        }
    }
}