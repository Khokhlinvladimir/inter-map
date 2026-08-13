package org.osmdroid.samples

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.R
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.TilesOverlay

/**
 * @author Alex van der Linden
 */
class SampleWithTilesOverlayAndCustomTileSource : AppCompatActivity() {
    private var mMapView: MapView? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup base map
        setContentView(R.layout.activity_samplewithtilesoverlayandcustomtilesource)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        val mapContainer = findViewById<LinearLayout>(R.id.map_container)

        mMapView = MapView(this)
        mMapView!!.setTilesScaledToDpi(true)

        //Copyright overlay
        val copyrightNotice = mMapView!!.getTileProvider()!!.getTileSource()!!.copyrightNotice
        val copyrightOverlay = CopyrightOverlay(this)
        copyrightOverlay.setCopyrightNotice(copyrightNotice)
        mMapView!!.getOverlays()!!.add(copyrightOverlay)

        mapContainer.addView(
            mMapView, RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )
        mMapView!!.getZoomController()!!.setVisibility(
            CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
        )

        // zoom to the netherlands
        mMapView!!.controller!!.setZoom(7.0)
        mMapView!!.controller!!.setCenter(GeoPoint(51.5, 5.4))

        // Add tiles layer with custom tile source
        val tileProvider = MapTileProviderBasic(getApplicationContext())
        val tileSource: ITileSource = XYTileSource(
            "FietsRegionaal", 3, 18, 256, ".png",
            arrayOf<String>("http://overlay.openstreetmap.nl/openfietskaart-rcn/")
        )
        tileProvider.setTileSource(tileSource)
        tileProvider.tileRequestCompleteHandlers.add(mMapView!!.getTileRequestCompleteHandler())
        val tilesOverlay = TilesOverlay(tileProvider, this.getBaseContext())
        tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
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
