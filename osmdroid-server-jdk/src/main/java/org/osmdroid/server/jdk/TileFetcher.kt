/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.osmdroid.server.jdk

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.cxf.jaxrs.model.wadl.Description
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

/**
 * This is a REST web service (via Apache CXF) that provides 3 functions
 *  * getSourceList() - provides a list of human readable tile sources
 *  * getImage - gets a ZXY OSM style map tile
 *  * serves from file system, a simple open layers slippy map that makes it easy to test to see if this thing is working
 * and let's you flip tiles sources using some jquery magic
 * 
 * 
 */
@Path("/")
@Produces("image/png", "application/json", "text/html", "text/css", "text/javascript")
@Description("")
class TileFetcher {
    var connections: HashMap<String?, Connection?> = HashMap<String?, Connection?>()

    init {
        initDatabases()
    }

    @Throws(Exception::class)
    private fun initDatabases() {
        val p = Properties()
        var fis: FileInputStream? = FileInputStream("sources.properties")
        p.load(fis)
        fis!!.close()
        fis = null

        val iterator = p.entries.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()

            val source = next.key as String?
            val filename = next.value as String
            val db = File(filename)
            if (!db.exists()) {
                throw FileNotFoundException("can't find the db " + filename + " current dir is " + File(".").getAbsolutePath())
            }
            try {
                val conn1 = DriverManager.getConnection("jdbc:sqlite:" + filename)
                val stat = conn1.createStatement()
                stat.executeUpdate("CREATE TABLE IF NOT EXISTS tiles (key INTEGER PRIMARY KEY, provider TEXT, tile BLOB)")
                stat.close()
                println("adding " + source + " from file " + filename)
                connections.put(source, conn1)
            } catch (e: SQLException) {
                e.printStackTrace()
                //throw new Exception("unable to initialize db", e);
            }
        }
    }

    @get:Throws(WebApplicationException::class, JsonProcessingException::class)
    @get:Description("Returns a JSON string array of all available map sources")
    @get:Produces("application/json")
    @get:Path("/sources")
    @get:GET
    val sourceList: String?
        get() {
            println("getSourceList")
            return om.writeValueAsString(connections.keys)
        }

    @GET //xyz no good
    //zyx no good
    //zxy closer
    @Path("/{source}/{z}/{x}/{y}.png")
    @Produces("image/png")
    @Description("Returns png of the specific map tile from the database")
    @Throws(WebApplicationException::class)
    fun getImage(
        @PathParam("source") id: String?,
        @PathParam("z") z: Int,
        @PathParam("x") x: Int,
        @PathParam("y") y: Int
    ): ByteArray? {
        val c = connections.get(id)
        if (c == null) {
            System.err.println(id + " isn't registered")
            throw WebApplicationException(Exception(id + " is not a valid tile source"), 400)
        }
        try {
            val prep = c.prepareStatement("Select tile from tiles where key=?;")

            val index = ((((z shl z) + x) shl z) + y).toLong()
            println("Fetching tile " + id + z + "/" + x + "/" + y + " as " + index)
            prep.setLong(1, index)
            val executeQuery = prep.executeQuery()
            if (executeQuery.next()) {
                //Blob b= executeQuery.getBlob(1);
                //byte[] image=b.getBytes(0, (int)b.length());
                val image2 = executeQuery.getBytes(1)
                //return image;
                return image2
            }
            println(id + "Tile not found " + z + "/" + x + "/" + y + " as " + index)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
        }
        throw WebApplicationException(404)
    }

    @get:Throws(WebApplicationException::class)
    @get:Description("Returns a basic html viewer of the slippy map")
    @get:Produces("text/html")
    @get:Path("/index.html")
    @get:GET
    val index: String
        get() = getFile("www/openlayers.html")

    @get:Throws(WebApplicationException::class)
    @get:Description("Returns a basic html viewer of the slippy map")
    @get:Produces("text/html")
    @get:Path("/")
    @get:GET
    val index4: String
        get() = getFile("www/openlayers.html")

    @get:Throws(WebApplicationException::class)
    @get:Description("Returns a basic html viewer of the slippy map")
    @get:Produces("application/javascript")
    @get:Path("/v3.5.0-dist/ol.js")
    @get:GET
    val index2: String
        get() = getFile("www/v3.5.0-dist/ol.js")

    @get:Throws(WebApplicationException::class)
    @get:Description("Returns a basic html viewer of the slippy map")
    @get:Produces("text/css")
    @get:Path("/v3.5.0-dist/ol.css")
    @get:GET
    val index3: String
        get() = getFile("www/v3.5.0-dist/ol.css")

    companion object {
        private val log: Log? = LogFactory.getLog(TileFetcher::class.java)
        var om: ObjectMapper = ObjectMapper()
        fun getFile(f: String): String {
            val sb = StringBuilder()
            try {
                val r = FileInputStream(f)
                var c = 0
                while ((r.read().also { c = it }) != -1) {
                    sb.append(c.toChar())
                }
                r.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
                System.err.println("Current dir is " + File(".").getAbsolutePath())
            }
            return sb.toString()
        }
    }
}
