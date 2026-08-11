package org.osmdroid.samplefragments.tileproviders

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.TileStates
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * Demo of the new tile states feature:
 * - how many tiles are currently being displayed
 * - how many tiles in which state? [U: up to date, E: expired, S: scaled, N: not found]
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleTileStates : BaseSampleFragment() {
    private var mTextView: TextView? = null
    private var mTileStates: TileStates? = null
    private var mOk = false

    override val sampleTitle: String
        get() = "Tile States"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox, container, false)
        mMapView = root.findViewById<MapView?>(R.id.mapview)
        mTextView = root.findViewById<TextView>(R.id.textViewCurrentLocation)
        mTileStates = mMapView!!.getMapOverlay()!!.getTileStates()
        return root
    }

    override fun addOverlays() {
        super.addOverlays()

        val ok = (getResources().getDrawable(R.drawable.baseline_done_outline_black_36) as BitmapDrawable).getBitmap()
        val ko = (getResources().getDrawable(R.drawable.twotone_warning_black_36) as BitmapDrawable).getBitmap()
        mMapView!!.getOverlayManager().add(object : Overlay() {
            override fun draw(c: Canvas, projection: Projection?) {
                val bitmap = if (mOk) ok else ko
                c.drawBitmap(
                    bitmap,
                    (c.getWidth() / 2 - bitmap.getWidth() / 2).toFloat(),
                    (c.getHeight() / 2 - bitmap.getHeight() / 2).toFloat(),
                    null
                )
            }
        })
        mMapView!!.getMapOverlay()!!.getTileStates().getRunAfters().add(object : Runnable {
            override fun run() {
                mTextView!!.setText(mTileStates.toString())
                mOk = mTileStates!!.isDone() && mTileStates!!.getTotal() == mTileStates!!.getUpToDate()
            }
        })
    }
}
