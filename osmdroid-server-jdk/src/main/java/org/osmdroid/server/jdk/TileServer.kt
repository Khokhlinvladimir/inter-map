package org.osmdroid.server.jdk

import org.apache.cxf.endpoint.Server
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider
import org.apache.cxf.jaxrs.provider.JAXBElementProvider
import org.apache.cxf.jaxrs.provider.json.JSONProvider

/**
 * This is a simple command list web server (jetty based) that starts up a rest endpoint that serves map tiles.
 * 
 * 
 * By default it will attempt to start up on port 80.
 * 
 * @author [Alex O'Ree](mailto:alexoree@apache.org)
 */
object TileServer {
    var port: Int = 80
    var ENDPOINT_ADDRESS: String? = null

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println("This will listen on port 80 by default for web traffic (on all IP addresses)")
        println("Usage")
        println("jar -jar <...with-dependencies.jar> <port>")
        if (args.size > 0) {
            try {
                port = args[0].toInt()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        ENDPOINT_ADDRESS = "http://0.0.0.0:" + port + "/"

        println("Attempting to bind to " + ENDPOINT_ADDRESS)
        startServer()


        if (System.console() != null) {
            println("Server started at " + ENDPOINT_ADDRESS + " press enter to stop.")
            System.console().readLine()
        } else {
            println("Server started at " + ENDPOINT_ADDRESS + " press Ctrl-C to stop.")
            while (true) {
                Thread.sleep(5000)
            }
        }
        server!!.stop()
        server!!.destroy()
    }

    private var server: Server? = null
    var instance: TileFetcher? = null

    /**
     * this files up a CXF based Jetty server to host tile rest service
     * 
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun startServer() {
        val sf = JAXRSServerFactoryBean()
        sf.setResourceClasses(TileFetcher::class.java)

        val providers: MutableList<Any?> = ArrayList<Any?>()
        // add custom providers if any
        providers.add(JAXBElementProvider<Any?>())
        providers.add(JSONProvider<Any?>())
        sf.setProviders(providers)

        sf.setResourceProvider(
            TileFetcher::class.java,
            SingletonResourceProvider(TileFetcher(), true)
        )
        sf.setAddress(ENDPOINT_ADDRESS)

        server = sf.create()
    }
}
