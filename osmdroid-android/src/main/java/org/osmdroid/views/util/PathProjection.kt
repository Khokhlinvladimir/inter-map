package org.osmdroid.views.util

import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.PointL
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.Projection

@Deprecated("Use {@link Polyline} or {@link Polygon} instead")
open class PathProjection {
    companion object {
    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun toPixels(
        projection: Projection, `in`: MutableList<out GeoPoint>,
        reuse: Path?, doGudermann: Boolean = true
    ): Path {
        require(`in`.size >= 2) { "List of GeoPoints needs to be at least 2." }

        val out = if (reuse != null) reuse else Path()
        out.incReserve(`in`.size)

        val tileSystem = getTileSystem()
        var first = true
        for (gp in `in`) {
            val underGeopointTileCoords = Point()
            val mapSize: Double = TileSystem.Companion.MapSize(projection.zoomLevel)
            val mercator = tileSystem.getMercatorFromGeo(
                gp.latitude, gp.longitude, mapSize,
                null, true
            )
            underGeopointTileCoords.x = projection.getTileFromMercator(mercator.x)
            underGeopointTileCoords.y = projection.getTileFromMercator(mercator.y)

            /*
             * Calculate the Latitude/Longitude on the left-upper ScreenCoords of the MapTile.
             */
            val upperRight = PointL(
                projection.getMercatorFromTile(underGeopointTileCoords.x),
                projection.getMercatorFromTile(underGeopointTileCoords.y)
            )
            val lowerLeft = PointL(
                projection.getMercatorFromTile(underGeopointTileCoords.x + TileSystem.tileSize),
                projection.getMercatorFromTile(underGeopointTileCoords.y + TileSystem.tileSize)
            )
            val neGeoPoint = tileSystem.getGeoFromMercator(upperRight.x, upperRight.y, mapSize, null, true, true)
            val swGeoPoint = tileSystem.getGeoFromMercator(lowerLeft.x, lowerLeft.y, mapSize, null, true, true)
            val bb = BoundingBox(
                neGeoPoint.latitude,
                neGeoPoint.longitude, swGeoPoint.latitude,
                swGeoPoint.longitude
            )

            val relativePositionInCenterMapTile: PointF
            if (doGudermann && (projection.zoomLevel < 7)) {
                relativePositionInCenterMapTile = bb
                    .getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(
                        gp.latitude, gp.longitude, null
                    )
            } else {
                relativePositionInCenterMapTile = bb
                    .getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
                        gp.latitude, gp.longitude, null
                    )
            }

            val screenRect = projection.screenRect
            val centerMapTileCoords = Point(
                projection.getTileFromMercator(screenRect.centerX().toLong()),
                projection.getTileFromMercator(screenRect.centerY().toLong())
            )
            val upperLeftCornerOfCenterMapTile = PointL(
                projection.getMercatorFromTile(centerMapTileCoords.x),
                projection.getMercatorFromTile(centerMapTileCoords.y)
            )
            val tileDiffX = centerMapTileCoords.x - underGeopointTileCoords.x
            val tileDiffY = centerMapTileCoords.y - underGeopointTileCoords.y
            val underGeopointTileScreenLeft: Long = (upperLeftCornerOfCenterMapTile.x
                    - (TileSystem.tileSize.toLong() * tileDiffX))
            val underGeopointTileScreenTop: Long = (upperLeftCornerOfCenterMapTile.y
                    - (TileSystem.tileSize.toLong() * tileDiffY))

            val x = (underGeopointTileScreenLeft
                    + (relativePositionInCenterMapTile.x * TileSystem.tileSize).toLong())
            val y = (underGeopointTileScreenTop
                    + (relativePositionInCenterMapTile.y * TileSystem.tileSize).toLong())

            /* Add up the offset caused by touch. */
            if (first) {
                out.moveTo(x.toFloat(), y.toFloat())
                // out.moveTo(x + MapView.this.mTouchMapOffsetX, y +
                // MapView.this.mTouchMapOffsetY);
            } else {
                out.lineTo(x.toFloat(), y.toFloat())
            }
            first = false
        }

        return out
    }
    }
}
