package org.osmdroid.views.overlay.simplefastpoint

import android.graphics.Color
import android.graphics.Paint

/**
 * Options for SimpleFastPointOverlay.
 * Created by Miguel Porto on 25-10-2016.
 */
open class SimpleFastPointOverlayOptions {
    enum class RenderingAlgorithm {
        NO_OPTIMIZATION, MEDIUM_OPTIMIZATION, MAXIMUM_OPTIMIZATION
    }

    enum class Shape {
        CIRCLE, SQUARE
    }

    enum class LabelPolicy {
        ZOOM_THRESHOLD, DENSITY_THRESHOLD
    }

    @JvmField protected var mPointStyle: Paint
    @JvmField protected var mSelectedPointStyle: Paint
    @JvmField protected var mTextStyle: Paint
    @JvmField protected var mCircleRadius: Float = 5f
    @JvmField protected var mSelectedCircleRadius: Float = 13f
    @JvmField protected var mClickable: Boolean = true
    @JvmField protected var mCellSize: Int = 10
    @JvmField protected var mAlgorithm: RenderingAlgorithm? = RenderingAlgorithm.MAXIMUM_OPTIMIZATION
    @JvmField protected var mSymbol: Shape? = Shape.SQUARE
    @JvmField protected var mLabelPolicy: LabelPolicy? = LabelPolicy.ZOOM_THRESHOLD
    @JvmField protected var mMaxNShownLabels: Int = 250
    @JvmField protected var mMinZoomShowLabels: Int = 11

    val pointStyle: Paint get() = mPointStyle
    val selectedPointStyle: Paint get() = mSelectedPointStyle
    val textStyle: Paint get() = mTextStyle
    val circleRadius: Float get() = mCircleRadius
    val selectedCircleRadius: Float get() = mSelectedCircleRadius
    val isClickable: Boolean get() = mClickable
    val cellSize: Int get() = mCellSize
    val algorithm: RenderingAlgorithm? get() = mAlgorithm
    val symbol: Shape? get() = mSymbol
    val labelPolicy: LabelPolicy? get() = mLabelPolicy
    val maxNShownLabels: Int get() = mMaxNShownLabels
    val minZoomShowLabels: Int get() = mMinZoomShowLabels

    init {
        this.mPointStyle = Paint()
        mPointStyle.setStyle(Paint.Style.FILL)
        mPointStyle.setColor(Color.parseColor("#ff7700"))

        this.mSelectedPointStyle = Paint()
        mSelectedPointStyle.setStrokeWidth(5f)
        mSelectedPointStyle.setStyle(Paint.Style.STROKE)
        mSelectedPointStyle.setColor(Color.parseColor("#ffff00"))

        this.mTextStyle = Paint()
        mTextStyle.setStyle(Paint.Style.FILL)
        mTextStyle.setColor(Color.parseColor("#ffff00"))
        mTextStyle.setTextAlign(Paint.Align.CENTER)
        mTextStyle.setTextSize(24f)
    }

    /**
     * Sets the style for the point overlay, which is applied to all circles.
     * If the layer is individually styled, the individual style overrides this.
     *
     * @param style A Paint object.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setPointStyle(style: Paint): SimpleFastPointOverlayOptions {
        this.mPointStyle = style
        return this
    }

    /**
     * Sets the style for the selected point.
     *
     * @param style A Paint object.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setSelectedPointStyle(style: Paint): SimpleFastPointOverlayOptions {
        this.mSelectedPointStyle = style
        return this
    }

    /**
     * Sets the radius of the circles to be drawn.
     *
     * @param radius Radius.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setRadius(radius: Float): SimpleFastPointOverlayOptions {
        this.mCircleRadius = radius
        return this
    }

    /**
     * Sets the radius of the selected point's circle.
     *
     * @param radius Radius.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setSelectedRadius(radius: Float): SimpleFastPointOverlayOptions {
        this.mSelectedCircleRadius = radius
        return this
    }

    /**
     * Sets whether this overlay is clickable or not. A clickable overlay will automatically select
     * the nearest point.
     *
     * @param clickable True or false.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setIsClickable(clickable: Boolean): SimpleFastPointOverlayOptions {
        this.mClickable = clickable
        return this
    }

    /**
     * Sets the grid cell size used for indexing, in pixels. Larger cells result in faster rendering
     * speed, but worse fidelity. Default is 10 pixels, for large datasets (>10k points), use 15.
     *
     * @param cellSize The cell size in pixels.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setCellSize(cellSize: Int): SimpleFastPointOverlayOptions {
        this.mCellSize = cellSize
        return this
    }

    /**
     * Sets the rendering algorithm. There are three options:
     * NO_OPTIMIZATION: Slowest option. Draw all points on each draw event.
     * MEDIUM_OPTIMIZATION: Faster. Recalculates the grid index on each draw event.
     * Not recommended for >10k points. Better UX, but may be choppier.
     * MAXIMUM_OPTIMIZATION: Fastest. Only recalculates the grid on touch up and animation end
     * , hence much faster display on move. Recommended for >10k points.
     *
     * @param algorithm A [RenderingAlgorithm].
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setAlgorithm(algorithm: RenderingAlgorithm?): SimpleFastPointOverlayOptions {
        this.mAlgorithm = algorithm
        return this
    }

    /**
     * Sets the symbol shape for this layer. Hint: circle shape is less performant, avoid for large N.
     *
     * @param symbol The symbol, currently CIRCLE or SQUARE.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setSymbol(symbol: Shape?): SimpleFastPointOverlayOptions {
        this.mSymbol = symbol
        return this
    }

    /**
     * Sets the style for the labels.
     * If the layer is individually styled, the individual style overrides this.
     *
     * @param textStyle The style.
     * @return The updated [SimpleFastPointOverlayOptions]
     */
    fun setTextStyle(textStyle: Paint): SimpleFastPointOverlayOptions {
        this.mTextStyle = textStyle
        return this
    }

    /**
     * Sets the minimum zoom level at which the labels should be drawn. This option is
     * **ignored** if LabelPolicy is DENSITY_THRESHOLD.
     *
     * @param minZoomShowLabels The zoom level.
     * @return
     */
    fun setMinZoomShowLabels(minZoomShowLabels: Int): SimpleFastPointOverlayOptions {
        this.mMinZoomShowLabels = minZoomShowLabels
        return this
    }

    /**
     * Sets the threshold (nr. of visible points) after which labels will not be drawn. **This
     * option only works when LabelPolicy is DENSITY_THRESHOLD and the algorithm is
     * MAXIMUM_OPTIMIZATION**.
     *
     * @param maxNShownLabels The maximum number of visible points
     * @return
     */
    fun setMaxNShownLabels(maxNShownLabels: Int): SimpleFastPointOverlayOptions {
        this.mMaxNShownLabels = maxNShownLabels
        return this
    }

    /**
     * Sets the policy for displaying point labels. Can be:<br></br>
     * ZOOM_THRESHOLD: Labels are not displayed is current map zoom level is lower than
     * `MinZoomShowLabels`
     * DENSITY_THRESHOLD: Labels are not displayed when the number of visible points is larger
     * than `MaxNShownLabels`. **This only works for MAXIMUM_OPTIMIZATION**<br></br>
     *
     * @param labelPolicy One of `ZOOM_THRESHOLD` or `DENSITY_THRESHOLD`
     * @return
     */
    fun setLabelPolicy(labelPolicy: LabelPolicy?): SimpleFastPointOverlayOptions {
        this.mLabelPolicy = labelPolicy
        return this
    }

    companion object {
        @get:JvmStatic
        val defaultStyle: SimpleFastPointOverlayOptions
            /**
             * Creates a new [SimpleFastPointOverlayOptions] object with default options.
             *
             * @return [SimpleFastPointOverlayOptions]
             */
            get() = SimpleFastPointOverlayOptions()
    }
}
