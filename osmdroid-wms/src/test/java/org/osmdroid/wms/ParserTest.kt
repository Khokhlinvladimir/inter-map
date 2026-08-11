package org.osmdroid.wms

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.osmdroid.wms.WMSParser.parse
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class ParserTest {
    @Test
    @Throws(Exception::class)
    fun testParserGeoserver() {
        val input = File("./src/test/resources/geoserver_getcapabilities_1.1.0.xml")
        if (!input.exists()) {
            Assert.fail(File(".").getAbsolutePath() + " = pwd. target file doesn't exist at " + input.getAbsolutePath())
        }
        val fis = FileInputStream(input)
        val cap = parse(fis)
        fis.close()


        verify(cap)
        Assert.assertTrue(cap.layers!!.size == 22)
        Assert.assertEquals("1.1.1", cap.wmsVersion)
    }

    @Ignore //only ignored to support offline builds
    @Test
    @Throws(Exception::class)
    fun testUSGS() {
        val c =
            URL("https://basemap.nationalmap.gov/arcgis/services/USGSTopo/MapServer/WMSServer?request=GetCapabilities&service=WMS").openConnection() as HttpURLConnection
        val `is` = c.getInputStream()
        val cap = parse(`is`)
        Assert.assertNotNull(cap)

        `is`.close()
        c.disconnect()

        verify(cap)
        Assert.assertTrue(cap.layers!!.size >= 1)
    }

    private fun verify(cap: WMSEndpoint?) {
        Assert.assertNotNull(cap)
        Assert.assertNotNull(cap!!.baseurl)
        Assert.assertFalse(cap.layers!!.isEmpty())
        for (i in cap.layers!!.indices) {
            val wmsLayer = cap.layers!!.get(i)
            Assert.assertNotNull(wmsLayer!!.name + wmsLayer.description + wmsLayer.title, wmsLayer.name)
            //            Assert.assertNotNull(wmsLayer.getName() + wmsLayer.getDescription() + wmsLayer.getTitle(), wmsLayer.getDescription());
            Assert.assertNotNull(wmsLayer.name + wmsLayer.description + wmsLayer.title, wmsLayer.title)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUSGSFile2() {
        val input = File("./src/test/resources/basemapNationalMapGov.xml")
        if (!input.exists()) {
            Assert.fail(File(".").getAbsolutePath() + " = pwd. target file doesn't exist at " + input.getAbsolutePath())
        }
        val fis = FileInputStream(input)
        val cap = parse(fis)
        fis.close()
        verify(cap)
        Assert.assertEquals("https://basemap.nationalmap.gov:443/arcgis/services/USGSTopo/MapServer/WmsServer?", cap.baseurl)
        Assert.assertTrue(cap.layers!!.size == 1)
        Assert.assertEquals("1.3.0", cap.wmsVersion)
        Assert.assertFalse(cap.layers!!.get(0)!!.styles!!.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testUSGSFile() {
        val input = File("./src/test/resources/usgs_getcapabilities.xml")
        if (!input.exists()) {
            Assert.fail(File(".").getAbsolutePath() + " = pwd. target file doesn't exist at " + input.getAbsolutePath())
        }
        val fis = FileInputStream(input)
        val cap = parse(fis)
        fis.close()
        Assert.assertEquals("http://basemap.nationalmap.gov/arcgis/services/USGSTopo/MapServer/WmsServer?", cap.baseurl)
        verify(cap)
        Assert.assertTrue(cap.layers!!.size == 1)
        Assert.assertEquals("1.3.0", cap.wmsVersion)
    }

    @Test
    @Throws(Exception::class)
    fun testNASA111File() {
        val input = File("./src/test/resources/nasawms111.xml")
        if (!input.exists()) {
            Assert.fail(File(".").getAbsolutePath() + " = pwd. target file doesn't exist at " + input.getAbsolutePath())
        }
        val fis = FileInputStream(input)
        val cap = parse(fis)
        fis.close()

        verify(cap)
        Assert.assertTrue(cap.layers!!.size == 129)
        Assert.assertEquals("1.1.1", cap.wmsVersion)
    }

    @Test
    @Throws(Exception::class)
    fun testNASA130File() {
        val input = File("./src/test/resources/nasawms130.xml")
        if (!input.exists()) {
            Assert.fail(File(".").getAbsolutePath() + " = pwd. target file doesn't exist at " + input.getAbsolutePath())
        }
        val fis = FileInputStream(input)
        val cap = parse(fis)
        fis.close()

        Assert.assertEquals("https://neo.sci.gsfc.nasa.gov/wms/wms", cap.baseurl)
        verify(cap)
        Assert.assertTrue(cap.layers!!.size == 129)
        Assert.assertEquals("1.3.0", cap.wmsVersion)
    }

    @Test
    @Throws(Exception::class)
    fun testNASA130SRSFile() {
        val input = File("./src/test/resources/nasasvs.xml")
        if (!input.exists()) {
            Assert.fail(File(".").getAbsolutePath() + " = pwd. target file doesn't exist at " + input.getAbsolutePath())
        }
        val fis = FileInputStream(input)
        val cap = parse(fis)
        fis.close()

        Assert.assertEquals("http://svs.gsfc.nasa.gov/cgi-bin/wms?", cap.baseurl)
        verify(cap)
        Assert.assertEquals(288, cap.layers!!.size.toLong())
        Assert.assertEquals("1.1.1", cap.wmsVersion)
        for (i in cap.layers!!.indices) {
            val wmsLayer = cap.layers!!.get(i)
            if (wmsLayer!!.name == "3238_22718_705010") {
                Assert.assertEquals(1024, wmsLayer.pixelSize.toLong())
                Assert.assertTrue(wmsLayer.srs.contains("EPSG:4326"))
            }
        }
    }
}
