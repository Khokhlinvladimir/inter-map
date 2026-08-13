package org.metalev.multitouch.controller

import android.util.Log
import android.view.MotionEvent
import java.lang.reflect.Method
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/**
 * MultiTouchController.java
 * Source https://github.com/lukehutch/android-multitouch-controller/blob/master/MTController/src/org/metalev/multitouch/controller/MultiTouchController.java
 *
 *
 * Author: Luke Hutchison (luke.hutch@mit.edu)
 * Please drop me an email if you use this code so I can list your project here!
 *
 *
 * Usage:
 * `
 * public class MyMTView extends View implements MultiTouchObjectCanvas<PinchWidgetType> {
 *
 *
 * private MultiTouchController<PinchWidgetType> multiTouchController = new MultiTouchController<PinchWidgetType>(this);
 *
 *
 * // Pass touch events to the MT controller
 * public boolean onTouchEvent(MotionEvent event) {
 * return multiTouchController.onTouchEvent(event);
 * }
 *
 *
 * // ... then implement the MultiTouchObjectCanvas interface here, see details in the comments of that interface.
 * }
</PinchWidgetType></PinchWidgetType></PinchWidgetType>` *
 *
 *
 * Changelog:
 * 2010-06-09 v1.5.1  Some API changes to make it possible to selectively update or not update scale / rotation.
 * Fixed anisotropic zoom.  Cleaned up rotation code.  Added more comments.  Better var names. (LH)
 * 2010-06-09 v1.4    Added ability to track pinch rotation (Mickael Despesse, author of "Face Frenzy") and anisotropic pinch-zoom (LH)
 * 2010-06-09 v1.3.3  Bugfixes for Android-2.1; added optional debug info (LH)
 * 2010-06-09 v1.3    Ported to Android-2.2 (handle ACTION_POINTER_* actions); fixed several bugs; refactoring; documentation (LH)
 * 2010-05-17 v1.2.1  Dual-licensed under Apache and GPL licenses
 * 2010-02-18 v1.2    Support for compilation under Android 1.5/1.6 using introspection (mmin, author of handyCalc)
 * 2010-01-08 v1.1.1  Bugfixes to Cyanogen's patch that only showed up in more complex uses of controller (LH)
 * 2010-01-06 v1.1    Modified for official level 5 MT API (Cyanogen)
 * 2009-01-25 v1.0    Original MT controller, released for hacked G1 kernel (LH)
 *
 *
 * TODO:
 * - Add inertia (flick-pinch-zoom or flick-scroll)
 * - Merge in Paul Bourke's "grab" support for single-finger drag of objects: git://github.com/brk3/android-multitouch-controller.git
 * (Initial concern are the two lines of the form "newScale = mCurrXform.scale - 0.04f", and the line in pastThreshold() that says
 * "if (newScale == mCurrXform.scale)" -- this doesn't look like a robust solution to convey state, by changing scale by a tiny
 * amount, but maybe I'm not understanding the intent behind the code or its behavior).
 *
 *
 * Known usages: see http://code.google.com/p/android-multitouch-controller/
 *
 *
 * --
 *
 *
 * Released under the MIT license (but please notify me if you use this code, so that I can give your project credit at
 * http://code.google.com/p/android-multitouch-controller ).
 *
 *
 * MIT license: http://www.opensource.org/licenses/MIT
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

/**
 * A class that simplifies the implementation of multitouch in applications. Subclass this and read the fields here as needed in subclasses.
 *
 * @author Luke Hutchison
 */
class MultiTouchController<T> @JvmOverloads constructor(objectCanvas: MultiTouchObjectCanvas<T?>, handleSingleTouchEvents: Boolean = true) {
    // ----------------------------------------------------------------------------------------------------------------------
    var objectCanvas: MultiTouchObjectCanvas<T?>

    /** The current touch point  */
    private var mCurrPt: PointInfo

    /** The previous touch point  */
    private var mPrevPt: PointInfo

    /** Fields extracted from mCurrPt  */
    private var mCurrPtX = 0f
    private var mCurrPtY = 0f
    private var mCurrPtDiam = 0f
    private var mCurrPtWidth = 0f
    private var mCurrPtHeight = 0f
    private var mCurrPtAng = 0f

    /**
     * Extract fields from mCurrPt, respecting the update* fields of mCurrPt. This just avoids code duplication. I hate that Java doesn't support
     * higher-order functions, tuples or multiple return values from functions.
     */
    private fun extractCurrPtInfo() {
        // Get new drag/pinch params. Only read multitouch fields that are needed,
        // to avoid unnecessary computation (diameter and angle are expensive operations).
        mCurrPtX = mCurrPt.x
        mCurrPtY = mCurrPt.y
        mCurrPtDiam = max(MIN_MULTITOUCH_SEPARATION * .71f, if (!mCurrXform.updateScale) 0.0f else mCurrPt.multiTouchDiameter)
        mCurrPtWidth = max(MIN_MULTITOUCH_SEPARATION, if (!mCurrXform.updateScaleXY) 0.0f else mCurrPt.multiTouchWidth)
        mCurrPtHeight = max(MIN_MULTITOUCH_SEPARATION, if (!mCurrXform.updateScaleXY) 0.0f else mCurrPt.multiTouchHeight)
        mCurrPtAng = if (!mCurrXform.updateAngle) 0.0f else mCurrPt.multiTouchAngle
    }

    // ----------------------------------------------------------------------------------------------------------------------
    /**
     * Whether to handle single-touch events/drags before multi-touch is initiated or not; if not, they are handled by subclasses. Default: true
     */
    /**
     * Whether to handle single-touch events/drags before multi-touch is initiated or not; if not, they are handled by subclasses. Default: true
     */
    /** Whether to handle single-touch events/drags before multi-touch is initiated or not; if not, they are handled by subclasses  */
    protected var handleSingleTouchEvents: Boolean

    /** The object being dragged/stretched  */
    private var selectedObject: T? = null

    /** Current position and scale of the dragged object  */
    private val mCurrXform = PositionAndScale()

    /** Drag/pinch start time and time to ignore spurious events until (to smooth over event noise)  */
    private var mSettleStartTime: Long = 0
    private var mSettleEndTime: Long = 0

    /** Conversion from object coords to screen coords  */
    private var startPosX = 0f
    private var startPosY = 0f

    /** Conversion between scale and width, and object angle and start pinch angle  */
    private var startScaleOverPinchDiam = 0f
    private var startAngleMinusPinchAngle = 0f

    /** Conversion between X scale and width, and Y scale and height  */
    private var startScaleXOverPinchWidth = 0f
    private var startScaleYOverPinchHeight = 0f

    /** Current drag mode  */
    var mode: Int = MODE_NOTHING
        private set

    // ------------------------------------------------------------------------------------

    /** Full constructor  */ // ----------------------------------------------------------------------------------------------------------------------
    /** Constructor that sets handleSingleTouchEvents to true  */
    init {
        this.mCurrPt = PointInfo()
        this.mPrevPt = PointInfo()
        this.handleSingleTouchEvents = handleSingleTouchEvents
        this.objectCanvas = objectCanvas
    }

    /** Process incoming touch events  */
    @Suppress("unused")
    fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            val pointerCount: Int =
                (if (MultiTouchController.Companion.multiTouchSupported) MultiTouchController.Companion.m_getPointerCount!!.invoke(event) as Int? else 1)!!
            if (DEBUG) Log.i(
                "MultiTouch",
                "Got here 1 - " + multiTouchSupported + " " + this.mode + " " + handleSingleTouchEvents + " " + pointerCount
            )
            if (this.mode == MODE_NOTHING && !handleSingleTouchEvents && pointerCount == 1)  // Not handling initial single touch events, just pass them on
                return false
            if (DEBUG) Log.i("MultiTouch", "Got here 2")

            // Handle history first (we sometimes get history with ACTION_MOVE events)
            val action = event.getAction()
            val histLen = event.getHistorySize() / pointerCount
            for (histIdx in 0..histLen) {
                // Read from history entries until histIdx == histLen, then read from current event
                val processingHist = histIdx < histLen
                if (!multiTouchSupported || pointerCount == 1) {
                    // Use single-pointer methods -- these are needed as a special case (for some weird reason) even if
                    // multitouch is supported but there's only one touch point down currently -- event.getX(0) etc. throw
                    // an exception if there's only one point down.
                    if (DEBUG) Log.i("MultiTouch", "Got here 3")
                    xVals[0] = if (processingHist) event.getHistoricalX(histIdx) else event.getX()
                    yVals[0] = if (processingHist) event.getHistoricalY(histIdx) else event.getY()
                    pressureVals[0] = if (processingHist) event.getHistoricalPressure(histIdx) else event.getPressure()
                } else {
                    // Read x, y and pressure of each pointer
                    if (DEBUG) Log.i("MultiTouch", "Got here 4")
                    val numPointers = min(pointerCount, MAX_TOUCH_POINTS)
                    if (DEBUG && pointerCount > MAX_TOUCH_POINTS) Log.i("MultiTouch", "Got more pointers than MAX_TOUCH_POINTS")
                    for (ptrIdx in 0 until numPointers) {
                        val ptrId = m_getPointerId!!.invoke(event, ptrIdx) as Int
                        pointerIds[ptrIdx] = ptrId
                        // N.B. if pointerCount == 1, then the following methods throw an array index out of range exception,
                        // and the code above is therefore required not just for Android 1.5/1.6 but also for when there is
                        // only one touch point on the screen -- pointlessly inconsistent :(
                        xVals[ptrIdx] = ((if (processingHist) MultiTouchController.Companion.m_getHistoricalX!!.invoke(
                            event,
                            ptrIdx,
                            histIdx
                        ) else MultiTouchController.Companion.m_getX!!.invoke(event, ptrIdx)) as kotlin.Float?)!!
                        yVals[ptrIdx] = ((if (processingHist) MultiTouchController.Companion.m_getHistoricalY!!.invoke(
                            event,
                            ptrIdx,
                            histIdx
                        ) else MultiTouchController.Companion.m_getY!!.invoke(event, ptrIdx)) as kotlin.Float?)!!
                        pressureVals[ptrIdx] = ((if (processingHist) MultiTouchController.Companion.m_getHistoricalPressure!!.invoke(
                            event,
                            ptrIdx,
                            histIdx
                        ) else MultiTouchController.Companion.m_getPressure!!
                            .invoke(event, ptrIdx)) as kotlin.Float?)!!
                    }
                }
                // Decode event
                decodeTouchEvent(
                    pointerCount, xVals, yVals, pressureVals, pointerIds,  //
                    /* action = */
                    if (processingHist) MotionEvent.ACTION_MOVE else action,  //
                    /* down = */
                    if (processingHist) true else action != MotionEvent.ACTION_UP //
                            && (action and ((1 shl ACTION_POINTER_INDEX_SHIFT) - 1)) != ACTION_POINTER_UP //
                            && action != MotionEvent.ACTION_CANCEL,  //
                    if (processingHist) event.getHistoricalEventTime(histIdx) else event.getEventTime()
                )
            }

            return true
        } catch (e: Exception) {
            // In case any of the introspection stuff fails (it shouldn't)
            Log.e("MultiTouchController", "onTouchEvent() failed", e)
            return false
        }
    }

    private fun decodeTouchEvent(
        pointerCount: Int,
        x: FloatArray,
        y: FloatArray,
        pressure: FloatArray,
        pointerIds: IntArray,
        action: Int,
        down: Boolean,
        eventTime: Long
    ) {
        if (DEBUG) Log.i("MultiTouch", "Got here 5 - " + pointerCount + " " + action + " " + down)

        // Swap curr/prev points
        val tmp = mPrevPt
        mPrevPt = mCurrPt
        mCurrPt = tmp
        // Overwrite old prev point
        mCurrPt.set(pointerCount, x, y, pressure, pointerIds, action, down, eventTime)
        multiTouchController()
    }

    // ------------------------------------------------------------------------------------
    /** Start dragging/pinching, or reset drag/pinch to current point if something goes out of range  */
    private fun anchorAtThisPositionAndScale() {
        if (DEBUG) Log.i("MulitTouch", "anchorAtThisPositionAndScale()")
        if (selectedObject == null) return

        // Get selected object's current position and scale
        objectCanvas.getPositionAndScale(selectedObject, mCurrXform)

        // Figure out the object coords of the drag start point's screen coords.
        // All stretching should be around this point in object-coord-space.
        // Also figure out out ratio between object scale factor and multitouch
        // diameter at beginning of drag; same for angle and optional anisotropic
        // scale.
        val currScaleInv = 1.0f / (if (!mCurrXform.updateScale) 1.0f else if (mCurrXform.scale == 0.0f) 1.0f else mCurrXform.scale)
        extractCurrPtInfo()
        startPosX = (mCurrPtX - mCurrXform.xOff) * currScaleInv
        startPosY = (mCurrPtY - mCurrXform.yOff) * currScaleInv
        startScaleOverPinchDiam = mCurrXform.scale / mCurrPtDiam
        startScaleXOverPinchWidth = mCurrXform.scaleX / mCurrPtWidth
        startScaleYOverPinchHeight = mCurrXform.scaleY / mCurrPtHeight
        startAngleMinusPinchAngle = mCurrXform.angle - mCurrPtAng
    }

    /** Drag/stretch/rotate the selected object using the current touch position(s) relative to the anchor position(s).  */
    private fun performDragOrPinch() {
        // Don't do anything if we're not dragging anything
        if (selectedObject == null) return

        // Calc new position of dragged object
        val currScale = if (!mCurrXform.updateScale) 1.0f else if (mCurrXform.scale == 0.0f) 1.0f else mCurrXform.scale
        extractCurrPtInfo()
        val newPosX = mCurrPtX - startPosX * currScale
        val newPosY = mCurrPtY - startPosY * currScale
        val newScale = startScaleOverPinchDiam * mCurrPtDiam
        val newScaleX = startScaleXOverPinchWidth * mCurrPtWidth
        val newScaleY = startScaleYOverPinchHeight * mCurrPtHeight
        val newAngle = startAngleMinusPinchAngle + mCurrPtAng

        // Set the new obj coords, scale, and angle as appropriate (notifying the subclass of the change).
        mCurrXform.set(newPosX, newPosY, newScale, newScaleX, newScaleY, newAngle)

        val success = objectCanvas.setPositionAndScale(selectedObject, mCurrXform, mCurrPt)
        if (!success);
    }

    val isPinching: Boolean
        /** Indicate if we are in the middle of a pinch action or not. osmdroid addon */
        get() = this.mode == MODE_PINCH


    /**
     * State-based controller for tracking switches between no-touch, single-touch and multi-touch situations. Includes logic for cleaning up the
     * event stream, as events around touch up/down are noisy at least on early Synaptics sensors.
     */
    private fun multiTouchController() {
        if (DEBUG) Log.i("MultiTouch", "Got here 6 - " + this.mode + " " + mCurrPt.numTouchPoints + " " + mCurrPt.isDown + mCurrPt.isMultiTouch)

        when (this.mode) {
            MODE_NOTHING -> {
                if (DEBUG) Log.i("MultiTouch", "MODE_NOTHING")
                // Not doing anything currently
                if (mCurrPt.isDown) {
                    // Start a new single-point drag
                    selectedObject = objectCanvas.getDraggableObjectAtPoint(mCurrPt)
                    if (selectedObject != null) {
                        // Started a new single-point drag
                        this.mode = MODE_DRAG
                        objectCanvas.selectObject(selectedObject, mCurrPt)
                        anchorAtThisPositionAndScale()
                        // Don't need any settling time if just placing one finger, there is no noise
                        mSettleEndTime = mCurrPt.eventTime
                        mSettleStartTime = mSettleEndTime
                    }
                }
            }

            MODE_DRAG -> {
                if (DEBUG) Log.i("MultiTouch", "MODE_DRAG")
                // Currently in a single-point drag
                if (!mCurrPt.isDown) {
                    // First finger was released, stop dragging
                    this.mode = MODE_NOTHING
                    objectCanvas.selectObject((null.also { selectedObject = it }), mCurrPt)
                } else if (mCurrPt.isMultiTouch) {
                    // Point 1 was already down and point 2 was just placed down
                    this.mode = MODE_PINCH
                    // Restart the drag with the new drag position (that is at the midpoint between the touchpoints)
                    anchorAtThisPositionAndScale()
                    // Need to let events settle before moving things, to help with event noise on touchdown
                    mSettleStartTime = mCurrPt.eventTime
                    mSettleEndTime = mSettleStartTime + EVENT_SETTLE_TIME_INTERVAL
                } else {
                    // Point 1 is still down and point 2 did not change state, just do single-point drag to new location
                    if (mCurrPt.eventTime < mSettleEndTime) {
                        // Ignore the first few events if we just stopped stretching, because if finger 2 was kept down while
                        // finger 1 is lifted, then point 1 gets mapped to finger 2. Restart the drag from the new position.
                        anchorAtThisPositionAndScale()
                    } else {
                        // Keep dragging, move to new point
                        performDragOrPinch()
                    }
                }
            }

            MODE_PINCH -> {
                if (DEBUG) Log.i("MultiTouch", "MODE_PINCH")
                // Two-point pinch-scale/rotate/translate
                if (!mCurrPt.isMultiTouch || !mCurrPt.isDown) {
                    // Dropped one or both points, stop stretching

                    if (!mCurrPt.isDown) {
                        // Dropped both points, go back to doing nothing
                        this.mode = MODE_NOTHING
                        objectCanvas.selectObject((null.also { selectedObject = it }), mCurrPt)
                    } else {
                        // Just dropped point 2, downgrade to a single-point drag
                        this.mode = MODE_DRAG
                        // Restart the pinch with the single-finger position
                        anchorAtThisPositionAndScale()
                        // Ignore the first few events after the drop, in case we dropped finger 1 and left finger 2 down
                        mSettleStartTime = mCurrPt.eventTime
                        mSettleEndTime = mSettleStartTime + EVENT_SETTLE_TIME_INTERVAL
                    }
                } else {
                    // Still pinching
                    if (abs(mCurrPt.x - mPrevPt.x) > MAX_MULTITOUCH_POS_JUMP_SIZE || abs(
                            mCurrPt.y - mPrevPt.y
                        ) > MAX_MULTITOUCH_POS_JUMP_SIZE || abs(mCurrPt.multiTouchWidth - mPrevPt.multiTouchWidth) * .5f > MAX_MULTITOUCH_DIM_JUMP_SIZE || abs(
                            mCurrPt.multiTouchHeight - mPrevPt.multiTouchHeight
                        ) * .5f > MAX_MULTITOUCH_DIM_JUMP_SIZE
                    ) {
                        // Jumped too far, probably event noise, reset and ignore events for a bit
                        anchorAtThisPositionAndScale()
                        mSettleStartTime = mCurrPt.eventTime
                        mSettleEndTime = mSettleStartTime + EVENT_SETTLE_TIME_INTERVAL
                    } else if (mCurrPt.eventTime < mSettleEndTime) {
                        // Events have not yet settled, reset
                        anchorAtThisPositionAndScale()
                    } else {
                        // Stretch to new position and size
                        performDragOrPinch()
                    }
                }
            }
        }
        if (DEBUG) Log.i("MultiTouch", "Got here 7 - " + this.mode + " " + mCurrPt.numTouchPoints + " " + mCurrPt.isDown + mCurrPt.isMultiTouch)
    }

    /** A class that packages up all MotionEvent information with all derived multitouch information (if available)  */
    class PointInfo {
        /** Return the total number of touch points  */
        // Multitouch information
        var numTouchPoints: Int = 0
            private set

        /** Return the array of X coords -- only the first getNumTouchPoints() of these is defined.  */
        val xs: FloatArray = FloatArray(MAX_TOUCH_POINTS)

        /** Return the array of Y coords -- only the first getNumTouchPoints() of these is defined.  */
        val ys: FloatArray = FloatArray(MAX_TOUCH_POINTS)

        /** Return the array of pressures -- only the first getNumTouchPoints() of these is defined.  */
        val pressures: FloatArray = FloatArray(MAX_TOUCH_POINTS)

        /**
         * Return the array of pointer ids -- only the first getNumTouchPoints() of these is defined. These don't have to be all the numbers from 0 to
         * getNumTouchPoints()-1 inclusive, numbers can be skipped if a finger is lifted and the touch sensor is capable of detecting that that
         * particular touch point is no longer down. Note that a lot of sensors do not have this capability: when finger 1 is lifted up finger 2
         * becomes the new finger 1.  However in theory these IDs can correct for that.  Convert back to indices using MotionEvent.findPointerIndex().
         */
        val pointerIds: IntArray = IntArray(MAX_TOUCH_POINTS)

        /** Return the X coord of the first touch point if there's only one, or the midpoint between first and second touch points if two or more.  */
        // Midpoint of pinch operations
        var x: Float = 0f
            private set

        /** Return the X coord of the first touch point if there's only one, or the midpoint between first and second touch points if two or more.  */
        var y: Float = 0f
            private set

        /** Return the pressure the first touch point if there's only one, or the average pressure of first and second touch points if two or more.  */
        var pressure: Float = 0f
            private set

        // Width/diameter/angle of pinch operations
        private var dx = 0f
        private var dy = 0f
        private var diameter = 0f
        private var diameterSq = 0f
        private var angle = 0f

        // -------------------------------------------------------------------------------------------------------------------------------------------
        // Whether or not there is at least one finger down (isDown) and/or at least two fingers down (isMultiTouch)
        var isDown: Boolean = false
            private set

        /** True if number of touch points greater than or equal to 2.  */
        var isMultiTouch: Boolean = false
            private set

        // Whether or not these fields have already been calculated, for caching purposes
        private var diameterSqIsCalculated = false
        private var diameterIsCalculated = false
        private var angleIsCalculated = false

        // Event action code and event time
        var action: Int = 0
            private set
        var eventTime: Long = 0
            private set

        // -------------------------------------------------------------------------------------------------------------------------------------------
        /** Set all point info  */
        internal fun set(
            numPoints: Int,
            x: FloatArray,
            y: FloatArray,
            pressure: FloatArray,
            pointerIds: IntArray,
            action: Int,
            isDown: Boolean,
            eventTime: Long
        ) {
            if (DEBUG) Log.i(
                "MultiTouch", ("Got here 8 - " + +numPoints + " " + x[0] + " " + y[0] + " " + (if (numPoints > 1) x[1] else x[0]) + " "
                        + (if (numPoints > 1) y[1] else y[0]) + " " + action + " " + isDown)
            )
            this.eventTime = eventTime
            this.action = action
            this.numTouchPoints = numPoints
            for (i in 0 until numPoints) {
                this.xs[i] = x[i]
                this.ys[i] = y[i]
                this.pressures[i] = pressure[i]
                this.pointerIds[i] = pointerIds[i]
            }
            this.isDown = isDown
            this.isMultiTouch = numPoints >= 2

            if (isMultiTouch) {
                this.x = (x[0] + x[1]) * .5f
                this.y = (y[0] + y[1]) * .5f
                this.pressure = (pressure[0] + pressure[1]) * .5f
                dx = abs(x[1] - x[0])
                dy = abs(y[1] - y[0])
            } else {
                // Single-touch event
                this.x = x[0]
                this.y = y[0]
                this.pressure = pressure[0]
                dy = 0.0f
                dx = dy
            }
            // Need to re-calculate the expensive params if they're needed
            angleIsCalculated = false
            diameterIsCalculated = angleIsCalculated
            diameterSqIsCalculated = diameterIsCalculated
        }

        /**
         * Copy all fields from one PointInfo class to another. PointInfo objects are volatile so you should use this if you want to keep track of the
         * last touch event in your own code.
         */
        fun set(other: PointInfo) {
            this.numTouchPoints = other.numTouchPoints
            for (i in 0 until this.numTouchPoints) {
                this.xs[i] = other.xs[i]
                this.ys[i] = other.ys[i]
                this.pressures[i] = other.pressures[i]
                this.pointerIds[i] = other.pointerIds[i]
            }
            this.x = other.x
            this.y = other.y
            this.pressure = other.pressure
            this.dx = other.dx
            this.dy = other.dy
            this.diameter = other.diameter
            this.diameterSq = other.diameterSq
            this.angle = other.angle
            this.isDown = other.isDown
            this.action = other.action
            this.isMultiTouch = other.isMultiTouch
            this.diameterIsCalculated = other.diameterIsCalculated
            this.diameterSqIsCalculated = other.diameterSqIsCalculated
            this.angleIsCalculated = other.angleIsCalculated
            this.eventTime = other.eventTime
        }

        // -------------------------------------------------------------------------------------------------------------------------------------------

        val multiTouchWidth: Float
            /** Difference between x coords of touchpoint 0 and 1.  */
            get() = if (isMultiTouch) dx else 0.0f

        val multiTouchHeight: Float
            /** Difference between y coords of touchpoint 0 and 1.  */
            get() = if (isMultiTouch) dy else 0.0f

        /** Fast integer sqrt, by Jim Ulery. Much faster than Math.sqrt() for integers.  */
        private fun julery_isqrt(`val`: Int): Int {
            var `val` = `val`
            var temp: Int
            var g = 0
            var b = 0x8000
            var bshft = 15
            do {
                if (`val` >= ((((g shl 1) + b) shl bshft--).also { temp = it })) {
                    g += b
                    `val` -= temp
                }
            } while ((1.let { b = b shr it; b }) > 0)
            return g
        }

        val multiTouchDiameterSq: Float
            /** Calculate the squared diameter of the multitouch event, and cache it. Use this if you don't need to perform the sqrt.  */
            get() {
                if (!diameterSqIsCalculated) {
                    diameterSq = (if (isMultiTouch) dx * dx + dy * dy else 0.0f)
                    diameterSqIsCalculated = true
                }
                return diameterSq
            }

        val multiTouchDiameter: Float
            /** Calculate the diameter of the multitouch event, and cache it. Uses fast int sqrt but gives accuracy to 1/16px.  */
            get() {
                if (!diameterIsCalculated) {
                    if (!isMultiTouch) {
                        diameter = 0.0f
                    } else {
                        // Get 1/16 pixel's worth of subpixel accuracy, works on screens up to 2048x2048
                        // before we get overflow (at which point you can reduce or eliminate subpix
                        // accuracy, or use longs in julery_isqrt())
                        val diamSq = this.multiTouchDiameterSq
                        diameter = (if (diamSq == 0.0f) 0.0f else julery_isqrt((256 * diamSq).toInt()).toFloat() / 16.0f)
                        // Make sure diameter is never less than dx or dy, for trig purposes
                        if (diameter < dx) diameter = dx
                        if (diameter < dy) diameter = dy
                    }
                    diameterIsCalculated = true
                }
                return diameter
            }

        val multiTouchAngle: Float
            /**
             * Calculate the angle of a multitouch event, and cache it. Actually gives the smaller of the two angles between the x axis and the line
             * between the two touchpoints, so range is [0,Math.PI/2]. Uses Math.atan2().
             */
            get() {
                if (!angleIsCalculated) {
                    if (!isMultiTouch) angle = 0.0f
                    else angle = atan2((ys[1] - ys[0]).toDouble(), (xs[1] - xs[0]).toDouble()).toFloat()
                    angleIsCalculated = true
                }
                return angle
            }

        // -------------------------------------------------------------------------------------------------------------------------------------------
    }

    // ------------------------------------------------------------------------------------
    /**
     * A class that is used to store scroll offsets and scale information for objects that are managed by the multitouch controller
     */
    class PositionAndScale {
        var xOff: Float = 0f
            private set
        var yOff: Float = 0f
            private set
        internal var scale = 0f
        internal var scaleX = 0f
        internal var scaleY = 0f
        internal var angle = 0f
        internal var updateScale = false
        internal var updateScaleXY = false
        internal var updateAngle = false

        /**
         * Set position and optionally scale, anisotropic scale, and/or angle. Where if the corresponding "update" flag is set to false, the field's
         * value will not be changed during a pinch operation. If the value is not being updated *and* the value is not used by the client
         * application, then the value can just be zero. However if the value is not being updated but the value *is* being used by the client
         * application, the value should still be specified and the update flag should be false (e.g. angle of the object being dragged should still
         * be specified even if the program is in "resize" mode rather than "rotate" mode).
         */
        fun set(
            xOff: Float, yOff: Float, updateScale: Boolean, scale: Float, updateScaleXY: Boolean, scaleX: Float, scaleY: Float,
            updateAngle: Boolean, angle: Float
        ) {
            this.xOff = xOff
            this.yOff = yOff
            this.updateScale = updateScale
            this.scale = if (scale == 0.0f) 1.0f else scale
            this.updateScaleXY = updateScaleXY
            this.scaleX = if (scaleX == 0.0f) 1.0f else scaleX
            this.scaleY = if (scaleY == 0.0f) 1.0f else scaleY
            this.updateAngle = updateAngle
            this.angle = angle
        }

        /** Set position and optionally scale, anisotropic scale, and/or angle, without changing the "update" flags.  */
        fun set(xOff: Float, yOff: Float, scale: Float, scaleX: Float, scaleY: Float, angle: Float) {
            this.xOff = xOff
            this.yOff = yOff
            this.scale = if (scale == 0.0f) 1.0f else scale
            this.scaleX = if (scaleX == 0.0f) 1.0f else scaleX
            this.scaleY = if (scaleY == 0.0f) 1.0f else scaleY
            this.angle = angle
        }

        fun getScale(): Float {
            return if (!updateScale) 1.0f else scale
        }

        /** Included in case you want to support anisotropic scaling  */
        fun getScaleX(): Float {
            return if (!updateScaleXY) 1.0f else scaleX
        }

        /** Included in case you want to support anisotropic scaling  */
        fun getScaleY(): Float {
            return if (!updateScaleXY) 1.0f else scaleY
        }

        fun getAngle(): Float {
            return if (!updateAngle) 0.0f else angle
        }
    }

    // ------------------------------------------------------------------------------------
    interface MultiTouchObjectCanvas<T> {
        /**
         * See if there is a draggable object at the current point. Returns the object at the point, or null if nothing to drag. To start a multitouch
         * drag/stretch operation, this routine must return some non-null reference to an object. This object is passed into the other methods in this
         * interface when they are called.
         *
         * @param touchPoint
         * The point being tested (in object coordinates). Return the topmost object under this point, or if dragging/stretching the whole
         * canvas, just return a reference to the canvas.
         * @return a reference to the object under the point being tested, or null to cancel the drag operation. If dragging/stretching the whole
         * canvas (e.g. in a photo viewer), always return non-null, otherwise the stretch operation won't work.
         */
        fun getDraggableObjectAtPoint(touchPoint: PointInfo?): T?

        /**
         * Get the screen coords of the dragged object's origin, and scale multiplier to convert screen coords to obj coords. The job of this routine
         * is to call the .set() method on the passed PositionAndScale object to record the initial position and scale of the object (in object
         * coordinates) before any dragging/stretching takes place.
         *
         * @param obj
         * The object being dragged/stretched.
         * @param objPosAndScaleOut
         * Output parameter: You need to call objPosAndScaleOut.set() to record the current position and scale of obj.
         */
        fun getPositionAndScale(obj: T?, objPosAndScaleOut: PositionAndScale?)

        /**
         * Callback to update the position and scale (in object coords) of the currently-dragged object.
         *
         * @param obj
         * The object being dragged/stretched.
         * @param newObjPosAndScale
         * The new position and scale of the object, in object coordinates. Use this to move/resize the object before returning.
         * @param touchPoint
         * Info about the current touch point, including multitouch information and utilities to calculate and cache multitouch pinch
         * diameter etc. (Note: touchPoint is volatile, if you want to keep any fields of touchPoint, you must copy them before the method
         * body exits.)
         * @return true if setting the position and scale of the object was successful, or false if the position or scale parameters are out of range
         * for this object.
         */
        fun setPositionAndScale(obj: T?, newObjPosAndScale: PositionAndScale?, touchPoint: PointInfo?): Boolean

        /**
         * Select an object at the given point. Can be used to bring the object to top etc. Only called when first touchpoint goes down, not when
         * multitouch is initiated. Also called with null on touch-up.
         *
         * @param obj
         * The object being selected by single-touch, or null on touch-up.
         * @param touchPoint
         * The current touch point.
         */
        fun selectObject(obj: T?, touchPoint: PointInfo?)
    }

    companion object {
        /**
         * Time in ms required after a change in event status (e.g. putting down or lifting off the second finger) before events actually do anything --
         * helps eliminate noisy jumps that happen on change of status
         */
        private const val EVENT_SETTLE_TIME_INTERVAL: Long = 20

        /**
         * The biggest possible abs val of the change in x or y between multitouch events (larger dx/dy events are ignored) -- helps eliminate jumps in
         * pointer position on finger 2 up/down.
         */
        private const val MAX_MULTITOUCH_POS_JUMP_SIZE = 30.0f

        /**
         * The biggest possible abs val of the change in multitouchWidth or multitouchHeight between multitouch events (larger-jump events are ignored) --
         * helps eliminate jumps in pointer position on finger 2 up/down.
         */
        private const val MAX_MULTITOUCH_DIM_JUMP_SIZE = 40.0f

        /** The smallest possible distance between multitouch points (used to avoid div-by-zero errors and display glitches)  */
        private const val MIN_MULTITOUCH_SEPARATION = 30.0f

        /** The max number of touch points that can be present on the screen at once  */
        const val MAX_TOUCH_POINTS: Int = 20

        /** Generate tons of log entries for debugging  */
        const val DEBUG: Boolean = false

        // ----------------------------------------------------------------------------------------------------------------------
        /** No touch points down.  */
        private const val MODE_NOTHING = 0

        /** One touch point down, dragging an object.  */
        private const val MODE_DRAG = 1

        /** Two or more touch points down, stretching/rotating an object using the first two touch points.  */
        private const val MODE_PINCH = 2

        // ------------------------------------------------------------------------------------
        val multiTouchSupported: Boolean
        private var m_getPointerCount: Method? = null
        private var m_getPointerId: Method? = null
        private var m_getPressure: Method? = null
        private var m_getHistoricalX: Method? = null
        private var m_getHistoricalY: Method? = null
        private var m_getHistoricalPressure: Method? = null
        private var m_getX: Method? = null
        private var m_getY: Method? = null
        private var ACTION_POINTER_UP = 6
        private var ACTION_POINTER_INDEX_SHIFT = 8

        init {
            var succeeded = false
            try {
                // Android 2.0.1 stuff:
                m_getPointerCount = MotionEvent::class.java.getMethod("getPointerCount")
                m_getPointerId = MotionEvent::class.java.getMethod("getPointerId", Integer.TYPE)
                m_getPressure = MotionEvent::class.java.getMethod("getPressure", Integer.TYPE)
                m_getHistoricalX = MotionEvent::class.java.getMethod("getHistoricalX", Integer.TYPE, Integer.TYPE)
                m_getHistoricalY = MotionEvent::class.java.getMethod("getHistoricalY", Integer.TYPE, Integer.TYPE)
                m_getHistoricalPressure = MotionEvent::class.java.getMethod("getHistoricalPressure", Integer.TYPE, Integer.TYPE)
                m_getX = MotionEvent::class.java.getMethod("getX", Integer.TYPE)
                m_getY = MotionEvent::class.java.getMethod("getY", Integer.TYPE)
                succeeded = true
            } catch (e: Exception) {
                Log.e("MultiTouchController", "static initializer failed", e)
            }
            multiTouchSupported = succeeded
            if (multiTouchSupported) {
                // Android 2.2+ stuff (the original Android 2.2 consts are declared above,
                // and these actions aren't used previous to Android 2.2):
                try {
                    ACTION_POINTER_UP = MotionEvent::class.java.getField("ACTION_POINTER_UP").getInt(null)
                    ACTION_POINTER_INDEX_SHIFT = MotionEvent::class.java.getField("ACTION_POINTER_INDEX_SHIFT").getInt(null)
                } catch (e: Exception) {
                }
            }
        }

        // ------------------------------------------------------------------------------------
        private val xVals = FloatArray(MAX_TOUCH_POINTS)
        private val yVals = FloatArray(MAX_TOUCH_POINTS)
        private val pressureVals = FloatArray(MAX_TOUCH_POINTS)
        private val pointerIds = IntArray(MAX_TOUCH_POINTS)
    }
}
