package org.osmdroid.views.overlay.simplefastpoint

import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay.PointAdapter

/**
 * This class is just a simple wrapper for a List of [IGeoPoint]s to be used in
 * [SimpleFastPointOverlay]. Can be used for unlabelled or labelled GeoPoints.
 * Use the simple constructor, or otherwise be sure to set the labelled and styled parameters of the
 * constructor to match the kind of points.
 * More complex cases should implement [SimpleFastPointOverlay.PointAdapter], not extend this
 * one. This is a simple example on how to implement an adapter for any case.
 * Created by Miguel Porto on 26-10-2016.
 */
open class SimplePointTheme @JvmOverloads constructor(
    private val mPoints: MutableList<IGeoPoint?>,
    private val mLabelled: Boolean = mPoints.size != 0 && mPoints.get(0) is LabelledGeoPoint,
    private val mStyled: Boolean = mPoints.size != 0 && mPoints.get(0) is StyledLabelledGeoPoint
) : PointAdapter {
    override fun size(): Int {
        return mPoints.size
    }

    override fun get(i: Int): IGeoPoint? {
        return mPoints.get(i)
    }

    override val isLabelled: Boolean
        get() = mLabelled

    override val isStyled: Boolean
        get() = mStyled

    /**
     * NOTE: this iterator will be called very frequently, avoid complicated code.
     *
     * @return
     */
    override fun iterator(): MutableIterator<IGeoPoint?> {
        return mPoints.iterator()
    }
}
