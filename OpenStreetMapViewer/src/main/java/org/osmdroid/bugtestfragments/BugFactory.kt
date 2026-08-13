package org.osmdroid.bugtestfragments

import org.osmdroid.ISampleFactory
import org.osmdroid.samplefragments.BaseSampleFragment


/**
 * Factory for all bug driver classes
 */
class BugFactory private constructor() : ISampleFactory {
    private val mSamples: Array<Class<out BaseSampleFragment>>


    init {
        mSamples = arrayOf(
            Bug82WinDeath::class.java,
            SampleBug57::class.java,
            Bug382Crash::class.java,
            Bug164EndlessOnScolls::class.java,
            Bug419Zoom::class.java,
            Bug445Caching::class.java,
            Bug512Marker::class.java,
            Bug512CacheManagerWp::class.java,
            Bug846InfiniteRedrawLoop::class.java,
            Bug1322::class.java, Issue1444::class.java
        )
    }

    override fun getSample(index: Int): BaseSampleFragment? {
        try {
            return mSamples[index].newInstance()
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
                    _instance = BugFactory()
                }
                return _instance!!
            }
    }
}
