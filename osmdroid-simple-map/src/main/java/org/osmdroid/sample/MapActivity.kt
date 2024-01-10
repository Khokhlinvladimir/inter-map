package org.osmdroid.sample;

import android.os.Bundle;
import android.preference.PreferenceManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.simplemap.R;
import org.osmdroid.views.MapView;

import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends  AppCompatActivity {
    private MapView mapView = null;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        //TODO check permissions
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
    }

    @Override
    public void onResume() {
        super.onResume();
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (mapView != null) {
            mapView.onResume();
            mapView.setMultiTouchControls(true);
            mapView.setMapOrientation(0, true);

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Configuration.getInstance().save(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
