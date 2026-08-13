package org.osmdroid.views.overlay


/**
 * @author Fabrice Fontaine
 * @since 6.0.0
 */
object OverlayLayoutParams {
    const val LEFT: Int = 1
    const val RIGHT: Int = 2
    const val CENTER_HORIZONTAL: Int = 4
    const val TOP: Int = 8
    const val BOTTOM: Int = 16
    const val CENTER_VERTICAL: Int = 32

    fun getMaskedValue(pValue: Int, pDefault: Int, pMasks: IntArray): Int {
        for (mask in pMasks) {
            if ((pValue and mask) == mask) {
                return mask
            }
        }
        return pDefault
    }
}
