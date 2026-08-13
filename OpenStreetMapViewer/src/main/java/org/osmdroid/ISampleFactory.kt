package org.osmdroid

import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * Created by alex on 6/29/16.
 */
interface ISampleFactory {
    fun getSample(index: Int): BaseSampleFragment?

    fun count(): Int
}
