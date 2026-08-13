package org.osmdroid

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

/**
 * created on 1/14/2017.
 *
 * @author Alex O'Ree
 */
class LicenseActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    var license: TextView? = null
    var values: Array<String> = arrayOf(
        "osmdroid", "geopackage",
        "mapsforge", "ACRA", "leakcanary", "ormlite", "pngj"
    )

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        val spinner = findViewById<Spinner>(R.id.license_module_spinner)
        val array = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)
        array.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.setAdapter(array)
        license = findViewById<TextView>(org.osmdroid.R.id.license_body)
        spinner.setOnItemSelectedListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> license!!.setText(org.osmdroid.R.string.license_osmdroid)
            1 -> license!!.setText(org.osmdroid.R.string.license_geopackage)
            2 -> license!!.setText(org.osmdroid.R.string.license_mapsforge)
            3 -> license!!.setText(org.osmdroid.R.string.license_acra)
            4 -> license!!.setText(org.osmdroid.R.string.license_leakcanary)
            5 -> license!!.setText(org.osmdroid.R.string.license_ormlite)
            6 -> license!!.setText(org.osmdroid.R.string.license_pngj)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
