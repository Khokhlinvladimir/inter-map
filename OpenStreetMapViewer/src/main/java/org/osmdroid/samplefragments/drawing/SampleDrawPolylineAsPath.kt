package org.osmdroid.samplefragments.drawing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleDrawPolylineAsPath : SampleDrawPolyline() {
    override val sampleTitle: String?
        get() = "Draw a polyline on screen as Path"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = super.onCreateView(inflater, container, savedInstanceState)
        paint!!.setMode(CustomPaintingSurface.Mode.PolylineAsPath)
        return result
    }
}
