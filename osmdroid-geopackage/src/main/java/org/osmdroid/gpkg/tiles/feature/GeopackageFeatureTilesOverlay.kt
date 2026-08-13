package org.osmdroid.gpkg.tiles.feature

import android.content.Context
import android.util.Log
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.GeoPackageManager
import mil.nga.geopackage.features.index.FeatureIndexManager
import mil.nga.geopackage.features.index.FeatureIndexType
import mil.nga.geopackage.features.user.FeatureDao
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles
import mil.nga.geopackage.tiles.features.FeatureTiles
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * created on 8/19/2017.
 *
 * @author Alex O'Ree
 */
class GeopackageFeatureTilesOverlay(provider: GeoPackageFeatureTileProvider, pContext: Context) : TilesOverlay(provider, pContext) {
    protected var manager: GeoPackageManager
    protected var ctx: Context
    var databases: MutableList<String?>?
        protected set
    protected var provider: GeoPackageFeatureTileProvider
    protected var geoPackage: GeoPackage? = null
    protected var featureDao: FeatureDao? = null
    protected var featureTiles: FeatureTiles? = null

    init {
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues")
        this.ctx = pContext

        this.provider = provider
        // Get a manager
        manager = GeoPackageFactory.getManager(pContext)

        // Available databases
        databases = manager.databases()
    }

    @Throws(Exception::class)
    fun getFeatureTable(database: String?): MutableList<String?>? {
        var open: GeoPackage? = null
        var featureTables: MutableList<String?>? = ArrayList<String?>()
        try {
            open = manager.open(database)
            featureTables = open.getFeatureTables()
        } catch (ex: Exception) {
            throw ex
        } finally {
            if (open != null) open.close()
        }

        return featureTables
    }


    fun setDatabaseAndFeatureTable(database: String?, featureTable: String?) {
        if (featureDao != null) featureDao = null
        if (geoPackage != null) {
            geoPackage!!.close()
            geoPackage = null
        }
        geoPackage = manager.open(database)
        val featureDao = geoPackage!!.getFeatureDao(featureTable)


        // Index Features
        val indexer = FeatureIndexManager(ctx, geoPackage, featureDao)
        indexer.setIndexLocation(FeatureIndexType.GEOPACKAGE)
        val indexedCount = indexer.index()
        // Draw tiles from features
        featureTiles = DefaultFeatureTiles(ctx, featureDao)
        featureTiles!!.setMaxFeaturesPerTile(1000) // Set max features to draw per tile
        val numberFeaturesTile = NumberFeaturesTile(ctx) // Custom feature tile implementation
        featureTiles!!.setMaxFeaturesTileDraw(numberFeaturesTile) // Draw feature count tiles when max features passed
        featureTiles!!.setIndexManager(indexer) // Set index manager to query feature indices

        provider.set(featureDao.getZoomLevel(), featureTiles)
    }


    override fun onDetach(pMapView: MapView?) {
        super.onDetach(pMapView)
        if (geoPackage != null) {
            geoPackage!!.close()
            geoPackage = null
        }

        featureDao = null
        featureTiles = null
    }
}
