package org.nocrala.tools.gis.data.esri.shapefile.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DoubleSerializer {
    private const val BYTE_ARRAY_SIZE = 8

    @JvmStatic
    fun deserializeBigEndian(b: ByteBuffer): Double {
        if (b == null) {
            throw RuntimeException("Cannot deserialize null byte buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot deserialize. Byte buffer must have at least "
                        + BYTE_ARRAY_SIZE + " bytes.")
            )
        }
        b.order(ByteOrder.BIG_ENDIAN)
        b.position(0)
        return b.getDouble()
    }

    @JvmStatic
    fun deserializeLittleEndian(b: ByteBuffer): Double {
        if (b == null) {
            throw RuntimeException("Cannot deserialize null buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot deserialize. Byte buffer must have at least "
                        + BYTE_ARRAY_SIZE + " bytes.")
            )
        }
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.position(0)
        return b.getDouble()
    }

    @JvmStatic
    fun serializeBigEndian(value: Double, b: ByteBuffer) {
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
        b.putDouble(value)
    }

    @JvmStatic
    fun serializeLittleEndian(
        value: Double,
        b: ByteBuffer
    ) {
        if (b == null) {
            throw RuntimeException("Cannot serialize into a null byte buffer.")
        }
        if (b.array().size < BYTE_ARRAY_SIZE) {
            throw RuntimeException(
                ("Cannot serialize. Byte buffer must have at least " + BYTE_ARRAY_SIZE
                        + " bytes.")
            )
        }
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.position(0)
        b.putDouble(value)
    }
}
