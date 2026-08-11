package org.nocrala.tools.gis.data.esri.shapefile.util

import org.nocrala.tools.gis.data.esri.shapefile.exception.DataStreamEOFException
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

object ISUtil {
    private val BUFFER = ByteArray(8)
    private val BYTE_BUFFER: ByteBuffer = ByteBuffer.wrap(BUFFER)

    // Big endian int
    @Synchronized
    @Throws(DataStreamEOFException::class, IOException::class)
    fun readBeIntMaybeEOF(`is`: InputStream): Int {
        readIntoBufferMaybeEOF(`is`, 4)
        return IntSerializer.deserializeBigEndian(BYTE_BUFFER)
    }

    @Synchronized
    @Throws(IOException::class)
    fun readBeInt(`is`: InputStream): Int {
        readIntoBuffer(`is`, 4)
        return IntSerializer.deserializeBigEndian(BYTE_BUFFER)
    }

    // Big endian double
    @Synchronized
    @Throws(DataStreamEOFException::class, IOException::class)
    fun readBeDoubleMaybeEOF(`is`: InputStream): Double {
        readIntoBufferMaybeEOF(`is`, 8)
        return DoubleSerializer.deserializeBigEndian(BYTE_BUFFER)
    }

    @Synchronized
    @Throws(IOException::class)
    fun readBeDouble(`is`: InputStream): Double {
        readIntoBuffer(`is`, 8)
        return DoubleSerializer.deserializeBigEndian(BYTE_BUFFER)
    }

    // Little endian int
    @Synchronized
    @Throws(DataStreamEOFException::class, IOException::class)
    fun readLeIntMaybeEOF(`is`: InputStream): Int {
        readIntoBufferMaybeEOF(`is`, 4)
        return IntSerializer.deserializeLittleEndian(BYTE_BUFFER)
    }

    @Synchronized
    @Throws(IOException::class)
    fun readLeInt(`is`: InputStream): Int {
        readIntoBuffer(`is`, 4)
        // System.out.println("--> " + HexaUtil.byteArrayToString(BUFFER));
        return IntSerializer.deserializeLittleEndian(BYTE_BUFFER)
    }

    // Little endian double
    @Synchronized
    @Throws(DataStreamEOFException::class, IOException::class)
    fun readLeDoubleMaybeEOF(`is`: InputStream): Double {
        readIntoBufferMaybeEOF(`is`, 8)
        return DoubleSerializer.deserializeLittleEndian(BYTE_BUFFER)
    }

    @Synchronized
    @Throws(IOException::class)
    fun readLeDouble(`is`: InputStream): Double {
        readIntoBuffer(`is`, 8)
        return DoubleSerializer.deserializeLittleEndian(BYTE_BUFFER)
    }

    // Utils
    @Throws(DataStreamEOFException::class, IOException::class)
    private fun readIntoBufferMaybeEOF(
        `is`: InputStream,
        length: Int
    ) {
        try {
            val read = `is`.read(BUFFER, 0, length)
            if (read != length) {
                throw DataStreamEOFException()
            }
        } catch (e: EOFException) {
            throw DataStreamEOFException()
        }
    }

    @Throws(IOException::class)
    private fun readIntoBuffer(`is`: InputStream, length: Int) {
        val read = `is`.read(BUFFER, 0, length)
        if (read != length) {
            throw EOFException()
        }
    }
}
