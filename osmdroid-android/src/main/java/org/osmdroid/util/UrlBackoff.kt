package org.osmdroid.util

class UrlBackoff {
    private var mExponentialBackoffDurationInMillis: LongArray? = mExponentialBackoffDurationInMillisDefault
    private val mDelays: MutableMap<String?, Delay?> = HashMap<String?, Delay?>()

    fun next(pUrl: String?) {
        var delay: Delay?
        synchronized(mDelays) {
            delay = mDelays.get(pUrl)
        }
        if (delay == null) {
            delay = Delay(mExponentialBackoffDurationInMillis!!)
            synchronized(mDelays) {
                mDelays.put(pUrl, delay)
            }
        } else {
            delay!!.next()
        }
    }

    fun remove(pUrl: String?): Delay? {
        synchronized(mDelays) {
            return mDelays.remove(pUrl)
        }
    }

    fun shouldWait(pUrl: String?): Boolean {
        val delay: Delay?
        synchronized(mDelays) {
            delay = mDelays.get(pUrl)
        }
        return delay != null && delay.shouldWait()
    }

    fun clear() {
        synchronized(mDelays) {
            mDelays.clear()
        }
    }

    fun setExponentialBackoffDurationInMillis(pExponentialBackoffDurationInMillis: LongArray?) {
        mExponentialBackoffDurationInMillis = pExponentialBackoffDurationInMillis
    }

    companion object {
        private val mExponentialBackoffDurationInMillisDefault = longArrayOf(
            5000, 15000, 60000, 120000, 300000
        )
    }
}
