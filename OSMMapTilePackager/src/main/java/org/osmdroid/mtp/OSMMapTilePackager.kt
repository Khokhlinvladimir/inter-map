// Created by plusminus on 9:22:20 PM - Mar 5, 2009
package org.osmdroid.mtp

import org.osmdroid.mtp.adt.OSMTileInfo
import org.osmdroid.mtp.download.DownloadManager
import org.osmdroid.mtp.ui.OSMMapTilePackagerUI
import org.osmdroid.mtp.util.DbCreator
import org.osmdroid.mtp.util.FolderDeleter
import org.osmdroid.mtp.util.FolderFileCounter
import org.osmdroid.mtp.util.FolderZipper
import org.osmdroid.mtp.util.Util
import org.osmdroid.util.GEMFFile
import java.awt.Toolkit
import java.io.File
import java.util.Locale
import java.util.Scanner

object OSMMapTilePackager {
    // ===========================================================
    // Constants
    // ===========================================================
    private const val THREADCOUNT_DEFAULT = 2
    private var FORCE = false

    // ===========================================================
    // Fields
    // ===========================================================
    // ===========================================================
    // Constructors
    // ===========================================================
    @JvmStatic
    fun main(args: Array<String>) {
        if (args == null || args.size == 0) {
            printUsageAndExit()
        }

        /* Parsing will only start if this variable was set. */
        FORCE = false
        var serverURL: String? = null
        var destinationFile: String? = null
        var tempFolder: String? = null
        var fileAppendix = ""
        var north: Double? = null
        var south: Double? = null
        var east: Double? = null
        var west: Double? = null
        var maxzoom: Int? = null
        var minzoom = 0
        var threadCount = THREADCOUNT_DEFAULT

        try {
            var i = 0
            while (i < args.size) {
                if (args[i] == "-u") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        serverURL = args[i + 1]
                    }
                } else if (args[i] == "-force") {
                    i--
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        FORCE = true
                    }
                } else if (args[i] == "-d") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        destinationFile = args[i + 1]
                    }
                } else if (args[i] == "-fa") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        fileAppendix = args[i + 1]
                    }
                } else if (args[i] == "-nthreads") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        threadCount = args[i + 1].toInt()
                    }
                } else if (args[i] == "-zmin") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        minzoom = args[i + 1].toInt()
                    }
                } else if (args[i] == "-zmax") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        maxzoom = args[i + 1].toInt()
                    }
                } else if (args[i] == "-t") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        tempFolder = args[i + 1]
                    }
                } else if (args[i] == "-n") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        north = args[i + 1].toDouble()
                    }
                } else if (args[i] == "-s") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        south = args[i + 1].toDouble()
                    }
                } else if (args[i] == "-e") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        east = args[i + 1].toDouble()
                    }
                } else if (args[i] == "-w") {
                    if (i >= args.size) {
                        printUsageAndExit()
                    } else {
                        west = args[i + 1].toDouble()
                    }
                } else if (args[i] == "-gui") {
                    OSMMapTilePackagerUI.main(emptyArray())
                    return
                }
                i += 2
            }
        } catch (nfe: NumberFormatException) {
            printUsageAndExit()
        }

        if (tempFolder == null) {
            printUsageAndExit()
        }

        if (serverURL == null && !File(tempFolder).exists()) {
            printUsageAndExit()
        }

        if (north == null || south == null || east == null || west == null) {
            printUsageAndExit()
        }

        OSMMapTilePackager.run(
            serverURL,
            destinationFile!!,
            tempFolder!!,
            threadCount,
            fileAppendix,
            minzoom,
            maxzoom!!,
            north!!,
            south!!,
            east!!,
            west!!
        )
    }

    /**
     * this starts executing the download and packaging
     *
     * @param pServerURL
     * @param pDestinationFile
     * @param pTempFolder
     * @param pThreadCount
     * @param pFileAppendix
     * @param pMinZoom
     * @param pMaxZoom
     * @param pNorth
     * @param pSouth
     * @param pEast
     * @param pWest
     */
    private fun run(
        pServerURL: String?,
        pDestinationFile: String,
        pTempFolder: String,
        pThreadCount: Int,
        pFileAppendix: String,
        pMinZoom: Int,
        pMaxZoom: Int,
        pNorth: Double,
        pSouth: Double,
        pEast: Double,
        pWest: Double
    ) {
        File(pTempFolder).mkdirs()

        println("---------------------------")
        runFileExpecterWithAbort(pMinZoom, pMaxZoom, pNorth, pSouth, pEast, pWest)
        execute(pServerURL, pDestinationFile, pTempFolder, pThreadCount, pFileAppendix, pMinZoom, pMaxZoom, pNorth, pSouth, pEast, pWest, null)
        if (pServerURL != null) {
            runCleanup(pTempFolder, true)
        }
    }

    @JvmStatic
    fun execute(
        pServerURL: String?, pDestinationFile: String,
        pTempFolder: String, pThreadCount: Int, pFileAppendix: String,
        pMinZoom: Int, pMaxZoom: Int, pNorth: Double, pSouth: Double,
        pEast: Double, pWest: Double, callbackNotification: ProgressNotification?
    ) {
        println("---------------------------")
        if (pServerURL != null) {
            runDownloading(
                pServerURL,
                pTempFolder,
                pThreadCount,
                pFileAppendix,
                pMinZoom,
                pMaxZoom,
                pNorth,
                pSouth,
                pEast,
                pWest,
                callbackNotification
            )
        } else {
            println("using temporary directory content")
        }
        println("---------------------------")
        if (callbackNotification != null) {
            callbackNotification.updateProgress("Download complete, creating archive")
        }
        if (pDestinationFile.endsWith(".zip")) {
            runZipToFile(pTempFolder, pDestinationFile)
        } else if (pDestinationFile.endsWith(".gemf")) {
            runCreateGEMFFile(pTempFolder, pDestinationFile)
        } else {
            runCreateDb(pTempFolder, pDestinationFile)
        }

        println("---------------------------")

        if (callbackNotification != null) {
            callbackNotification.updateProgress("Arching complete, deleting temp files")
        }

        println("---------------------------")
    }

    private fun runFileExistenceChecker(
        pExpectedFileCount: Int,
        pTempFolder: String,
        pMinZoom: Int,
        pMaxZoom: Int,
        pNorth: Double,
        pSouth: Double,
        pEast: Double,
        pWest: Double
    ) {
        abortIfUserIsNotSure("This will check if the actual filecount is the same as the expected (" + pExpectedFileCount + ").")

        /* Quickly count files in the tempFolder. */
        print("Counting existing files ...")
        val actualFileCount: Int = FolderFileCounter.getTotalRecursiveFileCount(File(pTempFolder))
        if (pExpectedFileCount == actualFileCount) {
            println(" done.")
        } else {
            println(" FAIL!")
            abortIfUserIsNotSure("Reason: Actual files:" + actualFileCount + "    Expected: " + pExpectedFileCount + ". Proceed?")
        }
    }

    private fun printUsageAndExit() {
        println(
            ("Usage:\n"
                    + "-gui\tLaunch the GUI\n"
                    + "-u\t[OSM-style tile URL: http://_URL_/%d/%d/%d.png]\n"
                    + "-t\t[Temporary Folder]\n"
                    + "-d\t[Destination-file: C:\\mappack.zip]\n"
                    + "-zmin\t[Minimum zoomLevel to download. Default: 0]\n"
                    + "-zmax\t[Maximum zoomLevel to download]\n"
                    + "-fa\t[Filename-Appendix. Default: \"\"]\n"
                    + "-n\t[North Latitude]\n"
                    + "-s\t[South Latitude]\n"
                    + "-e\t[East Longitude]\n"
                    + "-w\t[West Longitude]\n"
                    + "-nthreads\t[Number of Download-Threads. Default: 2]\n")
        )
        System.exit(0)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    private fun runCreateGEMFFile(pTempFolder: String, pDestinationFile: String?) {
        try {
            println("Creating GEMF archive from " + pTempFolder + " to " + pDestinationFile + " ...")
            val sourceFolders: MutableList<File?> = ArrayList<File?>()
            sourceFolders.add(File(pTempFolder))
            val file: GEMFFile = GEMFFile(pDestinationFile, sourceFolders)
            println(" done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runZipToFile(pTempFolder: String, pDestinationFile: String) {
        try {
            print("Zipping files to " + pDestinationFile + " ...")
            FolderZipper.zipFolderToFile(File(pDestinationFile), File(pTempFolder))
            println(" done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runCreateDb(pTempFolder: String, pDestinationFile: String) {
        try {
            print("Putting files into db : " + pDestinationFile + " ...")
            DbCreator.putFolderToDb(File(pDestinationFile), File(pTempFolder))
            println(" done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun runCleanup(pTempFolder: String, confirm: Boolean) {
        if (confirm) abortIfUserIsNotSure("This will delete the temp folder: " + pTempFolder + " !")

        /* deleteDirecto*/
        print("Deleting temp folder ...")
        FolderDeleter.deleteFolder(File(pTempFolder))
        println(" done.")
    }

    private fun runDownloading(
        pBaseURL: String, pTempFolder: String?, pThreadCount: Int,
        pFileAppendix: String, pMinZoom: Int, pMaxZoom: Int, pNorth: Double,
        pSouth: Double, pEast: Double, pWest: Double, callbackNotification: ProgressNotification?
    ) {
        val pTempBaseURL = (pTempFolder
                + File.separator + "%d"
                + File.separator + "%d"
                + File.separator + "%d"
                + pBaseURL.substring(pBaseURL.lastIndexOf('.'))
                + pFileAppendix
            .replace(File.separator + File.separator, File.separator))

        val dm = DownloadManager(pBaseURL, pTempBaseURL, pThreadCount)

        /* For each zoomLevel. */
        for (z in pMinZoom..pMaxZoom) {
            val upperLeft: OSMTileInfo = Util.getMapTileFromCoordinates(pNorth, pWest, z)
            val lowerRight: OSMTileInfo = Util.getMapTileFromCoordinates(pSouth, pEast, z)

            print("ZoomLevel: " + z + " ")

            for (x in upperLeft.x..lowerRight.x) {
                for (y in upperLeft.y..lowerRight.y) {
                    if (callbackNotification != null) {
                        callbackNotification.updateProgress("Downloading " + z + "/" + x + "/" + y)
                    }
                    dm.add(OSMTileInfo(x, y, z))
                }
            }
            try {
                dm.waitEmpty()
                println(" done.")
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        try {
            print("Awaiting termination of all threads ...")
            dm.waitFinished()
            println(" done.")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun runFileExpecter(pMinZoom: Int, pMaxZoom: Int, pNorth: Double, pSouth: Double, pEast: Double, pWest: Double): Int {
        /* Calculate file-count. */
        var fileCnt = 0
        for (z in pMinZoom..pMaxZoom) {
            val upperLeft: OSMTileInfo = Util.getMapTileFromCoordinates(pNorth, pWest, z)
            val lowerRight: OSMTileInfo = Util.getMapTileFromCoordinates(pSouth, pEast, z)

            val dx: Int = lowerRight.x - upperLeft.x + 1
            val dy: Int = lowerRight.y - upperLeft.y + 1
            fileCnt += dx * dy
        }

        return fileCnt
    }

    private fun runFileExpecterWithAbort(pMinZoom: Int, pMaxZoom: Int, pNorth: Double, pSouth: Double, pEast: Double, pWest: Double): Int {
        val cnt = runFileExpecter(pMinZoom, pMaxZoom, pNorth, pSouth, pEast, pWest)
        println("Using the bounds of N,S,E,W = " + pNorth + "," + pSouth + "," + pEast + "," + pWest)
        abortIfUserIsNotSure("This will download: " + cnt + " Maptiles!")
        return cnt
    }

    private fun abortIfUserIsNotSure(message: String?) {
        if (FORCE) {
            return
        }

        println(message)
        print("Are you sure? [Y/N] ?: ")
        try {
            Toolkit.getDefaultToolkit().beep()
        } catch (t: Throwable) {
            /* Ignore */
        }

        val line = Scanner(System.`in`).nextLine().uppercase(Locale.getDefault()).trim { it <= ' ' }

        if (line != "Y" && line != "YES") {
            System.err.println("User aborted.")
            System.exit(0)
        }
    } // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    interface ProgressNotification {
        fun updateProgress(msg: String?)
    }
}
