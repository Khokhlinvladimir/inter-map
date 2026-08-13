package org.osmdroid.samplefragments.tilesources

/**
 * Offline Second demo
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleOfflineSecond : SampleOfflinePriority() {
    override val isOfflineFirst: Boolean
        get() = false
}
