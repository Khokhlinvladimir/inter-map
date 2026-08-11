package tests

import junit.framework.Assert
import org.junit.Test
import org.nocrala.tools.gis.data.esri.shapefile.util.BAUtil.displayByteArray
import org.nocrala.tools.gis.data.esri.shapefile.util.IntSerializer.deserializeBigEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.IntSerializer.deserializeLittleEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.IntSerializer.serializeBigEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.IntSerializer.serializeLittleEndian
import java.nio.ByteBuffer

class IntTests {
    @Test
    fun testBeInt() {
        assertBe(byteArrayOf(0, 0, 0, 0), 0)
        assertBe(byteArrayOf(0, 0, 0, 1), 1)
        assertBe(byteArrayOf(0, 0, 0, 2), 2)
        assertBe(byteArrayOf(0, 0, 0, 10), 10)

        assertBe(byteArrayOf(0, 0, 0, 127), 127)
        assertBe(byteArrayOf(0, 0, 0, -128), 128)

        assertBe(byteArrayOf(0, 0, 0, -1), 255)

        assertBe(byteArrayOf(0, 0, 1, 0), 256)
        assertBe(byteArrayOf(0, 0, 1, 1), 257)

        assertBe(byteArrayOf(127, -1, -1, -1), Int.Companion.MAX_VALUE)
        assertBe(byteArrayOf(-128, 0, 0, 0), Int.Companion.MIN_VALUE)

        assertBe(byteArrayOf(-1, -1, -1, -1), -1)

        assertBe(byteArrayOf(-1, -1, -1, 0), -256)
        assertBe(byteArrayOf(-1, -1, -1, 1), -255)

        // Special deserialize cases
        try {
            Assert.assertEquals(0, deserBeInt(null))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        try {
            Assert.assertEquals(0, deserBeInt(byteArrayOf(0, 0, 0)))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        Assert.assertEquals(0, deserBeInt(byteArrayOf(0, 0, 0, 0, 0)))
    }

    @Test
    fun testLeInt() {
        assertLe(byteArrayOf(0, 0, 0, 0), 0)
        assertLe(byteArrayOf(1, 0, 0, 0), 1)
        assertLe(byteArrayOf(2, 0, 0, 0), 2)
        assertLe(byteArrayOf(10, 0, 0, 0), 10)

        assertLe(byteArrayOf(127, 0, 0, 0), 127)
        assertLe(byteArrayOf(-128, 0, 0, 0), 128)

        assertLe(byteArrayOf(-1, 0, 0, 0), 255)

        assertLe(byteArrayOf(0, 1, 0, 0), 256)
        assertLe(byteArrayOf(1, 1, 0, 0), 257)

        assertLe(byteArrayOf(-1, -1, -1, 127), Int.Companion.MAX_VALUE)
        assertLe(byteArrayOf(0, 0, 0, -128), Int.Companion.MIN_VALUE)

        assertLe(byteArrayOf(-1, -1, -1, -1), -1)

        assertLe(byteArrayOf(0, -1, -1, -1), -256)
        assertLe(byteArrayOf(1, -1, -1, -1), -255)

        // Special deserialize cases
        try {
            Assert.assertEquals(0, deserLeInt(null))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        try {
            Assert.assertEquals(0, deserLeInt(byteArrayOf(0, 0, 0)))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        Assert.assertEquals(0, deserLeInt(byteArrayOf(0, 0, 0, 0, 0)))
    }

    // Utils Be
    private fun serBeInt(value: Int): ByteArray {
        val result = ByteArray(4)
        serializeBigEndian(value, ByteBuffer.wrap(result))
        return result
    }

    private fun deserBeInt(b: ByteArray?): Int {
        return deserializeBigEndian(ByteBuffer.wrap(b!!))
    }

    private fun assertBe(serialized: ByteArray, value: Int) {
        Assert.assertTrue(equals(serialized, serBeInt(value)))
        Assert.assertEquals(value, deserBeInt(serialized))
    }

    // Utils Le
    private fun serLeInt(value: Int): ByteArray {
        val result = ByteArray(4)
        serializeLittleEndian(value, ByteBuffer.wrap(result))
        return result
    }

    private fun deserLeInt(b: ByteArray?): Int {
        return deserializeLittleEndian(ByteBuffer.wrap(b!!))
    }

    private fun assertLe(serialized: ByteArray, value: Int) {
        Assert.assertTrue(equals(serialized, serLeInt(value)))
        Assert.assertEquals(value, deserLeInt(serialized))
    }

    // Helper
    private fun equals(expected: ByteArray?, actual: ByteArray?): Boolean {
        if (expected == null) {
            displayByteArray("null", expected)
            return false
        }
        if (actual == null || actual.size < expected.size) {
            displayByteArray("other", expected)
            return false
        }
        for (i in expected.indices) {
            if (expected[i] != actual[i]) {
                displayByteArray("exp", expected)
                displayByteArray("act", actual)
                return false
            }
        }
        return true
    }
}
