// Created by plusminus on 20:50:06 - 03.10.2008
package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.TypedValue
import org.osmdroid.library.R
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace
import org.osmdroid.views.util.constants.OverlayConstants
import kotlin.math.max
import kotlin.math.min

/**
 * @param <Item>
</Item> */
@Deprecated(
    """see {@link Marker}
  it is generally recommended to use the  {@link Marker} class instead of this.
  While it does work and is usually maintained, the Marker class as a lot more capabilities"""
)
class ItemizedOverlayWithFocus<Item : OverlayItem?>(
    aList: MutableList<Item?>?, pMarker: Drawable?,
    pMarkerFocused: Drawable?, pFocusedBackgroundColor: Int,
    aOnItemTapListener: OnItemGestureListener<Item?>?, pContext: Context
) : ItemizedIconOverlay<Item?>(aList, pMarker, aOnItemTapListener, pContext) {
    // ===========================================================
    // Constants
    // ===========================================================
    private val DEFAULTMARKER_BACKGROUNDCOLOR = Color.rgb(101, 185, 74)


    // ===========================================================
    // Fields
    // ===========================================================
    private var DESCRIPTION_BOX_PADDING = 3
    private var DESCRIPTION_BOX_CORNERWIDTH = 3

    /**
     * Additional to `DESCRIPTION_LINE_HEIGHT`.
     */
    private var DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = 2

    private var FONT_SIZE_DP = 14
    private var DESCRIPTION_MAXWIDTH = 600
    private var DESCRIPTION_LINE_HEIGHT = 30

    protected var mMarkerFocusedBackgroundColor: Int
    protected var mMarkerBackgroundPaint: Paint? = null
    protected var mDescriptionPaint: Paint? = null
    protected var mTitlePaint: Paint? = null


    protected var mMarkerFocusedBase: Drawable? = null
    protected var mFocusedItemIndex: Int = 0
    protected var mFocusItemsOnTap: Boolean = false
    private var fontSizePixels = 0
    private val mFocusedScreenCoords = Point()

    private var mContext: Context?

    private var UNKNOWN: String? = null

    // ===========================================================
    // Constructors
    // ===========================================================
    constructor(
        pContext: Context, aList: MutableList<Item?>?,
        aOnItemTapListener: OnItemGestureListener<Item?>?
    ) : this(aList, aOnItemTapListener, pContext)

    constructor(
        aList: MutableList<Item?>?,
        aOnItemTapListener: OnItemGestureListener<Item?>?, pContext: Context
    ) : this(
        aList,
        pContext.getResources().getDrawable(R.drawable.marker_default),
        null, OverlayConstants.Companion.NOT_SET,
        aOnItemTapListener, pContext
    )

    private fun calculateDrawSettings() {
        //calculate font size based on DP
        fontSizePixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            FONT_SIZE_DP.toFloat(), mContext!!.getResources().getDisplayMetrics()
        ).toInt()
        DESCRIPTION_LINE_HEIGHT = fontSizePixels + 5

        //calculate max width based on screen width.
        DESCRIPTION_MAXWIDTH = (mContext!!.getResources().getDisplayMetrics().widthPixels * 0.8).toInt()
        UNKNOWN = mContext!!.getResources().getString(R.string.unknown)

        this.mMarkerBackgroundPaint = Paint() // Color is set in onDraw(...)

        this.mDescriptionPaint = Paint()
        this.mDescriptionPaint!!.setAntiAlias(true)
        this.mDescriptionPaint!!.setTextSize(fontSizePixels.toFloat())
        this.mTitlePaint = Paint()
        this.mTitlePaint!!.setTextSize(fontSizePixels.toFloat())
        this.mTitlePaint!!.setFakeBoldText(true)
        this.mTitlePaint!!.setAntiAlias(true)
    }


    // ===========================================================
    // Getter & Setter
    // ===========================================================
    /**
     * default is 3 pixels
     *
     * @param value
     */
    fun setDescriptionBoxPadding(value: Int) {
        DESCRIPTION_BOX_PADDING = value
    }

    /**
     * default 3
     *
     * @param value
     */
    fun setDescriptionBoxCornerWidth(value: Int) {
        DESCRIPTION_BOX_CORNERWIDTH = value
    }

    /**
     * default is 2
     *
     * @param value
     */
    fun setDescriptionTitleExtraLineHeight(value: Int) {
        DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = value
    }

    /**
     * default is a green like color
     *
     * @param value
     */
    fun setMarkerBackgroundColor(value: Int) {
        mMarkerFocusedBackgroundColor = value
    }

    fun setMarkerTitleForegroundColor(value: Int) {
        mTitlePaint!!.setColor(value)
    }

    fun setMarkerDescriptionForegroundColor(value: Int) {
        mDescriptionPaint!!.setColor(value)
    }

    /**
     * default is 14
     *
     * @param value
     */
    fun setFontSize(value: Int) {
        FONT_SIZE_DP = value
        calculateDrawSettings()
    }

    /**
     * in pixels, default is 600
     *
     * @param value
     */
    fun setDescriptionMaxWidth(value: Int) {
        DESCRIPTION_MAXWIDTH = value
        calculateDrawSettings()
    }

    /**
     * default is 30
     *
     * @param value
     */
    fun setDescriptionLineHeight(value: Int) {
        DESCRIPTION_LINE_HEIGHT = value
        calculateDrawSettings()
    }

    val focusedItem: Item?
        get() {
            if (this.mFocusedItemIndex == OverlayConstants.Companion.NOT_SET) {
                return null
            }
            return this.mItemList?.get(this.mFocusedItemIndex)
        }

    fun setFocusedItem(pIndex: Int) {
        this.mFocusedItemIndex = pIndex
    }

    fun unSetFocusedItem() {
        this.mFocusedItemIndex = OverlayConstants.Companion.NOT_SET
    }

    fun setFocusedItem(pItem: Item?) {
        val indexFound = super.mItemList?.indexOf(pItem) ?: -1
        require(indexFound >= 0)

        this.setFocusedItem(indexFound)
    }

    fun setFocusItemsOnTap(doit: Boolean) {
        this.mFocusItemsOnTap = doit
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun onSingleTapUpHelper(index: Int, item: Item?, mapView: MapView?): Boolean {
        if (this.mFocusItemsOnTap) {
            this.mFocusedItemIndex = index
            mapView?.postInvalidate()
        }
        return this.mOnItemGestureListener?.onItemSingleTapUp(index, item) ?: false
    }

    private val mRect = Rect()

    init {
        mContext = pContext
        if (pMarkerFocused == null) {
            this.mMarkerFocusedBase = boundToHotspot(
                pContext.getResources().getDrawable(R.drawable.marker_default_focused_base),
                HotspotPlace.BOTTOM_CENTER
            )
        } else this.mMarkerFocusedBase = pMarkerFocused

        this.mMarkerFocusedBackgroundColor = if (pFocusedBackgroundColor != OverlayConstants.Companion.NOT_SET)
            pFocusedBackgroundColor
        else
            DEFAULTMARKER_BACKGROUNDCOLOR

        calculateDrawSettings()

        this.unSetFocusedItem()
    }

    override fun draw(c: Canvas, pProjection: Projection) {
        super.draw(c, pProjection)

        if (this.mFocusedItemIndex == OverlayConstants.Companion.NOT_SET) {
            return
        }

        // this happens during shutdown
        val itemList = super.mItemList ?: return
        // get focused item's preferred marker & hotspot
        val focusedItem = itemList.get(this.mFocusedItemIndex)
        var markerFocusedBase = focusedItem!!.getMarker(OverlayItem.Companion.ITEM_STATE_FOCUSED_MASK)
        if (markerFocusedBase == null) {
            markerFocusedBase = this.mMarkerFocusedBase
        }

        /* Calculate and set the bounds of the marker. */
        pProjection.toPixels(focusedItem.getPoint(), mFocusedScreenCoords)

        markerFocusedBase!!.copyBounds(mRect)
        mRect.offset(mFocusedScreenCoords.x, mFocusedScreenCoords.y)

        /* Strings of the OverlayItem, we need. */
        val itemTitle = (if (focusedItem.getTitle() == null) UNKNOWN else focusedItem
            .getTitle())!!
        val itemDescription = (if (focusedItem.getSnippet() == null) UNKNOWN else focusedItem
            .getSnippet())!!

        /*
         * Store the width needed for each char in the description to a float array. This is pretty
         * efficient.
         */
        val widths = FloatArray(itemDescription.length)
        this.mDescriptionPaint!!.getTextWidths(itemDescription, widths)

        val sb = StringBuilder()
        var maxWidth = 0
        var curLineWidth = 0
        var lastStop = 0
        var i: Int
        var lastwhitespace = 0
        /*
         * Loop through the charwidth array and harshly insert a linebreak, when the width gets
         * bigger than DESCRIPTION_MAXWIDTH.
         */
        i = 0
        while (i < widths.size) {
            if (!Character.isLetter(itemDescription.get(i))) {
                lastwhitespace = i
            }

            val charwidth = widths[i]

            if (itemDescription.get(i) == '\n') {
                sb.append(itemDescription.subSequence(lastStop, i + 1))
                lastStop = i + 1
                maxWidth = max(maxWidth, curLineWidth)
                curLineWidth = 0
                lastwhitespace = lastStop
                i++
                continue
            } else if (curLineWidth + charwidth > DESCRIPTION_MAXWIDTH) {
                val noSpace = lastStop == lastwhitespace
                if (!noSpace) {
                    i = lastwhitespace
                }

                sb.append(itemDescription.subSequence(lastStop, i))
                sb.append('\n')

                lastStop = i
                maxWidth = max(maxWidth, curLineWidth)
                curLineWidth = 0
                lastwhitespace = lastStop
                if (noSpace) {
                    i--
                    i++
                    continue
                }
            }

            curLineWidth = (curLineWidth + charwidth).toInt()
            i++
        }
        /* Add the last line to the rest to the buffer. */
        if (i != lastStop) {
            val rest = itemDescription.substring(lastStop, i)
            maxWidth = max(maxWidth, this.mDescriptionPaint!!.measureText(rest).toInt())
            sb.append(rest)
        }
        val lines: Array<String?> = sb.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        /*
         * The title also needs to be taken into consideration for the width calculation.
         */
        val titleWidth = this.mDescriptionPaint!!.measureText(itemTitle).toInt()

        maxWidth = max(maxWidth, titleWidth)
        val descWidth = min(maxWidth, DESCRIPTION_MAXWIDTH)

        /* Calculate the bounds of the Description box that needs to be drawn. */
        val descBoxLeft = (mRect.left - descWidth / 2 - DESCRIPTION_BOX_PADDING
                + mRect.width() / 2)
        val descBoxRight = descBoxLeft + descWidth + 2 * DESCRIPTION_BOX_PADDING
        val descBoxBottom = mRect.top
        val descBoxTop = (descBoxBottom - DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT
                - (lines.size + 1) * DESCRIPTION_LINE_HEIGHT /* +1 because of the title. */ - 2 * DESCRIPTION_BOX_PADDING)

        if (pProjection.orientation != 0f) {
            c.save()
            c.rotate(-pProjection.orientation, mFocusedScreenCoords.x.toFloat(), mFocusedScreenCoords.y.toFloat())
        }

        /* Twice draw a RoundRect, once in black with 1px as a small border. */
        this.mMarkerBackgroundPaint!!.setColor(Color.BLACK)
        c.drawRoundRect(
            RectF(
                (descBoxLeft - 1).toFloat(), (descBoxTop - 1).toFloat(), (descBoxRight + 1).toFloat(),
                (descBoxBottom + 1).toFloat()
            ), DESCRIPTION_BOX_CORNERWIDTH.toFloat(), DESCRIPTION_BOX_CORNERWIDTH.toFloat(),
            this.mDescriptionPaint!!
        )
        this.mMarkerBackgroundPaint!!.setColor(this.mMarkerFocusedBackgroundColor)
        c.drawRoundRect(
            RectF(descBoxLeft.toFloat(), descBoxTop.toFloat(), descBoxRight.toFloat(), descBoxBottom.toFloat()),
            DESCRIPTION_BOX_CORNERWIDTH.toFloat(), DESCRIPTION_BOX_CORNERWIDTH.toFloat(),
            this.mMarkerBackgroundPaint!!
        )

        val descLeft = descBoxLeft + DESCRIPTION_BOX_PADDING
        var descTextLineBottom = descBoxBottom - DESCRIPTION_BOX_PADDING

        /* Draw all the lines of the description. */
        for (j in lines.indices.reversed()) {
            c.drawText(lines[j]!!.trim { it <= ' ' }, descLeft.toFloat(), descTextLineBottom.toFloat(), this.mDescriptionPaint!!)
            descTextLineBottom -= DESCRIPTION_LINE_HEIGHT
        }
        /* Draw the title. */
        c.drawText(
            itemTitle, descLeft.toFloat(), (descTextLineBottom - DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT).toFloat(),
            this.mTitlePaint!!
        )
        c.drawLine(
            descBoxLeft.toFloat(), descTextLineBottom.toFloat(), descBoxRight.toFloat(), descTextLineBottom.toFloat(),
            mDescriptionPaint!!
        )

        /*
         * Finally draw the marker base. This is done in the end to make it look better.
         */
        markerFocusedBase.setBounds(mRect)
        markerFocusedBase.draw(c)
        mRect.offset(-mFocusedScreenCoords.x, -mFocusedScreenCoords.y)
        markerFocusedBase.setBounds(mRect)

        if (pProjection.orientation != 0f) {
            c.restore()
        }
    }

    override fun onDetach(mapView: MapView?) {
        super.onDetach(mapView)
        this.mContext = null
    } // ===========================================================
    // Methods
    // ===========================================================
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
