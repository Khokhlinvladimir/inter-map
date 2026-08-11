package org.osmdroid.tileprovider.tilesource

/**
 * this is an extremely simple tile source that should only be used for offline sources. assumes that the file name matches the source name
 *
 * @author alex
 * @see OfflineTileProvider
 */
class FileBasedTileSource(
    aName: String?,
    aZoomMinLevel: Int,
    aZoomMaxLevel: Int,
    aTileSizePixels: Int,
    aImageFilenameEnding: String?,
    aBaseUrl: Array<out String?>?
) : XYTileSource(aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl) {
    companion object {
        fun getSource(name: String): ITileSource {
            var name = name
            if (name.contains(".")) {
                name = name.substring(0, name.indexOf("."))
            }
            return FileBasedTileSource(
                name,
                0, 18, 256, ".png", arrayOf<String>("http://localhost")
            )
        }
    }
}
