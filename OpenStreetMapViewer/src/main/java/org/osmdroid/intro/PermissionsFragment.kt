package org.osmdroid.intro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.R
import org.osmdroid.config.Configuration.instance

/**
 * created on 1/5/2017.
 *
 * @author Alex O'Ree
 */
class PermissionsFragment : Fragment(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.intro_permissions, container, false)
        if (Build.VERSION.SDK_INT >= 23 && needsPermissions()) {
            v.findViewById<View?>(R.id.askPermissions).setOnClickListener(this)
            v.findViewById<View?>(R.id.askPermissions).setVisibility(View.VISIBLE)
        } else {
            v.findViewById<View?>(R.id.askPermissions).setVisibility(View.GONE)
        }

        return v
    }


    override fun onClick(v: View?) {
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions()
        } else {
            instance!!.load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        }
    }


    // START PERMISSION CHECK
    private val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124

    private fun needsPermissions(): Boolean {
        val permissions: MutableList<String?> = ArrayList<String?>()
        if (ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.isEmpty()) {
            return false
        } // else: We already have permissions, so handle as normal

        return true
    }

    private fun checkPermissions() {
        val permissions: MutableList<String?> = ArrayList<String?>()
        var message = "osmdroid permissions:"
        if (ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            message += "\nLocation to show user location."
        }
        if (ContextCompat.checkSelfPermission(getContext()!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            message += "\nStorage access to store map tiles."
        }
        if (!permissions.isEmpty()) {
            //Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            val params = permissions.toTypedArray<String?>()
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS)
        } // else: We already have permissions, so handle as normal
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<String?, Int?> = HashMap<String?, Int?>()
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED)
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED)
                // Fill with results
                var i = 0
                while (i < permissions.size) {
                    perms.put(permissions[i], grantResults[i])
                    i++
                }
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                val location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (location && storage) {
                    // All Permissions Granted
                    //Toast.makeText(getContext(), "All permissions granted", Toast.LENGTH_SHORT).show();
                    Snackbar.make(getView()!!, "All permissions granted", Snackbar.LENGTH_LONG).show()
                } else if (storage) {
                    Toast.makeText(
                        getContext(),
                        "Storage permission is required to store map tiles to reduce data usage and for offline usage.",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (location) {
                    Toast.makeText(getContext(), "Location permission is required to show the user's location on map.", Toast.LENGTH_LONG).show()
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(
                        getContext(), "Storage permission is required to store map tiles to reduce data usage and for offline usage." +
                                "\nLocation permission is required to show the user's location on map.", Toast.LENGTH_SHORT
                    ).show()
                }
                instance!!.load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
