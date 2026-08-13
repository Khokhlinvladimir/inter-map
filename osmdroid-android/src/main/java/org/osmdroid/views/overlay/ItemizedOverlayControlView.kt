// Created by plusminus on 22:59:38 - 12.09.2008
package org.osmdroid.views.overlay

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.osmdroid.library.R


class ItemizedOverlayControlView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    protected var mPreviousButton: ImageButton
    protected var mNextButton: ImageButton
    protected var mCenterToButton: ImageButton
    protected var mNavToButton: ImageButton

    protected var mLis: ItemizedOverlayControlViewListener? = null

    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        this.mPreviousButton = ImageButton(context)
        this.mPreviousButton
            .setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_osm_arrow_back))

        this.mNextButton = ImageButton(context)
        this.mNextButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_osm_arrow_forward))

        this.mCenterToButton = ImageButton(context)
        this.mCenterToButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_osm_center_focus))

        this.mNavToButton = ImageButton(context)
        this.mNavToButton
            .setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_osm_navigation))

        mPreviousButton.contentDescription = "Previous item"
        mNextButton.contentDescription = "Next item"
        mCenterToButton.contentDescription = "Center item on map"
        mNavToButton.contentDescription = "Navigate to item"

        this.addView(
            mPreviousButton, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )
        this.addView(
            mCenterToButton, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )
        this.addView(
            mNavToButton, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )
        this.addView(
            mNextButton, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        initViewListeners()
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    fun setItemizedOverlayControlViewListener(lis: ItemizedOverlayControlViewListener?) {
        this.mLis = lis
    }

    fun setNextEnabled(pEnabled: Boolean) {
        this.mNextButton.setEnabled(pEnabled)
    }

    fun setPreviousEnabled(pEnabled: Boolean) {
        this.mPreviousButton.setEnabled(pEnabled)
    }

    fun setNavToVisible(pVisibility: Int) {
        this.mNavToButton.setVisibility(pVisibility)
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    private fun initViewListeners() {
        this.mNextButton.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                if (this@ItemizedOverlayControlView.mLis != null) this@ItemizedOverlayControlView.mLis!!.onNext()
            }
        })

        this.mPreviousButton.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                if (this@ItemizedOverlayControlView.mLis != null) this@ItemizedOverlayControlView.mLis!!.onPrevious()
            }
        })

        this.mCenterToButton.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                if (this@ItemizedOverlayControlView.mLis != null) this@ItemizedOverlayControlView.mLis!!.onCenter()
            }
        })

        this.mNavToButton.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                if (this@ItemizedOverlayControlView.mLis != null) this@ItemizedOverlayControlView.mLis!!.onNavTo()
            }
        })
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    interface ItemizedOverlayControlViewListener {
        fun onPrevious()

        fun onNext()

        fun onCenter()

        fun onNavTo()
    }
}
