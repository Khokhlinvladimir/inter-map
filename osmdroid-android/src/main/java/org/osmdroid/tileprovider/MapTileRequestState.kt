package org.osmdroid.tileprovider

import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import java.util.Collections

class MapTileRequestState {
    private val mProviderQueue: List<MapTileModuleProviderBase?>?

    /**
     * @since 6.0.0
     */
    val mapTile: Long
    val callback: IMapTileProviderCallback?
    private var index = 0
    var currentProvider: MapTileModuleProviderBase? = null
        private set

    @Deprecated("use {@link MapTileRequestState#MapTileRequestState(long, List, IMapTileProviderCallback)}  instead")
    constructor(
        pMapTleIndex: Long,
        providers: Array<MapTileModuleProviderBase?>,
        callback: IMapTileProviderCallback?
    ) {
        mProviderQueue = ArrayList<MapTileModuleProviderBase?>()
        Collections.addAll<MapTileModuleProviderBase?>(mProviderQueue, *providers)
        this.mapTile = pMapTleIndex
        this.callback = callback
    }

    /**
     * @since 6.0
     */
    constructor(
        pMapTileIndex: Long,
        providers: List<MapTileModuleProviderBase?>?,
        callback: IMapTileProviderCallback?
    ) {
        mProviderQueue = providers
        this.mapTile = pMapTileIndex
        this.callback = callback
    }

    val isEmpty: Boolean
        get() = mProviderQueue == null || index >= mProviderQueue.size

    val nextProvider: MapTileModuleProviderBase?
        get() {
            this.currentProvider = if (this.isEmpty) null else mProviderQueue!!.get(index++)
            return this.currentProvider
        }
}
