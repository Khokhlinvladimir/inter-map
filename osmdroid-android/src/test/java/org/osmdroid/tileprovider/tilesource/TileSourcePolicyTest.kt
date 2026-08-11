package org.osmdroid.tileprovider.tilesource

import org.junit.Assert
import org.junit.Test
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import java.net.HttpURLConnection
import java.util.Random

class TileSourcePolicyTest {
    private val mCacheControlValue = 172800L
    private val mCacheControlStringOK = arrayOf("max-age=172800, public", "public, max-age=172800", "max-age=172800")
    private val mCacheControlStringKO = arrayOf("max-age=, public", "public")
    private val mExpiresValue = 1539971220000L
    private val mExpiresStringOK = arrayOf("Fri, 19 Oct 2018 17:47:00 GMT")
    private val mExpiresStringKO = arrayOf("Frfgi, 19 Oct 2018 17:47:00 GMT")

    @Test
    fun testGetHttpExpiresTime() {
        val policy = TileSourcePolicy()
        for (value in mExpiresStringOK) Assert.assertEquals(mExpiresValue, policy.getHttpExpiresTime(value)!!.toLong())
        for (value in mExpiresStringKO) Assert.assertNull(policy.getHttpExpiresTime(value))
    }

    @Test
    fun testGetHttpCacheControlDuration() {
        val policy = TileSourcePolicy()
        for (value in mCacheControlStringOK) Assert.assertEquals(mCacheControlValue, policy.getHttpCacheControlDuration(value)!!.toLong())
        for (value in mCacheControlStringKO) Assert.assertNull(policy.getHttpCacheControlDuration(value))
    }

    @Test
    fun testComputeExpirationTime() {
        val random = Random()
        val oneWeek = 7 * 24 * 3600 * 1000
        testComputeExpirationTimeHelper(null, random.nextInt(oneWeek).toLong())
        testComputeExpirationTimeHelper(random.nextInt(oneWeek).toLong(), random.nextInt(oneWeek).toLong())
    }

    private fun testComputeExpirationTimeHelper(pOverride: Long?, pExtension: Long) {
        val policy = TileSourcePolicy()
        val now = System.currentTimeMillis()
        Configuration.instance!!.expirationOverrideDuration = pOverride
        Configuration.instance!!.expirationExtendedDuration = pExtension
        for (cacheControl in mCacheControlStringOK) {
            val expected = if (pOverride != null) now + pOverride else now + mCacheControlValue * 1000 + pExtension
            for (expires in mExpiresStringOK) Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
            for (expires in mExpiresStringKO) Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
        }
        for (cacheControl in mCacheControlStringKO) {
            for (expires in mExpiresStringOK) {
                val expected = if (pOverride != null) now + pOverride else mExpiresValue + pExtension
                Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
            }
            for (expires in mExpiresStringKO) {
                val expected = if (pOverride != null) now + pOverride else now + OpenStreetMapTileProviderConstants.DEFAULT_MAXIMUM_CACHED_FILE_AGE + pExtension
                Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
            }
        }
    }

    @Test
    fun testCustomExpirationTimeWithValues() {
        val twentyMinutes = 20 * 60 * 1000L
        val policy = object : TileSourcePolicy() {
            override fun computeExpirationTime(pHttpExpiresHeader: String?, pHttpCacheControlHeader: String?, pNow: Long): Long =
                pNow + twentyMinutes
        }
        val now = System.currentTimeMillis()
        val expected = now + twentyMinutes
        for (cacheControl in mCacheControlStringOK) {
            for (expires in mExpiresStringOK) Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
            for (expires in mExpiresStringKO) Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
        }
        for (cacheControl in mCacheControlStringKO) {
            for (expires in mExpiresStringOK) Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
            for (expires in mExpiresStringKO) Assert.assertEquals(expected, policy.computeExpirationTime(expires, cacheControl, now))
        }
    }

    @Test
    fun testCustomExpirationTimeWithHttpConnection() {
        val twentyMinutes = 20 * 60 * 1000L
        val thirtyMinutes = 30 * 60 * 1000L
        val dummyConnection = object : HttpURLConnection(null) {
            override fun disconnect() = Unit
            override fun usingProxy(): Boolean = false
            override fun connect() = Unit
            override fun getHeaderField(name: String?): String? = null
        }
        val policy = object : TileSourcePolicy() {
            override fun computeExpirationTime(pHttpExpiresHeader: String?, pHttpCacheControlHeader: String?, pNow: Long): Long =
                pNow + twentyMinutes

            override fun computeExpirationTime(pHttpURLConnection: HttpURLConnection, pNow: Long): Long =
                pNow + thirtyMinutes
        }
        val now = System.currentTimeMillis()
        Assert.assertEquals(now + thirtyMinutes, policy.computeExpirationTime(dummyConnection, now))
    }
}
