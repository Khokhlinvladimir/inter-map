// Created by plusminus on 9:34:16 PM - Mar 5, 2009
package org.osmdroid.mtp.download

import org.osmdroid.mtp.adt.OSMTileInfo
import org.osmdroid.tileprovider.util.StreamUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.Queue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DownloadManager(private val mBaseURL: String, private val mDestinationURL: String, mThreads: Int) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    private val mThreadPool: ExecutorService

    private val mQueue: Queue<OSMTileInfo?> = LinkedBlockingQueue<OSMTileInfo?>()

    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        this.mThreadPool = Executors.newFixedThreadPool(mThreads)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    @Synchronized
    fun add(pTileInfo: OSMTileInfo?) {
        this.mQueue.add(pTileInfo)
        spawnNewThread()
    }

    @get:Synchronized
    private val next: OSMTileInfo?
        get() {
            val tile: OSMTileInfo? = this.mQueue.poll()

            val remaining = this.mQueue.size
            if (remaining % 10 == 0 && remaining > 0) {
                print("(" + remaining + ")")
            } else {
                print(".")
            }

            (this as Object).notify()
            return tile
        }

    @Synchronized
    @Throws(InterruptedException::class)
    fun waitEmpty() {
        while (this.mQueue.size > 0) {
            (this as Object).wait()
        }
    }

    @Throws(InterruptedException::class)
    fun waitFinished() {
        waitEmpty()
        this.mThreadPool.shutdown()
        this.mThreadPool.awaitTermination(6, TimeUnit.HOURS)
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    private fun spawnNewThread() {
        this.mThreadPool.execute(DownloadRunner())
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    private inner class DownloadRunner : Runnable {
        private var mTileInfo: OSMTileInfo? = null
        private var mDestinationFile: File? = null

        fun init(pTileInfo: OSMTileInfo) {
            this.mTileInfo = pTileInfo
            /* Create destination file. */
            val filename = String.format(this@DownloadManager.mDestinationURL, pTileInfo.zoom, pTileInfo.x, pTileInfo.y)
            this.mDestinationFile = File(filename)

            val parent = this.mDestinationFile!!.getParentFile()
            parent.mkdirs()
        }

        override fun run() {
            var `in`: InputStream? = null
            var out: OutputStream? = null

            val tileInfo = this@DownloadManager.next ?: return
            init(tileInfo)

            if (mDestinationFile!!.exists()) {
                return  // TODO issue 70 - make this an option
            }

            val finalURL = String.format(this@DownloadManager.mBaseURL, tileInfo.zoom, tileInfo.x, tileInfo.y)

            try {
                `in` = BufferedInputStream(URL(finalURL).openStream(), StreamUtils.IO_BUFFER_SIZE)

                val fileOut = FileOutputStream(this.mDestinationFile)
                out = BufferedOutputStream(fileOut, StreamUtils.IO_BUFFER_SIZE)

                StreamUtils.copy(`in`, out)

                out.flush()
            } catch (e: Exception) {
                System.err.println("Error downloading: '" + this.mTileInfo + "' from URL: " + finalURL + " : " + e)
                this@DownloadManager.add(this.mTileInfo) // try again later
            } finally {
                StreamUtils.closeStream(`in`)
                StreamUtils.closeStream(out)
            }
        }
    }
}
