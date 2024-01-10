package org.osmdroid.views.overlay.milestones

/**
 * A milestone step is a pixel position where a milestone should be displayed with an orientation
 * Created by Fabrice Fontaine on 20/12/2017.
 *
 * @since 6.0.0
 */
class MilestoneStep @JvmOverloads constructor(
        val x: Long, val y: Long, // in degree
        val orientation: Double, val `object`: Any? = null
) {

    override fun toString(): String {
        return javaClass.simpleName + ":" + x + "," + y + "," + orientation + "," + `object`
    }
}