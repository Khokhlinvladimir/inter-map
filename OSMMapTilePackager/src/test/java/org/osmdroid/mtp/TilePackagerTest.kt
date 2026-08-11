package org.osmdroid.mtp

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File

/**
 * Created by alex on 9/13/16.
 */
class TilePackagerTest {
    @Ignore
    @Test
    fun runBasicTest() {
        runTest("fr_mapnick_12.zip")
    }

    @Ignore
    @Test
    fun runBasicTestSql() {
        runTest("fr_mapnick_12.sql")
    }

    @Ignore
    @Test
    fun runBasicTestGemf() {
        runTest("fr_mapnick_12.gemf")
    }

    private fun runTest(outputFile: String) {
        OSMMapTilePackager.main(
            arrayOf(
                "-force",
                "-u", "https://b.tile.openstreetmap.org/%d/%d/%d.png",
                "-t", "Mapnik",
                "-d", outputFile,
                "-zmax", "2",
                "-n", "51.091099",
                "-s", "41.366379",
                "-e", "9.543055",
                "-w", "-4.790556"
            )
        )
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        Assert.assertTrue(File(outputFile).exists())
        Assert.assertTrue(File(outputFile).length() > 0)

        File(outputFile).delete()
    }
}
