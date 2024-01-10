package org.osmdroid.views

import android.view.View

interface OnFirstLayoutListener {
    /**
     * this generally means that the map is ready to go
     *
     * @param v
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int)
}