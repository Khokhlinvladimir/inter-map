package org.osmdroid.tileprovider

import android.graphics.drawable.Drawable
import org.osmdroid.views.overlay.TilesOverlay

/**
 * To be used by some kind of [TilesOverlay], in order to get a count of the tiles, by state
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class TileStates {
    val runAfters: MutableCollection<Runnable?> = LinkedHashSet<Runnable?>()
    var isDone: Boolean = false
        private set
    var total: Int = 0
        private set
    var upToDate: Int = 0
        private set
    var expired: Int = 0
        private set
    var scaled: Int = 0
        private set
    var notFound: Int = 0
        private set

    fun initialiseLoop() {
        this.isDone = false
        this.total = 0
        this.upToDate = 0
        this.expired = 0
        this.scaled = 0
        this.notFound = 0
    }

    fun finaliseLoop() {
        this.isDone = true
        for (runnable in this.runAfters) {
            if (runnable != null) {
                runnable.run()
            }
        }
    }

    fun handleTile(pDrawable: Drawable?) {
        this.total++
        if (pDrawable == null) {
            this.notFound++
        } else {
            val state: Int = ExpirableBitmapDrawable.Companion.getState(pDrawable)
            when (state) {
                ExpirableBitmapDrawable.Companion.UP_TO_DATE -> this.upToDate++
                ExpirableBitmapDrawable.Companion.EXPIRED -> this.expired++
                ExpirableBitmapDrawable.Companion.SCALED -> this.scaled++
                ExpirableBitmapDrawable.Companion.NOT_FOUND -> this.notFound++
                else -> throw IllegalArgumentException("Unknown state: " + state)
            }
        }
    }

    override fun toString(): String {
        if (this.isDone) {
            return ("TileStates: " + this.total
                    + " = " + this.upToDate + "(U)"
                    + " + " + this.expired + "(E)"
                    + " + " + this.scaled + "(S)"
                    + " + " + this.notFound + "(N)")
        }
        return "TileStates"
    }
}
