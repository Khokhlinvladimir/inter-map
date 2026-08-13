package org.osmdroid.views.overlay.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class InternalCompassOrientationProvider(context: Context) : SensorEventListener, IOrientationProvider {
    private var mOrientationConsumer: IOrientationConsumer? = null
    private var mSensorManager: SensorManager?
    private var mAzimuth = 0f

    init {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    }

    //
    // IOrientationProvider
    //
    /**
     * Enable orientation updates from the internal compass sensor and show the compass.
     */
    override fun startOrientationProvider(orientationConsumer: IOrientationConsumer?): Boolean {
        mOrientationConsumer = orientationConsumer
        var result = false

        val sensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        if (sensor != null) {
            result = mSensorManager!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        return result
    }

    override fun stopOrientationProvider() {
        mOrientationConsumer = null
        mSensorManager!!.unregisterListener(this)
    }

    override val lastKnownOrientation: Float
        get() = mAzimuth

    override fun destroy() {
        stopOrientationProvider()
        mOrientationConsumer = null
        mSensorManager = null
    }

    //
    // SensorEventListener
    //
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // This is not interesting for us at the moment
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            if (event.values != null) {
                mAzimuth = event.values[0]
                if (mOrientationConsumer != null) mOrientationConsumer!!.onOrientationChanged(mAzimuth, this)
            }
        }
    }
}
