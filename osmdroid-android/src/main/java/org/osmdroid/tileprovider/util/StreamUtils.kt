// Created by plusminus on 19:14:08 - 20.10.2008
package org.osmdroid.tileprovider.util

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * don't use android classes here, since this class is used outside of android
 */
class StreamUtils private constructor() {
    companion object {
    // ===========================================================
    // Constants
    // ===========================================================
    const val IO_BUFFER_SIZE: Int = 8 * 1024

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    /**
     * Copy the content of the input stream into the output stream, using a temporary byte array
     * buffer whose size is defined by [.IO_BUFFER_SIZE].
     *
     * @param in  The input stream to copy from.
     * @param out The output stream to copy to.
     * @return the total length copied
     * @throws IOException If any error occurs during the copy.
     */
    @Throws(IOException::class)
    @JvmStatic
    fun copy(`in`: InputStream, out: OutputStream): Long {
        var length: Long = 0
        val b = ByteArray(IO_BUFFER_SIZE)
        var read: Int
        while ((`in`.read(b).also { read = it }) != -1) {
            out.write(b, 0, read)
            length += read.toLong()
        }
        return length
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    @JvmStatic
    fun closeStream(stream: Closeable?) {
        if (stream != null) {
            try {
                stream.close()
            } catch (e: IOException) {
                //don't use android classes here, since this class is used outside of android
                e.printStackTrace()
            }
        }
    }
    }
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
