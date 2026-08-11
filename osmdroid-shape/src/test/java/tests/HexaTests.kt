package tests

import junit.framework.Assert
import org.junit.Test
import org.nocrala.tools.gis.data.esri.shapefile.util.HexaUtil.byteArrayToString
import org.nocrala.tools.gis.data.esri.shapefile.util.HexaUtil.stringToByteArray

class HexaTests {
    @Test
    fun testOneByte() {
        val serialized = stringToByteArray("12")
        Assert.assertNotNull(serialized)
        Assert.assertEquals(1, serialized.size)
        Assert.assertEquals(18, serialized[0].toInt())

        val back = byteArrayToString(serialized)
        // System.out.println("back='" + back + "'");
        Assert.assertEquals("12", back)
    }

    @Test
    fun testTwoBytes() {
        val serialized = stringToByteArray("cdef")
        Assert.assertNotNull(serialized)
        Assert.assertEquals(2, serialized.size)
        Assert.assertEquals(205 - 256, serialized[0].toInt())
        Assert.assertEquals(239 - 256, serialized[1].toInt())

        val back = byteArrayToString(serialized)
        // System.out.println("back='" + back + "'");
        Assert.assertEquals("cdef", back)
    }

    @Test
    fun testTwoBytesUppercase() {
        val serialized = stringToByteArray("CDEF")
        Assert.assertNotNull(serialized)
        Assert.assertEquals(2, serialized.size)
        Assert.assertEquals(205 - 256, serialized[0].toInt())
        Assert.assertEquals(239 - 256, serialized[1].toInt())

        val back = byteArrayToString(serialized)
        // System.out.println("back='" + back + "'");
        Assert.assertEquals("cdef", back)
    }
}
