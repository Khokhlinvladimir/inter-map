package org.osmdroid.events

import android.os.Handler
import android.util.Log
import org.osmdroid.api.IMapView

/*
 * A MapListener that aggregates multiple events called in quick succession.
 * After an event arrives, if another event arrives within <code>delay</code> milliseconds,
 * the original event is discarded.  Otherwise, the event is propagated to the wrapped
 * MapListener.  Note: This class is not thread-safe.
 *
 * @author Theodore Hong
 */
class DelayedMapListener @JvmOverloads constructor(
    /**
     * The wrapped MapListener
     */
    var wrappedListener: MapListener,
    /**
     * Listening delay, in milliseconds
     */
    protected var delay: Long = DEFAULT_DELAY.toLong()
) : MapListener {
    protected var handler: Handler
    private var callback: CallbackTask? = null

    /*
     * @param wrappedListener The wrapped MapListener
     *
     * @param delay Listening delay, in milliseconds
     */
    /*
     * Constructor with default delay.
     *
     * @param wrappedListener The wrapped MapListener
     */
    init {
        this.handler = Handler()
    }

    override fun onScroll(event: ScrollEvent): Boolean {
        dispatch(event)
        return true
    }

    override fun onZoom(event: ZoomEvent): Boolean {
        dispatch(event)
        return true
    }

    /*
     * Process an incoming MapEvent.
     */
    protected fun dispatch(event: MapEvent?) {
        // cancel any pending callback
        if (callback != null) {
            handler.removeCallbacks(callback!!)
        }
        callback = CallbackTask(event)

        // set timer
        handler.postDelayed(callback!!, delay)
    }

    // Callback tasks
    private inner class CallbackTask(private val event: MapEvent?) : Runnable {
        override fun run() {
            // do the callback
            if (event is ScrollEvent) {
                wrappedListener.onScroll(event)
            } else if (event is ZoomEvent) {
                wrappedListener.onZoom(event)
            } else {
                // unknown event; discard
                Log.d(IMapView.LOGTAG, "Unknown event received: " + event)
            }
        }
    }

    companion object {
        /**
         * Default listening delay
         */
        protected const val DEFAULT_DELAY: Int = 100
    }
}
