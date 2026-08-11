package org.osmdroid.tileprovider.modules

import java.util.concurrent.ThreadFactory

/**
 * @author Jastrzab
 */
class ConfigurablePriorityThreadFactory(pPriority: Int, pName: String?) : ThreadFactory {
    private val mPriority: Int
    private val mName: String?

    init {
        mPriority = pPriority
        mName = pName
    }

    override fun newThread(pRunnable: Runnable?): Thread {
        val thread = Thread(pRunnable)
        thread.setPriority(mPriority)
        if (mName != null) {
            thread.setName(mName)
        }
        return thread
    }
}
