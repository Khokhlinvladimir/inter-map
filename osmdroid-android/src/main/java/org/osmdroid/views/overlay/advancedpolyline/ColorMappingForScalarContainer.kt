package org.osmdroid.views.overlay.advancedpolyline

/**
 * Scalar container on top of any ColorMappingForScalar
 * Typical use:
 * * create the [ColorMappingForScalar] you need
 * * create a ColorMappingForScalarContainer on top of it
 * * add your scalars to the container
 * If you need to adjust your color mapping because you have a better idea of the actual scalar range
 * * first adjust the settings
 * (e.g. with [ColorMappingVariation.init])
 * * then call [.refresh] and the colors ([ColorMapping.getColorForIndex])
 * will reflect the new set-up
 * cf. https://github.com/osmdroid/osmdroid/issues/1551
 *
 * @author Fabrice Fontaine
 * @since 6.1.7
 */
class ColorMappingForScalarContainer(val mappingForScalar: ColorMappingForScalar) {
    private val mScalars: MutableList<Float?> = ArrayList<Float?>()
    var scalarMin: Float = Float.Companion.MAX_VALUE
        private set
    var scalarMax: Float = Float.Companion.MIN_VALUE
        private set

    fun size(): Int {
        return mScalars.size
    }

    fun add(pScalar: Float) {
        mappingForScalar.add(pScalar)
        mScalars.add(pScalar)
        if (this.scalarMin > pScalar) {
            this.scalarMin = pScalar
        }
        if (this.scalarMax < pScalar) {
            this.scalarMax = pScalar
        }
    }

    fun refresh() {
        var i = 0
        for (scalar in mScalars) {
            mappingForScalar.set(i, scalar!!)
            i++
        }
    }
}
