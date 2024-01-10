package org.osmdroid.tileprovider

import android.graphics.drawable.Drawable

interface IMapTileProviderCallback {
    /**
     * The map tile request has completed.
     *
     * @param aState    a state object
     * @param aDrawable a drawable
     */
    fun mapTileRequestCompleted(aState: MapTileRequestState?, aDrawable: Drawable?)

    /**
     * The map tile request has failed.
     *
     * @param aState a state object
     */
    fun mapTileRequestFailed(aState: MapTileRequestState?)

    /**
     * The map tile request has failed - exceeds MaxQueueSize.
     *
     * @param aState a state object
     */
    fun mapTileRequestFailedExceedsMaxQueueSize(aState: MapTileRequestState?)

    /**
     * The map tile request has produced an expired tile.
     *
     * @param aState a state object
     */
    fun mapTileRequestExpiredTile(aState: MapTileRequestState?, aDrawable: Drawable?)

    /**
     * Returns true if the network connection should be used, false if not.
     *
     * @return true if data connection should be used, false otherwise
     */
    fun useDataConnection(): Boolean
}