// Created by plusminus on 20:36:01 - 26.09.2008
package org.osmdroid.views.util

import org.osmdroid.util.MyMath

/**
 * @author Nicolas Gramlich
 */
@Deprecated("Use {@link org.osmdroid.util.MyMath} instead")
object MyMath {
    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    /**
     * Calculates i.e. the increase of zoomlevel needed when the visible latitude needs to be bigger
     * by `factor`.
     *
     *
     * Assert.assertEquals(1, getNextSquareNumberAbove(1.1f)); Assert.assertEquals(2,
     * getNextSquareNumberAbove(2.1f)); Assert.assertEquals(2, getNextSquareNumberAbove(3.9f));
     * Assert.assertEquals(3, getNextSquareNumberAbove(4.1f)); Assert.assertEquals(3,
     * getNextSquareNumberAbove(7.9f)); Assert.assertEquals(4, getNextSquareNumberAbove(8.1f));
     * Assert.assertEquals(5, getNextSquareNumberAbove(16.1f));
     *
     *
     * Assert.assertEquals(-1, - getNextSquareNumberAbove(1 / 0.4f) + 1); Assert.assertEquals(-2, -
     * getNextSquareNumberAbove(1 / 0.24f) + 1);
     *
     * @param factor
     * @return
     */
    @Deprecated("Use {@link org.osmdroid.util.MyMath#getNextSquareNumberAbove(float)} instead")
    fun getNextSquareNumberAbove(factor: Float): Int {
        return MyMath.getNextSquareNumberAbove(factor)
    } // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
