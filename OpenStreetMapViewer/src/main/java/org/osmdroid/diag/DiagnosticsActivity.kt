package org.osmdroid.diag

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import org.osmdroid.R
import org.osmdroid.tileprovider.util.StorageUtils

/**
 * created on 2/6/2018.
 *
 * @author Alex O'Ree
 */
class DiagnosticsActivity : AppCompatActivity(), View.OnClickListener, LocationListener, GpsStatus.Listener {
    var output: TextView? = null
    var lm: LocationManager? = null
    var currentLocation: Location? = null
    var gpsStatus: GpsStatus? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diag)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        findViewById<View?>(R.id.diag_location).setOnClickListener(this)
        findViewById<View?>(R.id.diag_orientation).setOnClickListener(this)
        findViewById<View?>(R.id.diag_gps).setOnClickListener(this)
        findViewById<View?>(R.id.diag_permissions).setOnClickListener(this)
        findViewById<View?>(R.id.diag_storage).setOnClickListener(this)
        output = findViewById<TextView?>(R.id.diag_output)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.diag_location -> probeLocation()
            R.id.diag_orientation -> probeOrientation()
            R.id.diag_permissions -> checkPermissions()
            R.id.diag_storage -> probeStorage()
            R.id.diag_gps -> probeGps()
        }
    }

    public override fun onResume() {
        super.onResume()
        lm = getSystemService(LOCATION_SERVICE) as LocationManager?
        try {
            lm!!.addGpsStatusListener(this)
            lm!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        } catch (e: SecurityException) {
        } catch (r: RuntimeException) {
        }
    }

    public override fun onPause() {
        super.onPause()
        lm = getSystemService(LOCATION_SERVICE) as LocationManager?
        try {
            lm!!.removeUpdates(this)
            lm!!.removeGpsStatusListener(this)
        } catch (e: SecurityException) {
        } catch (r: RuntimeException) {
        }
    }

    private fun probeStorage() {
        val sb = StringBuilder()
        val storageInfos = StorageUtils.getStorageList(this)
        for (storageInfo in storageInfos) {
            sb.append(storageInfo.path).append("\n")
        }
        output!!.setText(sb.toString())
    }


    private fun probeGps() {
        val sb = StringBuilder()
        if (currentLocation != null) {
            sb.append("Current Location:\n")
            sb.append(currentLocation!!.getLatitude()).append(",").append(currentLocation!!.getLongitude()).append("\n")
            sb.append("Alt ").append(currentLocation!!.getAltitude()).append("\n")
            sb.append("Accuracy ").append(currentLocation!!.getAccuracy()).append("\n")
            sb.append("Bearing ").append(currentLocation!!.getBearing()).append("\n")
            sb.append("Speed ").append(currentLocation!!.getSpeed()).append("\n\n")
        }
        try {
            if (gpsStatus != null) {
                val iterator = gpsStatus!!.getSatellites().iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    sb.append("Sat PRN " + next.getPrn() + " Elevation " + next.getElevation() + " Azimuth " + next.getAzimuth() + "SNR " + next.getSnr())
                        .append("\n")
                }
            }
        } catch (e: Exception) {
            sb.append(e.toString())
        }
        output!!.setText(sb.toString())
    }

    private fun checkPermissions() {
        val sb = StringBuilder()
        sb.append("Fine Location Granted: ")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            sb.append("yes\n")
        } else sb.append("no\n")
        sb.append("Write External Storage: ")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            sb.append("yes\n")
        } else sb.append("no\n")
        output!!.setText(sb.toString())
    }

    private fun probeOrientation() {
        val sb = StringBuilder()
        val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensorList = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION)
        for (s in sensorList) {
            sb.append(s.getName() + ":" + s.toString() + "\n")
        }
        output!!.setText(sb.toString())
    }

    private fun probeLocation() {
        val sb = StringBuilder()

        val allProviders = lm!!.getAllProviders()
        for (s in allProviders) {
            sb.append(s).append("\n")
            val provider = lm!!.getProvider(s)
            sb.append("Name " + provider!!.getName()).append("\n")
            sb.append("Cell " + provider.requiresCell()).append("\n")
            sb.append("Network " + provider.requiresNetwork()).append("\n")
            sb.append("Satellite " + provider.requiresSatellite()).append("\n")
            sb.append("Altitude " + provider.supportsAltitude()).append("\n")
            sb.append("Bearing " + provider.supportsBearing()).append("\n")
            sb.append("Speed " + provider.supportsSpeed()).append("\n\n")
            //GpsStatus gpsStatus = lm.getGpsStatus(null);
            //gpsStatus.
        }
        output!!.setText(sb.toString())
    }

    override fun onLocationChanged(location: Location) {
        this.currentLocation = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    override fun onGpsStatusChanged(event: Int) {
        when (event) {
            GpsStatus.GPS_EVENT_SATELLITE_STATUS -> try {
                gpsStatus = lm!!.getGpsStatus(gpsStatus)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            GpsStatus.GPS_EVENT_FIRST_FIX -> {}
        }
    }
}
