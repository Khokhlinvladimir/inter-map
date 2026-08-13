// Created by plusminus on 10:15:51 PM - Mar 5, 2009
package org.osmdroid.mtp.util

import org.osmdroid.mtp.adt.OSMTileInfo
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

object Util {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    // ===========================================================
    // Constructors
    // ===========================================================
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
     * For a description see:
     * see [http://wiki.openstreetmap.org/index.php/Slippy_map_tilenames](http://wiki.openstreetmap.org/index.php/Slippy_map_tilenames)
     * For a code-description see:
     * see [http://wiki.openstreetmap.org/index.php/Slippy_map_tilenames#compute_bounding_box_for_tile_number](http://wiki.openstreetmap.org/index.php/Slippy_map_tilenames#compute_bounding_box_for_tile_number)
     *
     * @param aLat latitude to get the [OSMTileInfo] for.
     * @param aLon longitude to get the [OSMTileInfo] for.
     * @return The [OSMTileInfo] providing 'x' 'y' and 'z'(oom) for the coordinates passed.
     */
    fun getMapTileFromCoordinates(aLat: Double, aLon: Double, zoom: Int): OSMTileInfo {
        val y = floor((1 - ln(tan(aLat * Math.PI / 180) + 1 / cos(aLat * Math.PI / 180)) / Math.PI) / 2 * (1 shl zoom)).toInt()
        val x = floor((aLon + 180) / 360 * (1 shl zoom)).toInt()

        return OSMTileInfo(x, y, zoom)
    } // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
