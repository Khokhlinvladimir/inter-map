package org.osmdroid.util

import org.osmdroid.tileprovider.modules.SqlTileWriter

/**
 * Put there everything that could be done during a splash screen
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class DuringSplashScreen : SplashScreenable {
    override fun runDuringSplashScreen() {
        val sqlTileWriter = SqlTileWriter()
        sqlTileWriter.runDuringSplashScreen()
    }
}