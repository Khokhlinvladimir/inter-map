package org.osmdroid.samplefragments.tileproviders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.Companion.getTileSystem

/**
 * test for showing the map for different repetition modes
 * https://github.com/osmdroid/osmdroid/issues/183
 * Created by Maradox on 11/26/17.
 */
class SampleAssetsOnlyRepetitionModes : BaseSampleFragment() {
    var horizontalCb: CheckBox? = null
    var verticalCb: CheckBox? = null
    var limitBoundsCb: CheckBox? = null

    override val sampleTitle: String
        get() = "Assets Only With Repetition Modes"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.layout_wrapping, null)
        horizontalCb = v.findViewById<CheckBox>(R.id.horizontalRepetitionCb)
        verticalCb = v.findViewById<CheckBox>(R.id.verticalRepetitionCb)
        limitBoundsCb = v.findViewById<CheckBox>(R.id.limitBoundsCb)
        mMapView = v.findViewById<MapView?>(R.id.mapview)

        horizontalCb!!.setChecked(true)
        verticalCb!!.setChecked(true)
        limitBoundsCb!!.setChecked(false)

        horizontalCb!!.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                mMapView!!.setHorizontalMapRepetitionEnabled(isChecked)
            }
        })

        verticalCb!!.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                mMapView!!.setVerticalMapRepetitionEnabled(isChecked)
            }
        })

        limitBoundsCb!!.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked) {
                    mMapView!!.setScrollableAreaLimitDouble(
                        BoundingBox(
                            getTileSystem().getMaxLatitude(), getTileSystem().getMaxLongitude(),
                            getTileSystem().getMinLatitude(), getTileSystem().getMinLongitude()
                        )
                    )
                } else {
                    mMapView!!.setScrollableAreaLimitDouble(null)
                }
            }
        })

        return v
    }
}
