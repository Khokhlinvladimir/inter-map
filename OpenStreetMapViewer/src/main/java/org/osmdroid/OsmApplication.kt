package org.osmdroid

import android.content.Context
import android.os.Debug
import android.os.Environment
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import org.acra.ACRA
import org.acra.annotation.ReportsCrashes
import org.acra.collector.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.osmdroid.config.Configuration.instance
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

/**
 * This is the base application for the sample app. We only use to catch errors during development cycles
 *
 *
 * Also see note on setting the UserAgent value
 * Created by alex on 7/4/16.
 */
@ReportsCrashes(formUri = "")
class OsmApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        /*if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }*/
        Thread.currentThread().setUncaughtExceptionHandler(OsmUncaughtExceptionHandler())

        //https://github.com/osmdroid/osmdroid/issues/366

        //super important. Many tile servers, including open street maps, will BAN applications by user
        //agent. Do not use the sample application's user agent for your app! Use your own setting, such
        //as the app id.
        instance!!.userAgentValue = getPackageName()


        /*
        FIXME, need a key for bing
        BingMapTileSource.retrieveBingKey(this);
        final BingMapTileSource source = new BingMapTileSource(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                source.initMetaData();
            }
        }).start();
        source.setStyle(BingMapTileSource.IMAGERYSET_AERIALWITHLABELS);
        TileSourceFactory.addTileSource(source);

        final BingMapTileSource source2 = new BingMapTileSource(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                source2.initMetaData();
            }
        }).start();
        source2.setStyle(BingMapTileSource.IMAGERYSET_ROAD);
        TileSourceFactory.addTileSource(source2);
        */


        //FIXME need a key for this TileSourceFactory.addTileSource(TileSourceFactory.CLOUDMADESMALLTILES);

        //FIXME need a key for this TileSourceFactory.addTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);


        //the sample app a few additional tile sources that we have api keys for, so add them here
        //this will automatically show up in the tile source list
        //FIXME this key is expired TileSourceFactory.addTileSource(new HEREWeGoTileSource(getApplicationContext()));
        //TileSourceFactory.addTileSource(new MapBoxTileSource(getApplicationContext()));
        //TileSourceFactory.addTileSource(new MapQuestTileSource(getApplicationContext()));
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)

        try {
            // Initialise ACRA
            ACRA.init(this)
            ACRA.getErrorReporter().setReportSender(ErrorFileWriter())
        } catch (t: Throwable) {
            t.printStackTrace()
            //this can happen on androidx86 getExternalStorageDir is not writable or if there is a
            //permissions issue
        }
    }

    class OsmUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread?, ex: Throwable) {
            Log.e("UncaughtException", "Got an uncaught exception: " + ex.toString())
            if (ex.javaClass == OutOfMemoryError::class.java) {
                writeHprof()
            }
            ex.printStackTrace()
        }
    }

    /**
     * Writes hard crash stack traces to a file on the SD card.
     */
    private class ErrorFileWriter : ReportSender {
        @Throws(ReportSenderException::class)
        override fun send(context: Context?, crashReportData: CrashReportData) {
            try {
                val rootDirectory = Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                var f = File(
                    (rootDirectory
                            + File.separatorChar
                            + "osmdroid"
                            + File.separatorChar)
                )
                f.mkdirs()
                f = File(
                    (rootDirectory
                            + File.separatorChar
                            + "osmdroid"
                            + File.separatorChar
                            + "crash.log")
                )
                if (f.exists()) f.delete()


                f.createNewFile()
                val pw = PrintWriter(FileWriter(f))
                pw.println(crashReportData.toString())
                pw.close()
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }
    }

    companion object {
        /**
         * writes the current heap to the file system at /sdcard/osmdroid/trace-{timestamp}.hprof
         * again, used only during out CI/memory leak tests
         */
        @JvmStatic
        fun writeHprof() {
            try {
                Debug.dumpHprofData(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/trace-" + System.currentTimeMillis() + ".hprof"
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
