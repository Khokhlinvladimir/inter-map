package org.osmdroid.samplefragments.tilesources

/**
 * Offline First demo
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleOfflineFirst : SampleOfflinePriority() {
    override val isOfflineFirst: Boolean
        get() = true
}
