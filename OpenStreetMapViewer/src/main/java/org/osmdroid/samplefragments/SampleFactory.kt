package org.osmdroid.samplefragments

import android.os.Build
import org.osmdroid.ISampleFactory
import org.osmdroid.samplefragments.animations.AnimatedMarkerHandler
import org.osmdroid.samplefragments.animations.AnimatedMarkerTimer
import org.osmdroid.samplefragments.animations.AnimatedMarkerTypeEvaluator
import org.osmdroid.samplefragments.animations.AnimatedMarkerValueAnimator
import org.osmdroid.samplefragments.animations.FastZoomSpeedAnimations
import org.osmdroid.samplefragments.animations.MinMaxZoomLevel
import org.osmdroid.samplefragments.bookmarks.BookmarkSample
import org.osmdroid.samplefragments.cache.CacheImport
import org.osmdroid.samplefragments.cache.CachePurge
import org.osmdroid.samplefragments.cache.SampleAlternateCacheDir
import org.osmdroid.samplefragments.cache.SampleCacheDelete
import org.osmdroid.samplefragments.cache.SampleCacheDownloader
import org.osmdroid.samplefragments.cache.SampleCacheDownloaderArchive
import org.osmdroid.samplefragments.cache.SampleCacheDownloaderCustomUI
import org.osmdroid.samplefragments.cache.SampleJumboCache
import org.osmdroid.samplefragments.cache.SampleSqliteOnly
import org.osmdroid.samplefragments.data.AsyncTaskDemoFragment
import org.osmdroid.samplefragments.data.Gridlines2
import org.osmdroid.samplefragments.data.HeatMap
import org.osmdroid.samplefragments.data.SampleGridlines
import org.osmdroid.samplefragments.data.SampleIISTracker
import org.osmdroid.samplefragments.data.SampleIISTrackerMotionTrails
import org.osmdroid.samplefragments.data.SampleItemizedOverlayMultiClick
import org.osmdroid.samplefragments.data.SampleMapSnapshot
import org.osmdroid.samplefragments.data.SampleMarker
import org.osmdroid.samplefragments.data.SampleMarkerMultiClick
import org.osmdroid.samplefragments.data.SampleMilestonesNonRepetitive
import org.osmdroid.samplefragments.data.SampleMilitaryIconsItemizedIcons
import org.osmdroid.samplefragments.data.SampleMilitaryIconsMarker
import org.osmdroid.samplefragments.data.SampleOsmPath
import org.osmdroid.samplefragments.data.SampleRace
import org.osmdroid.samplefragments.data.SampleShapeFile
import org.osmdroid.samplefragments.data.SampleSimpleFastPointOverlay
import org.osmdroid.samplefragments.data.SampleSimpleLocation
import org.osmdroid.samplefragments.data.SampleSpeechBalloon
import org.osmdroid.samplefragments.data.SampleWithMinimapItemizedOverlayWithFocus
import org.osmdroid.samplefragments.data.SampleWithMinimapItemizedOverlayWithScale
import org.osmdroid.samplefragments.data.WeatherGroundOverlaySample
import org.osmdroid.samplefragments.drawing.DrawCircle10km
import org.osmdroid.samplefragments.drawing.DrawPolygon
import org.osmdroid.samplefragments.drawing.DrawPolygonHoles
import org.osmdroid.samplefragments.drawing.DrawPolygonWithArrows
import org.osmdroid.samplefragments.drawing.DrawPolygonWithoutVerticalWrapping
import org.osmdroid.samplefragments.drawing.DrawPolygonWithoutWrapping
import org.osmdroid.samplefragments.drawing.DrawPolylineWithArrows
import org.osmdroid.samplefragments.drawing.PressToPlot
import org.osmdroid.samplefragments.drawing.PressToPlotWithoutWrapping
import org.osmdroid.samplefragments.drawing.SampleDrawPolyline
import org.osmdroid.samplefragments.drawing.SampleDrawPolylineAsPath
import org.osmdroid.samplefragments.drawing.SampleDrawPolylineWithoutVerticalWrapping
import org.osmdroid.samplefragments.drawing.SampleDrawPolylineWithoutWrapping
import org.osmdroid.samplefragments.drawing.ShowAdvancedPolylineStyles
import org.osmdroid.samplefragments.drawing.ShowAdvancedPolylineStylesInvalidation
import org.osmdroid.samplefragments.events.MarkerDrag
import org.osmdroid.samplefragments.events.SampleAnimateTo
import org.osmdroid.samplefragments.events.SampleAnimateToWithOrientation
import org.osmdroid.samplefragments.events.SampleAnimatedZoomToLocation
import org.osmdroid.samplefragments.events.SampleLimitedScrollArea
import org.osmdroid.samplefragments.events.SampleMapBootListener
import org.osmdroid.samplefragments.events.SampleMapCenterOffset
import org.osmdroid.samplefragments.events.SampleMapEventListener
import org.osmdroid.samplefragments.events.SampleSnappable
import org.osmdroid.samplefragments.events.SampleZoomRounding
import org.osmdroid.samplefragments.events.SampleZoomToBounding
import org.osmdroid.samplefragments.events.ZoomToBoundsOnStartup
import org.osmdroid.samplefragments.geopackage.GeopackageFeatureTiles
import org.osmdroid.samplefragments.geopackage.GeopackageFeatures
import org.osmdroid.samplefragments.geopackage.GeopackageSample
import org.osmdroid.samplefragments.layers.LayerManager
import org.osmdroid.samplefragments.layouts.MapInAViewPagerFragment
import org.osmdroid.samplefragments.layouts.MapInScrollView
import org.osmdroid.samplefragments.layouts.RecyclerCardView
import org.osmdroid.samplefragments.layouts.SampleFragmentXmlLayout
import org.osmdroid.samplefragments.layouts.SampleSplitScreen
import org.osmdroid.samplefragments.layouts.ScaleBarOnBottom
import org.osmdroid.samplefragments.layouts.StreetAddressFragment
import org.osmdroid.samplefragments.location.CompassPointerSample
import org.osmdroid.samplefragments.location.CompassRoseSample
import org.osmdroid.samplefragments.location.SampleCustomIconDirectedLocationOverlay
import org.osmdroid.samplefragments.location.SampleCustomMyLocation
import org.osmdroid.samplefragments.location.SampleFollowMe
import org.osmdroid.samplefragments.location.SampleHeadingCompassUp
import org.osmdroid.samplefragments.location.SampleMyLocationWithClick
import org.osmdroid.samplefragments.location.SampleRotation
import org.osmdroid.samplefragments.milstd2525.Plotter
import org.osmdroid.samplefragments.tileproviders.MapsforgeTileProviderSample
import org.osmdroid.samplefragments.tileproviders.OfflinePickerSample
import org.osmdroid.samplefragments.tileproviders.SampleAssetsOnly
import org.osmdroid.samplefragments.tileproviders.SampleAssetsOnlyRepetitionModes
import org.osmdroid.samplefragments.tileproviders.SampleOfflineGemfOnly
import org.osmdroid.samplefragments.tileproviders.SampleOfflineOnly
import org.osmdroid.samplefragments.tileproviders.SampleTileStates
import org.osmdroid.samplefragments.tileproviders.SampleUnreachableOnlineTiles
import org.osmdroid.samplefragments.tileproviders.SampleVeryHighZoomLevel
import org.osmdroid.samplefragments.tilesources.SampleCopyrightOverlay
import org.osmdroid.samplefragments.tilesources.SampleCustomLoadingImage
import org.osmdroid.samplefragments.tilesources.SampleCustomTileSource
import org.osmdroid.samplefragments.tilesources.SampleInvertedTiles_NightMode
import org.osmdroid.samplefragments.tilesources.SampleLieFi
import org.osmdroid.samplefragments.tilesources.SampleOfflineFirst
import org.osmdroid.samplefragments.tilesources.SampleOfflineSecond
import org.osmdroid.samplefragments.tilesources.SampleOpenSeaMap
import org.osmdroid.samplefragments.tilesources.SampleWMSSource
import org.osmdroid.samplefragments.tilesources.SampleWhackyColorFilter
import org.osmdroid.samplefragments.tilesources.SepiaToneTiles


/**
 * factory for all examples
 */
class SampleFactory private constructor() : ISampleFactory {
    private val mSamples: MutableList<Class<out BaseSampleFragment?>?> = ArrayList<Class<out BaseSampleFragment?>?>()


    init {
        //these are indexed with comments to make life easier when running
        //stress/memory leak testing
        //0

        mSamples.add(SampleWithMinimapItemizedOverlayWithFocus::class.java)
        //1
        mSamples.add(SampleWithMinimapItemizedOverlayWithScale::class.java)
        //2
        mSamples.add(SampleLimitedScrollArea::class.java)
        //3
        mSamples.add(SampleFragmentXmlLayout::class.java)
        //4
        mSamples.add(SampleOsmPath::class.java)
        mSamples.add(SampleRace::class.java)
        //5
        mSamples.add(SampleInvertedTiles_NightMode::class.java)
        //6
        mSamples.add(SampleOfflineOnly::class.java)
        //7
        mSamples.add(SampleAlternateCacheDir::class.java)
        //8
        mSamples.add(SampleMilitaryIconsItemizedIcons::class.java)
        //9
        mSamples.add(SampleMilitaryIconsMarker::class.java)
        //10
        //mSamples.add(SampleMapBox.class);
        //11
        mSamples.add(SampleJumboCache::class.java)
        //12
        mSamples.add(SampleCustomTileSource::class.java)
        //13
        mSamples.add(SampleAnimatedZoomToLocation::class.java)
        //14
        mSamples.add(SampleWhackyColorFilter::class.java)
        //15
        mSamples.add(SampleCustomIconDirectedLocationOverlay::class.java)
        //16
        mSamples.add(SampleAssetsOnly::class.java)
        //17
        mSamples.add(SampleSqliteOnly::class.java)
        //18
        mSamples.add(SampleCacheDownloader::class.java)
        //19
        mSamples.add(SampleCacheDownloaderCustomUI::class.java)
        //20
        mSamples.add(SampleCacheDownloaderArchive::class.java)
        //21
        mSamples.add(SampleGridlines::class.java)
        //22
        mSamples.add(SampleMapEventListener::class.java)
        //23
        mSamples.add(SampleAnimateTo::class.java)
        //24
        mSamples.add(SampleHeadingCompassUp::class.java)
        //25
        mSamples.add(SampleSplitScreen::class.java)
        //26
        mSamples.add(SampleMapBootListener::class.java)
        //27
        mSamples.add(SampleFollowMe::class.java)
        //28
        //mSamples.add(SampleMapQuest.class);
        //29
        //mSamples.add(SampleHereWeGo.class);
        //30
        mSamples.add(SampleCustomLoadingImage::class.java)
        //31
        mSamples.add(AsyncTaskDemoFragment::class.java)
        //32
        mSamples.add(CacheImport::class.java)
        //33
        mSamples.add(CachePurge::class.java)
        //34
        mSamples.add(SampleZoomToBounding::class.java)
        //35
        mSamples.add(MapInAViewPagerFragment::class.java)
        //36
        mSamples.add(ZoomToBoundsOnStartup::class.java)
        //37
        mSamples.add(SampleSimpleLocation::class.java)
        //38
        mSamples.add(SampleSimpleFastPointOverlay::class.java)
        //39
        mSamples.add(SampleOpenSeaMap::class.java)
        //40
        mSamples.add(SampleMarker::class.java)
        //41
        mSamples.add(SampleRotation::class.java)
        //42
        mSamples.add(HeatMap::class.java)
        //43
        mSamples.add(MapInScrollView::class.java)
        //44
        mSamples.add(SampleCopyrightOverlay::class.java)
        //45
        mSamples.add(SampleIISTracker::class.java)
        //46
        mSamples.add(SampleIISTrackerMotionTrails::class.java)
        //47
        mSamples.add(SampleMyLocationWithClick::class.java)
        //48
        mSamples.add(SampleDrawPolyline::class.java)
        mSamples.add(SampleDrawPolylineAsPath::class.java)
        //49
        mSamples.add(RecyclerCardView::class.java)
        //50
        mSamples.add(ScaleBarOnBottom::class.java)
        //51
        //mSamples.add(SampleBingHybrid.class);
        //52
        //mSamples.add(SampleBingRoad.class);
        //53
        mSamples.add(Gridlines2::class.java)
        //54
        mSamples.add(SepiaToneTiles::class.java)
        //55
        mSamples.add(AnimatedMarkerTimer::class.java)
        //56
        mSamples.add(FastZoomSpeedAnimations::class.java)
        //57
        mSamples.add(SampleOfflineGemfOnly::class.java)
        //58
        mSamples.add(DrawPolygon::class.java)
        mSamples.add(DrawPolygonHoles::class.java)
        mSamples.add(SampleWMSSource::class.java)
        mSamples.add(SampleAssetsOnlyRepetitionModes::class.java)
        mSamples.add(SampleDrawPolylineWithoutWrapping::class.java)
        mSamples.add(DrawPolygonWithoutWrapping::class.java)

        //mSamples.add(NasaWms111Source.class);
        //mSamples.add(NasaWms130Source.class);
        //mSamples.add(NasaWmsSrs.class);
        mSamples.add(AnimatedMarkerHandler::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) mSamples.add(AnimatedMarkerTypeEvaluator::class.java)
        mSamples.add(AnimatedMarkerValueAnimator::class.java)

        mSamples.add(MapsforgeTileProviderSample::class.java)
        mSamples.add(OfflinePickerSample::class.java)
        //59
        if (Build.VERSION.SDK_INT >= 14) {
            mSamples.add(GeopackageSample::class.java)
            mSamples.add(GeopackageFeatures::class.java)
            mSamples.add(GeopackageFeatureTiles::class.java)
        }
        // 60
        mSamples.add(SampleVeryHighZoomLevel::class.java)
        mSamples.add(MinMaxZoomLevel::class.java)
        mSamples.add(PressToPlot::class.java)
        mSamples.add(PressToPlotWithoutWrapping::class.java)
        mSamples.add(DrawPolygonWithoutVerticalWrapping::class.java)
        mSamples.add(SampleDrawPolylineWithoutVerticalWrapping::class.java)
        mSamples.add(DrawPolylineWithArrows::class.java)
        mSamples.add(ShowAdvancedPolylineStyles::class.java)
        mSamples.add(ShowAdvancedPolylineStylesInvalidation::class.java)
        mSamples.add(DrawPolygonWithArrows::class.java)

        mSamples.add(StreetAddressFragment::class.java) //map in a list view

        mSamples.add(SampleCustomMyLocation::class.java)
        mSamples.add(DrawCircle10km::class.java)
        mSamples.add(MarkerDrag::class.java)
        mSamples.add(SampleCacheDelete::class.java)
        if (Build.VERSION.SDK_INT >= 15) mSamples.add(Plotter::class.java)
        mSamples.add(WeatherGroundOverlaySample::class.java)
        mSamples.add(SampleShapeFile::class.java)
        mSamples.add(CompassPointerSample::class.java)
        mSamples.add(CompassRoseSample::class.java)
        mSamples.add(SampleZoomRounding::class.java)
        mSamples.add(LayerManager::class.java)
        mSamples.add(BookmarkSample::class.java)
        mSamples.add(SampleLieFi::class.java)
        mSamples.add(SampleItemizedOverlayMultiClick::class.java)
        mSamples.add(SampleMarkerMultiClick::class.java)
        mSamples.add(SampleMilestonesNonRepetitive::class.java)
        mSamples.add(SampleOfflineFirst::class.java)
        mSamples.add(SampleOfflineSecond::class.java)
        mSamples.add(SampleTileStates::class.java)
        mSamples.add(SampleAnimateToWithOrientation::class.java)
        mSamples.add(SampleMapSnapshot::class.java)
        mSamples.add(SampleSpeechBalloon::class.java)
        mSamples.add(SampleMapCenterOffset::class.java)
        mSamples.add(SampleSnappable::class.java)
        mSamples.add(SampleUnreachableOnlineTiles::class.java)
    }

    fun addSample(clz: Class<out BaseSampleFragment?>?) {
        mSamples.add(clz)
    }

    override fun getSample(index: Int): BaseSampleFragment? {
        try {
            return mSamples.get(index)!!.newInstance()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }

    override fun count(): Int {
        return mSamples.size
    }

    companion object {
        private var _instance: ISampleFactory? = null

        @JvmStatic
        val instance: ISampleFactory
            get() {
                if (_instance == null) {
                    _instance = SampleFactory()
                }
                return _instance!!
            }
    }
}
