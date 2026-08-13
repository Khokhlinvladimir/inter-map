package org.osmdroid

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorEventListenerProxy(pSensorManager: SensorManager) : SensorEventListener {
    private val mSensorManager: SensorManager
    private var mListener: SensorEventListener? = null

    init {
        mSensorManager = pSensorManager
    }

    fun startListening(
        pListener: SensorEventListener?, pSensorType: Int,
        pRate: Int
    ): Boolean {
        val sensor = mSensorManager.getDefaultSensor(pSensorType)
        if (sensor == null) return false
        mListener = pListener
        return mSensorManager.registerListener(this, sensor, pRate)
    }

    fun stopListening() {
        mListener = null
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(pSensor: Sensor?, pAccuracy: Int) {
        if (mListener != null) {
            mListener!!.onAccuracyChanged(pSensor, pAccuracy)
        }
    }

    override fun onSensorChanged(pEvent: SensorEvent?) {
        if (mListener != null) {
            mListener!!.onSensorChanged(pEvent)
        }
    }
}
