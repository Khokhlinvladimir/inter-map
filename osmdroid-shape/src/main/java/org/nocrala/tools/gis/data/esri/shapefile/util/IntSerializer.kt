package org.nocrala.tools.gis.data.esri.shapefile.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

object IntSerializer {
    private const val BYTE_ARRAY_SIZE = 4

    @JvmStatic
    fun deserializeBigEndian(b: ByteBuffer): Int {
        if (b == null) {
            throw RuntimeException("Cannot deserialize null byte buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot deserialize. Byte buffer must have at least "
                        + BYTE_ARRAY_SIZE + " bytes.")
            )
        }
        // BAUtil.displayByteArray("deserializeBigEndian():", b.array());
        b.order(ByteOrder.BIG_ENDIAN)
        b.position(0)
        return b.getInt()
    }

    @JvmStatic
    fun deserializeLittleEndian(b: ByteBuffer): Int {
        if (b == null) {
            throw RuntimeException("Cannot deserialize null byte buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot deserialize. Byte buffer must have at least "
                        + BYTE_ARRAY_SIZE + " bytes.")
            )
        }
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.position(0)
        return b.getInt()
    }

    @JvmStatic
    fun serializeBigEndian(value: Int, b: ByteBuffer) {
        if (b == null) {
            throw RuntimeException("Cannot serialize into null byte buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot serialize. Byte buffer must have at least " + BYTE_ARRAY_SIZE
                        + " bytes.")
            )
        }
        b.order(ByteOrder.BIG_ENDIAN)
        b.position(0)
        b.putInt(value)
    }

    @JvmStatic
    fun serializeLittleEndian(value: Int, b: ByteBuffer) {
        if (b == null) {
            throw RuntimeException("Cannot serialize into null byte buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot serialize. Byte buffer must have at least " + BYTE_ARRAY_SIZE
                        + " bytes.")
            )
        }
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.position(0)
        b.putInt(value)
    }
}
