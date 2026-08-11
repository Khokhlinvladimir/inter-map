package org.osmdroid.samplefragments.milstd2525

import android.graphics.drawable.BitmapDrawable
import android.util.SparseArray
import android.view.MotionEvent
import armyc2.c2sd.renderer.MilStdIconRenderer
import armyc2.c2sd.renderer.utilities.MilStdAttributes
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

/**
 * A super simple overlay to plot a marker when the user long presses on the map.
 *
 *
 * It does not draw anything on screen but does intercept long press events then adds
 * a hardcoded Marker to the map
 * created on 11/19/2017.
 *
 * @author Alex O'Ree
 * @since 6.0.0
 */
class MilStdPointPlottingOverlay : Overlay() {
    @JvmField
    var def: SimpleSymbol? = null

    fun setSymbol(def: SimpleSymbol?) {
        this.def = def
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        if (def != null) {
            val pt = mapView.projection.fromPixels(e.getX().toInt(), e.getY().toInt(), null) as GeoPoint

            //just in case the point is off the map, let's fix the coordinates
            if (pt.longitude < -180) pt.setLongitude(pt.longitude + 360)
            if (pt.longitude > 180) pt.setLongitude(pt.longitude - 360)
            //latitude is a bit harder. see https://en.wikipedia.org/wiki/Mercator_projection
            if (pt.latitude > getTileSystem().getMaxLatitude()) pt.setLatitude(getTileSystem().getMaxLatitude())
            if (pt.latitude < getTileSystem().getMinLatitude()) pt.setLatitude(getTileSystem().getMinLatitude())

            val code = requireNotNull(def!!.symbolCode).replace("*", "-")
            //TODO if (!def.isMultiPoint())
            run {
                val size = 128
                val attr = SparseArray<String?>()
                attr.put(MilStdAttributes.PixelSize, size.toString() + "")

                val ii = MilStdIconRenderer.getInstance().RenderIcon(code, def!!.modifiers, attr)
                val m = Marker(mapView)
                m.setPosition(pt)
                m.setTitle(code)
                m.setSnippet(def!!.description + "\n" + def!!.hierarchy)
                m.setSubDescription(def!!.path + "\n" + m.getPosition().latitude + "," + m.getPosition().longitude)
                if (ii != null && ii.getImage() != null) {
                    val d = BitmapDrawable(ii.getImage())
                    m.setImage(d)
                    m.setIcon(d)

                    val centerX = ii.getCenterPoint().x //pixel center position
                    //calculate what percentage of the center this value is
                    val realCenterX = centerX.toFloat() / ii.getImage().getWidth().toFloat()

                    val centerY = ii.getCenterPoint().y
                    val realCenterY = centerY.toFloat() / ii.getImage().getHeight().toFloat()
                    m.setAnchor(realCenterX, realCenterY)


                    mapView.getOverlayManager().add(m)
                    mapView.invalidate()
                }
            }

            return true
        }
        return false
    }
}
