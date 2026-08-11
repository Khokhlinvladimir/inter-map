package org.osmdroid.views.overlay.advancedpolyline

/**
 * An abstract [ColorMapping] populated by scalar data
 *
 * @author Fabrice Fontaine
 * @since 6.2.0
 */
abstract class ColorMappingForScalar : ColorMapping {
    private val mColors: MutableList<Int?> = ArrayList<Int?>()

    override fun getColorForIndex(pSegmentIndex: Int): Int {
        return mColors.get(pSegmentIndex)!!
    }

    fun add(pScalar: Float) {
        mColors.add(computeColor(pScalar))
    }

    protected abstract fun computeColor(pScalar: Float): Int

    /**
     * @since 6.1.7
     */
    fun set(pIndex: Int, pScalar: Float) {
        mColors.set(pIndex, computeColor(pScalar))
    }
}
