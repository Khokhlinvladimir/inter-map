package org.osmdroid.samplefragments.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.String
import kotlin.arrayOf
import kotlin.floatArrayOf

/**
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleAnimateToWithOrientation : BaseSampleFragment(), View.OnClickListener {
    private val MAP_CENTER = GeoPoint(0.0, 0.0)
    private var mIndex = -1
    private var mLabel: String? = null

    override val sampleTitle: String
        get() = "Animate To With Orientation"

    private var btnCache: Button? = null

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        next()

        /*        final RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(mMapView);
        rotationGestureOverlay.setEnabled(true);
        mMapView.getOverlays().add(rotationGestureOverlay);
*/
        return root
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnCache -> {
                mMapView!!.controller!!.animateTo(MAP_CENTER, null, null, ORIENTATIONS[mIndex], CLOCKWISES[mIndex])
                next()
            }
        }
    }

    private fun next() {
        mIndex++
        mIndex %= ORIENTATIONS.size
        mLabel = "To " + ORIENTATIONS[mIndex] + " " +
                (if (CLOCKWISES[mIndex] == null) "" else if (CLOCKWISES[mIndex] == true) "clockwise" else "anticlockwise")
        btnCache!!.setText(mLabel)
    }

    companion object {
        private val ORIENTATIONS = floatArrayOf(30f, 0f, -30f, 0f, -30f, 0f)
        private val CLOCKWISES = arrayOf<Boolean?>(null, null, null, null, true, false)
    }
}
