package org.osmdroid.tileprovider.modules

/**
 * @author Fabrice Fontaine
 * Used to be embedded in MapTileModuleProviderBase
 *
 *
 * Thrown by a tile provider module in TileLoader.loadTile() to signal that it can no longer
 * function properly. This will typically clear the pending queue.
 * @since 6.0.2
 */
class CantContinueException : Exception {
    constructor(pDetailMessage: String?) : super(pDetailMessage)

    constructor(pThrowable: Throwable?) : super(pThrowable)

    companion object {
        private const val serialVersionUID = 146526524087765133L
    }
}
