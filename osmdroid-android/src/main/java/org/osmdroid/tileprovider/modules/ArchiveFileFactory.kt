package org.osmdroid.tileprovider.modules

import android.os.Build
import android.util.Log
import org.osmdroid.api.IMapView
import java.io.File
import java.util.Locale

object ArchiveFileFactory {
    var extensionMap: MutableMap<String?, Class<out IArchiveFile>?> = HashMap()

    init {
        extensionMap.put("zip", ZipFileArchive::class.java)
        if (Build.VERSION.SDK_INT >= 10) {
            extensionMap.put("sqlite", DatabaseFileArchive::class.java)
            extensionMap.put("mbtiles", MBTilesFileArchive::class.java)
            extensionMap.put("gemf", GEMFFileArchive::class.java)
        }
    }

    /**
     * Returns true if and only if the extension (minus the ".") is registered, meaning that osmdroid
     * has a driver to read map tiles/data from that source.
     *
     * @param extension the file extension in question, minus the "."
     * @return
     * @since 5.0
     */
    fun isFileExtensionRegistered(extension: String?): Boolean {
        return extensionMap.containsKey(extension)
    }

    /**
     * Registers a custom archive file provider
     *
     * @param provider
     * @param fileExtension without the dot
     * @since 5.0
     */
    fun registerArchiveFileProvider(provider: Class<out IArchiveFile>?, fileExtension: String?) {
        extensionMap.put(fileExtension, provider)
    }

    /**
     * Return an implementation of [IArchiveFile] for the specified file.
     *
     * @return an implementation, or null if there's no suitable implementation
     */
    fun getArchiveFile(pFile: File): IArchiveFile? {
        var extension = pFile.name
        if (extension.contains(".")) {
            try {
                extension = extension.substring(extension.lastIndexOf(".") + 1)
            } catch (ex: Exception) {
                //just to catch any potential out of index errors
            }
        }
        val aClass = extensionMap.get(extension.lowercase(Locale.getDefault()))
        if (aClass != null) {
            try {
                val provider: IArchiveFile = aClass.newInstance()
                provider.init(pFile)
                return provider
            } catch (e: InstantiationException) {
                Log.e(IMapView.LOGTAG, "Error initializing archive file provider " + pFile.getAbsolutePath(), e)
            } catch (e: IllegalAccessException) {
                Log.e(IMapView.LOGTAG, "Error initializing archive file provider " + pFile.getAbsolutePath(), e)
            } catch (e: Exception) {
                Log.e(IMapView.LOGTAG, "Error opening archive file " + pFile.getAbsolutePath(), e)
            }
        }


        return null
    }

    val registeredExtensions: MutableSet<String?>
        /**
         * @return
         * @since 6.0.0
         */
        get() {
            val r: MutableSet<String?> = HashSet<String?>()
            r.addAll(extensionMap.keys)
            return r
        }
}
