package tests

import junit.framework.Assert
import org.junit.Test
import org.nocrala.tools.gis.data.esri.shapefile.util.BAUtil.displayByteArray
import org.nocrala.tools.gis.data.esri.shapefile.util.DoubleSerializer.deserializeBigEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.DoubleSerializer.deserializeLittleEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.DoubleSerializer.serializeBigEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.DoubleSerializer.serializeLittleEndian
import org.nocrala.tools.gis.data.esri.shapefile.util.HexaUtil.byteArrayToString
import org.nocrala.tools.gis.data.esri.shapefile.util.HexaUtil.stringToByteArray
import java.nio.ByteBuffer

class DoubleTests {
    @Test
    fun testBigEndian() {
        assertBe(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0), 0.0)
        assertBe(byteArrayOf(63, -16, 0, 0, 0, 0, 0, 0), 1.0)
        assertBe(byteArrayOf(64, 0, 0, 0, 0, 0, 0, 0), 2.0)
        assertBe(byteArrayOf(-65, -16, 0, 0, 0, 0, 0, 0), -1.0)

        assertBe(byteArrayOf(63, -32, 0, 0, 0, 0, 0, 0), 0.5)
        assertBe(byteArrayOf(-65, -32, 0, 0, 0, 0, 0, 0), -0.5)

        assertBe(byteArrayOf(64, 36, 0, 0, 0, 0, 0, 0), 10.0)

        assertBe(byteArrayOf(64, 95, -64, 0, 0, 0, 0, 0), 127.0)
        assertBe(byteArrayOf(64, 96, 0, 0, 0, 0, 0, 0), 128.0)

        assertBe(byteArrayOf(64, 111, -32, 0, 0, 0, 0, 0), 255.0)

        assertBe(byteArrayOf(64, 112, 0, 0, 0, 0, 0, 0), 256.0)
        assertBe(byteArrayOf(64, 112, 16, 0, 0, 0, 0, 0), 257.0)

        assertBe(byteArrayOf(65, -33, -1, -1, -1, -64, 0, 0), Int.Companion.MAX_VALUE.toDouble())
        assertBe(byteArrayOf(-63, -32, 0, 0, 0, 0, 0, 0), Int.Companion.MIN_VALUE.toDouble())

        assertBe(byteArrayOf(-65, -16, 0, 0, 0, 0, 0, 0), -1.0)

        assertBe(byteArrayOf(-64, 112, 0, 0, 0, 0, 0, 0), -256.0)
        assertBe(byteArrayOf(-64, 111, -32, 0, 0, 0, 0, 0), -255.0)

        // Special deserialize cases
        try {
            Assert.assertEquals(0, deserBe(null))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        try {
            Assert.assertEquals(0, deserBe(byteArrayOf(0, 0, 0)))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        Assert.assertEquals(0.0, deserBe(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun testLittleEndian() {
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0), 0.0)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, -16, 63), 1.0)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 64), 2.0)

        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, -16, -65), -1.0)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, -32, 63), 0.5)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, -32, -65), -0.5)

        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, 36, 64), 10.0)

        assertLe(byteArrayOf(0, 0, 0, 0, 0, -64, 95, 64), 127.0)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, 96, 64), 128.0)

        assertLe(byteArrayOf(0, 0, 0, 0, 0, -32, 111, 64), 255.0)

        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, 112, 64), 256.0)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 16, 112, 64), 257.0)

        assertLe(byteArrayOf(0, 0, -64, -1, -1, -1, -33, 65), Int.Companion.MAX_VALUE.toDouble())
        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, -32, -63), Int.Companion.MIN_VALUE.toDouble())

        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, -16, -65), -1.0)

        assertLe(byteArrayOf(0, 0, 0, 0, 0, 0, 112, -64), -256.0)
        assertLe(byteArrayOf(0, 0, 0, 0, 0, -32, 111, -64), -255.0)

        assertLe("A5 3E BE E9 FF 7F 66 C0", -179.999989387104, 0.000000001)

        // Special deserialize cases
        try {
            Assert.assertEquals(0, deserLe(null))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        try {
            Assert.assertEquals(0, deserLe(byteArrayOf(0, 0, 0)))
            Assert.fail()
        } catch (e: Exception) {
            // ok
        }

        Assert.assertEquals(0.0, deserLe(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)))
    }

    // Utils Be
    private fun serBe(value: Double): ByteArray {
        val result = ByteArray(8)
        serializeBigEndian(value, ByteBuffer.wrap(result))
        return result
    }

    private fun deserBe(b: ByteArray?): Double {
        return deserializeBigEndian(ByteBuffer.wrap(b!!))
    }

    private fun assertBe(serialized: ByteArray, value: Double) {
        Assert.assertTrue(equals(serialized, serBe(value)))
        Assert.assertEquals(value, deserBe(serialized))
    }

    // Utils Le
    private fun serLe(value: Double): ByteArray {
        val result = ByteArray(8)
        serializeLittleEndian(value, ByteBuffer.wrap(result))
        return result
    }

    private fun deserLe(b: ByteArray?): Double {
        return deserializeLittleEndian(ByteBuffer.wrap(b!!))
    }

    private fun assertLe(serialized: ByteArray, value: Double) {
        Assert.assertTrue(equals(serialized, serLe(value)))
        Assert.assertEquals(value, deserLe(serialized))
    }

    private fun assertLe(
        hexaSerialized: String, value: Double,
        delta: Double
    ) {
        val serialized = stringToByteArray(hexaSerialized)
        display(hexaSerialized + " --> " + byteArrayToString(serialized))
        Assert.assertEquals(value, deserLe(serialized), delta)
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

    companion object {
        private fun display(txt: String?) {
            // System.out.println(txt);
        }
    }
}
