package org.osmdroid.samplefragments.layouts

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.views.MapView

/**
 * The only delta with this class vs the standard mapview is that it overcomes some of the issues
 * with the map view being inside of some kind of container which allows scrolling, such as
 * scroll view
 * recycler/card view
 *
 *
 *
 *
 * created on 1/3/2017.
 *
 * @author Alex O'Ree
 */
class CustomMapView : MapView {
    constructor(context: Context, tileProvider: MapTileProviderBase?, tileRequestCompleteHandler: Handler?, attrs: AttributeSet?) : super(
        context,
        tileProvider,
        tileRequestCompleteHandler,
        attrs
    )

    constructor(
        context: Context,
        tileProvider: MapTileProviderBase?,
        tileRequestCompleteHandler: Handler?,
        attrs: AttributeSet?,
        hardwareAccelerated: Boolean
    ) : super(context, tileProvider, tileRequestCompleteHandler, attrs, hardwareAccelerated)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    constructor(context: Context, aTileProvider: MapTileProviderBase?) : super(context, aTileProvider)

    constructor(context: Context, aTileProvider: MapTileProviderBase?, tileRequestCompleteHandler: Handler?) : super(
        context,
        aTileProvider,
        tileRequestCompleteHandler
    )

    public override fun onTouchEvent(ev: MotionEvent?): Boolean {
        val action = requireNotNull(ev).getAction()
        when (action) {
            MotionEvent.ACTION_DOWN ->                 // Disallow ScrollView to intercept touch events.
                this.getParent().requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP ->                 // Allow ScrollView to intercept touch events.
                this.getParent().requestDisallowInterceptTouchEvent(false)
        }

        // Handle MapView's touch events.
        return super.onTouchEvent(ev)
    }
}
