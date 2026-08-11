package org.osmdroid.samples

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.R
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.TilesOverlay

/**
 * @author Alex van der Linden
 */
class SampleWithTilesOverlay : AppCompatActivity() {
    private var mMapView: MapView? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup base map
        setContentView(R.layout.activity_samplewithtilesoverlay)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        val mapContainer = findViewById<LinearLayout>(R.id.map_container)

        mMapView = MapView(this)
        mMapView!!.setTilesScaledToDpi(true)
        mapContainer.addView(
            this.mMapView, RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )
        mMapView!!.getZoomController()!!.setVisibility(
            CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
        )

        //Copyright overlay
        val copyrightNotice = mMapView!!.getTileProvider()!!.getTileSource().copyrightNotice
        val copyrightOverlay = CopyrightOverlay(this)
        copyrightOverlay.setCopyrightNotice(copyrightNotice)
        mMapView!!.getOverlays()!!.add(copyrightOverlay)

        // zoom to the netherlands
        mMapView!!.controller!!.setZoom(8.0)
        mMapView!!.controller!!.setCenter(GeoPoint(53.6, 5.3))

        // Add tiles layer
        val provider = MapTileProviderBasic(getApplicationContext())
        provider.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT)
        val tilesOverlay = TilesOverlay(provider, this.getBaseContext())
        tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT)
        mMapView!!.getOverlays()!!.add(tilesOverlay)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public override fun onPause() {
        super.onPause()
        mMapView!!.onPause()
    }

    public override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }
}
