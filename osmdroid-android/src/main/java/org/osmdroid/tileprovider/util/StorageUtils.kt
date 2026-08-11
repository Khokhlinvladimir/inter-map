package org.osmdroid.tileprovider.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import org.osmdroid.config.IConfigurationProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.util.Arrays
import java.util.Scanner
import java.util.StringTokenizer
import java.util.UUID

object StorageUtils {
    const val SD_CARD: String = "sdCard"
    const val EXTERNAL_SD_CARD: String = "externalSdCard"

    private const val TAG = "StorageUtils"

    val storageList: MutableList<StorageInfo>
        /**
         * Attention! This method only gets storage locations that are context independent. Especially
         * it does not return application specific paths like getFilesDir() or getCacheDir(), which
         * might lead to problems especially on API29 and up due to scoped storage restrictions, where
         * this method will always return an empty list!
         * It's always recommended to use [.getStorageList] instead!
         *
         * @return A [List] of [StorageInfo] of all storage paths, writable or not.
         */
        get() = getStorageList(null)

    /**
     * Detects all available storage locations, writable or not.
     *
     *
     * Attention! If context==null this method only gets storage locations that are context
     * independent. Especially it will not return application specific paths like getFilesDir() or
     * getCacheDir(), which might lead to problems especially on API29 and up due to scoped storage
     * restrictions, where this is then guaranteed to return an empty list!
     *
     * @return A [List] of [StorageInfo] of all storage paths, writable or not.
     */
    fun getStorageList(context: Context?): MutableList<StorageInfo> {
        val storageInfos: MutableList<StorageInfo>
        // only use this for Q and up for now, to not break behaviour on other versions
        if (Build.VERSION.SDK_INT >= 29) {
            if (context != null) {
                storageInfos = getStorageListApi19(context)
            } else {
                // This is fallback for the case when targetSdk of the application is < 29
                // In this case scoped storage restrictions are not enforced, even though device
                // is API29. Will always return an empty list when targetSdk >= API29.
                storageInfos = storageListPreApi19
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            storageInfos = storageListPreApi19
            if (context != null) {
                val storageInfosApi19 = getStorageListApi19(context)
                // de-duplicate list and add additional ones
                storageInfosApi19.removeAll(storageInfos)
                storageInfos.addAll(storageInfosApi19)
            }
        } else {
            storageInfos = storageListPreApi19
            // make this consistent with the old getStorage(context) method's behaviour
            if (storageInfos.size == 0 && context != null) {
                // http://stackoverflow.com/questions/21230629/getfilesdir-vs-environment-getdatadirectory
                val dbPath = context.getDatabasePath("temp.sqlite").getAbsolutePath().replace("temp.sqlite", "")
                if (isWritable(File(dbPath))) {
                    storageInfos.add(StorageInfo(dbPath, true, false, -1))
                }
            }
        }
        return storageInfos
    }

    private val storageListPreApi19: MutableList<StorageInfo>
        get() {
            val storageInfos: MutableList<StorageInfo> = ArrayList<StorageInfo>()

            val primarySharedStorageInfo: StorageInfo? = primarySharedStorage
            if (primarySharedStorageInfo != null) {
                storageInfos.add(primarySharedStorageInfo)
            }

            storageInfos.addAll(
                tryToFindOtherVoIdManagedStorages(
                    if (primarySharedStorageInfo != null) primarySharedStorageInfo.path else ""
                )
            )

            val allStorageLocationsRevised: MutableSet<File> = allWritableStorageLocations
            for (storageLocation in allStorageLocationsRevised) {
                var found = false
                for (storageInfo in storageInfos) {
                    if (storageInfo.path == storageLocation.getAbsolutePath()) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    storageInfos.add(StorageInfo(storageLocation.getAbsolutePath(), false, false, -1))
                }
            }

            return storageInfos
        }

    @SuppressLint("NewApi")
    private fun getStorageListApi19(context: Context): MutableList<StorageInfo> {
        val storageInfos = ArrayList<StorageInfo>()

        storageInfos.add(StorageInfo(context.getFilesDir().getAbsolutePath(), true, false, -1))

        val storageDirs = ArrayList<File>()
        val externalDirs = context.getExternalFilesDirs(null)

        for (externalDir in externalDirs) {
            // "Returned paths may be null if a storage device is unavailable."
            if (externalDir == null) {
                continue
            }

            val state = Environment.getStorageState(externalDir)
            if (Environment.MEDIA_MOUNTED == state) {
                storageDirs.add(externalDir)
            }
        }

        for (storageDir in storageDirs) {
            storageInfos.add(StorageInfo(storageDir.getAbsolutePath(), false, false, -1))
        }

        return storageInfos
    }

    @get:Deprecated("As of 6.1.7, use {@link #getBestWritableStorage()} instead.")
    val storage: File?
        /**
         * Gets the best possible storage location by free space
         *
         *
         * Attention! This method only gets storage locations that are context independent. Especially
         * it does not return application specific paths like getFilesDir() or getCacheDir(), which
         * might lead to problems especially on API29 and up due to scoped storage restrictions, where
         * this method will always return null!
         * It's always recommended to use [.getBestWritableStorage] instead!
         *
         */
        get() = getStorage(null)

    val bestWritableStorage: StorageInfo?
        /**
         * Gets the best possible storage location by free space
         *
         *
         * Attention! This method only gets storage locations that are context independent. Especially
         * it does not return application specific paths like getFilesDir() or getCacheDir(), which
         * might lead to problems especially on API29 and up due to scoped storage restrictions.
         * For now it is advised to manually determine a proper cache location and set it via
         * [IConfigurationProvider.setOsmdroidTileCache].
         *
         * @return A [StorageInfo] object.
         */
        get() = getBestWritableStorage(null)

    /**
     * Gets the best possible storage location by free space
     *
     *
     * Attention! If context==null this method only gets storage locations that are context
     * independent. Especially it will not return application specific paths like getFilesDir() or
     * getCacheDir(), which might lead to problems especially on API29 and up due to scoped storage
     * restrictions, where this is then guaranteed to return null!
     *
     */
    @Deprecated("As of 6.1.7, use {@link #getBestWritableStorage(Context)} instead.")
    fun getStorage(context: Context?): File? {
        val bestStorage = getBestWritableStorage(context)
        if (bestStorage != null) {
            return File(bestStorage.path)
        }

        return null
    }

    /**
     * Gets the best possible storage location by free space
     *
     *
     * Attention! If context==null this method only gets storage locations that are context
     * independent. Especially it will not return application specific paths like getFilesDir() or
     * getCacheDir(), which might lead to problems especially on API29 and up due to scoped storage
     * restrictions, where this is then guaranteed to return null!
     *
     * @return A [StorageInfo] object.
     */
    fun getBestWritableStorage(context: Context?): StorageInfo? {
        var bestStorage: StorageInfo? = null
        val storageList = getStorageList(context)
        for (i in storageList.indices) {
            val currentStorage = storageList.get(i)
            if (!currentStorage.readonly && isWritable(File(currentStorage.path))) {
                if (bestStorage != null) {
                    //compare free space
                    if (bestStorage.freeSpace < currentStorage.freeSpace) {
                        bestStorage = currentStorage
                    }
                } else {
                    bestStorage = currentStorage
                }
            }
        }
        return bestStorage
    }

    @get:Deprecated("As of 6.1.7, will be removed in the future.")
    val isAvailable: Boolean
        /**
         * @return True if the primary shared storage is available. False otherwise.
         */
        get() = isPrimarySharedStorageAvailable

    private val isPrimarySharedStorageAvailable: Boolean
        /**
         * @return True if the primary shared storage is available. False otherwise.
         */
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    @get:Deprecated("As of 6.1.7, will be removed in the future.")
    val sdCardPath: String
        /**
         * @return The path of the primary shared storage.
         */
        get() = Environment.getExternalStorageDirectory().getPath() + "/"

    @get:Deprecated("As of 6.1.7, will be removed in the future.")
    val isWritable: Boolean
        /**
         * @return True if the primary shared storage is writable. False otherwise.
         */
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    /**
     * @return True if the path is writable. False otherwise.
     */
    fun isWritable(path: File): Boolean {
        var fos: FileOutputStream? = null
        try {
            val tmp = File(path.getAbsolutePath() + File.separator + UUID.randomUUID().toString())
            fos = FileOutputStream(tmp)
            fos.write("hi".toByteArray())

            tmp.delete()
            Log.i(TAG, path.getAbsolutePath() + " is writable")
            return true
        } catch (ex: Throwable) {
            Log.i(TAG, path.getAbsolutePath() + " is NOT writable")
            return false
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    @get:Deprecated("As of 6.1.7, use {@link #getStorageList()} instead.")
    val allStorageLocations: MutableMap<String?, File?>
        /**
         * @return A [Map] of all storage locations available
         */
        get() {
            val map: MutableMap<String?, File?> = HashMap<String?, File?>(10)

            map.putAll(tryToGetMountedStoragesFromFilesystem())

            //ok now that we've done the dirty linux work, let's pull in the android bits
            if (!map.containsValue(Environment.getExternalStorageDirectory())) map.put(
                SD_CARD,
                Environment.getExternalStorageDirectory()
            )

            val fromSystemEnv = tryToGetStorageFromSystemEnv()
            for (file in fromSystemEnv) {
                if (file.exists() && !map.containsValue(file)) {
                    map.put(SD_CARD, file)
                }
            }

            return map
        }

    private val allWritableStorageLocations: MutableSet<File>
        /**
         * @return A [Set] of all writable storage locations available
         */
        get() {
            val map: MutableSet<File> = HashSet<File>()

            val fromSystemEnv = tryToGetStorageFromSystemEnv()
            for (file in fromSystemEnv) {
                if (isWritable(file)) {
                    map.add(file)
                }
            }

            if (Environment.getExternalStorageDirectory() != null) {
                val t = Environment.getExternalStorageDirectory()
                if (isWritable(t)) {
                    map.add(t)
                }
            }

            val mounts = tryToGetMountedStoragesFromFilesystem()
            for (file in mounts.values) {
                if (isWritable(file)) {
                    map.add(file)
                }
            }

            return map
        }

    private val primarySharedStorage: StorageInfo?
        get() {
            var primarySharedStoragePath = ""
            var isPrimarySharedStorageNotRemovable = false
            var isPrimarySharedStorageReadonly = true
            var isPrimarySharedStorageAvailable = false

            try {
                if (Environment.getExternalStorageDirectory() != null) {
                    primarySharedStoragePath = Environment.getExternalStorageDirectory().getPath()
                }
            } catch (ex: Throwable) {
                //trap for android studio layout editor and some for certain devices
                //see https://github.com/osmdroid/osmdroid/issues/508
                ex.printStackTrace()
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    isPrimarySharedStorageNotRemovable = !Environment.isExternalStorageRemovable()
                }
            } catch (ex: Throwable) {
                //trap for android studio layout editor and some for certain devices
                //see https://github.com/osmdroid/osmdroid/issues/508
                ex.printStackTrace()
            }
            try {
                isPrimarySharedStorageAvailable = StorageUtils.isPrimarySharedStorageAvailable
            } catch (ex: Throwable) {
                //trap for android studio layout editor and some for certain devices
                //see https://github.com/osmdroid/osmdroid/issues/508
                ex.printStackTrace()
            }
            try {
                isPrimarySharedStorageReadonly = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED_READ_ONLY
            } catch (ex: Throwable) {
                //trap for android studio layout editor and some for certain devices
                //see https://github.com/osmdroid/osmdroid/issues/508
                ex.printStackTrace()
            }

            var primarySharedStorageInfo: StorageInfo? = null
            if (isPrimarySharedStorageAvailable) {
                primarySharedStorageInfo =
                    StorageInfo(primarySharedStoragePath, isPrimarySharedStorageNotRemovable, isPrimarySharedStorageReadonly, -1)
            }
            return primarySharedStorageInfo
        }

    private fun tryToFindOtherVoIdManagedStorages(storagePathToIgnore: String?): MutableList<StorageInfo> {
        val storageInfos: MutableList<StorageInfo> = ArrayList<StorageInfo>()
        var bufferedReader: BufferedReader? = null

        try {
            val paths = HashSet<String?>()
            bufferedReader = BufferedReader(FileReader("/proc/mounts"))
            var line: String?
            var currentDisplayNumber = 1
            Log.d(TAG, "/proc/mounts")
            while ((bufferedReader.readLine().also { line = it }) != null) {
                val currentLine = line ?: continue
                Log.d(TAG, currentLine)
                if (currentLine.contains("vfat") || currentLine.contains("/mnt")) {
                    val tokens = StringTokenizer(currentLine, " ")
                    var unused = tokens.nextToken() //device
                    val mountPoint = tokens.nextToken() //mount point
                    if (paths.contains(mountPoint)) {
                        continue
                    }
                    unused = tokens.nextToken() //file system
                    val flags = Arrays.asList<String?>(*tokens.nextToken().split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) //flags
                    val readonly = flags.contains("ro")

                    // if mountPoint is the primary shared storage, skip it
                    if (mountPoint == storagePathToIgnore) {
                        paths.add(storagePathToIgnore)
                    } else if (currentLine.contains("/dev/block/vold")) {
                        if (!currentLine.contains("/mnt/secure") && !currentLine.contains("/mnt/asec") && !currentLine.contains("/mnt/obb") && !currentLine.contains("/dev/mapper") && !currentLine.contains(
                                "tmpfs"
                            )
                        ) {
                            paths.add(mountPoint)
                            if (File(mountPoint + File.separator).exists()) {
                                storageInfos.add(StorageInfo(mountPoint, false, readonly, currentDisplayNumber++))
                            }
                        }
                    }
                }
            }
        } catch (ex: FileNotFoundException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close()
                } catch (ignored: IOException) {
                }
            }
        }
        return storageInfos
    }

    private fun tryToGetMountedStoragesFromFilesystem(): MutableMap<String?, File> {
        val map: MutableMap<String?, File> = HashMap<String?, File>()

        val mounts: MutableList<String> = ArrayList<String>(10)
        val vold: MutableList<String?> = ArrayList<String?>(10)
        mounts.add("/mnt/sdcard")
        vold.add("/mnt/sdcard")

        var scanner: Scanner? = null
        try {
            val mountFile = File("/proc/mounts")
            if (mountFile.exists()) {
                scanner = Scanner(mountFile)
                while (scanner.hasNext()) {
                    val line = scanner.nextLine()
                    if (line.startsWith("/dev/block/vold/")) {
                        val lineElements = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val element = lineElements[1]

                        // don't add the default mount path
                        // it's already in the list.
                        if (element != "/mnt/sdcard") {
                            mounts.add(element)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (scanner != null) {
                try {
                    scanner.close()
                } catch (ignored: Exception) {
                }
            }
            scanner = null
        }

        try {
            val voldFile = File("/system/etc/vold.fstab")
            if (voldFile.exists()) {
                scanner = Scanner(voldFile)
                while (scanner.hasNext()) {
                    val line = scanner.nextLine()
                    if (line.startsWith("dev_mount")) {
                        val lineElements = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        var element = lineElements[2]

                        if (element.contains(":")) {
                            element = element.substring(0, element.indexOf(":"))
                        }
                        if (element != "/mnt/sdcard") {
                            vold.add(element)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (scanner != null) {
                try {
                    scanner.close()
                } catch (ignored: Exception) {
                }
            }
        }

        var i = 0
        while (i < mounts.size) {
            val mount: String? = mounts.get(i)
            if (!vold.contains(mount)) {
                mounts.removeAt(i--)
            }
            i++
        }
        vold.clear()

        val mountHash: MutableList<String?> = ArrayList<String?>(10)
        for (mount in mounts) {
            val root = File(mount)
            if (root.exists() && root.isDirectory() && root.canWrite()) {
                val list = root.listFiles()
                val hash = StringBuilder("[")
                if (list != null) {
                    for (f in list) {
                        hash.append(f.name.hashCode()).append(":").append(f.length()).append(", ")
                    }
                }
                hash.append("]")
                if (!mountHash.contains(hash.toString())) {
                    var key = SD_CARD + "_" + map.size
                    if (map.size == 0) {
                        key = SD_CARD
                    } else if (map.size == 1) {
                        key = EXTERNAL_SD_CARD
                    }
                    mountHash.add(hash.toString())
                    map.put(key, root)
                }
            }
        }

        return map
    }

    private fun tryToGetStorageFromSystemEnv(): MutableSet<File> {
        val storages: MutableSet<File> = HashSet<File>()
        val primarySd = System.getenv("EXTERNAL_STORAGE")
        if (primarySd != null) {
            val t = File(primarySd + File.separator)
            storages.add(t)
        }

        val secondarySd = System.getenv("SECONDARY_STORAGE")
        if (secondarySd != null) {
            val split = secondarySd.split(File.pathSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (s in split) {
                val t = File(s + File.separator)
                storages.add(t)
            }
        }

        return storages
    }

    class StorageInfo(path: String, internal: Boolean, readonly: Boolean, display_number: Int) {
        val path: String?
        val internal: Boolean
        var readonly: Boolean = false
        val display_number: Int
        var freeSpace: Long = 0
        var displayName: String? = null

        init {
            this.path = path
            this.internal = internal
            this.display_number = display_number

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                // gives more accurate information
                this.freeSpace = StatFs(path).getAvailableBytes()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                this.freeSpace = File(path).getFreeSpace()
            }

            if (!readonly) {
                //confirm it's writable
                this.readonly = !isWritable(File(path))
            }

            val res = StringBuilder()
            if (internal) {
                res.append("Internal SD card")
            } else if (display_number > 1) {
                res.append("SD card ").append(display_number)
            } else {
                res.append("SD card")
            }
            if (readonly) {
                res.append(" (Read only)")
            }
            displayName = res.toString()
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false

            val that = o as StorageInfo

            if (internal != that.internal) return false
            if (readonly != that.readonly) return false
            if (display_number != that.display_number) return false
            if (freeSpace != that.freeSpace) return false
            if (if (path != null) (path != that.path) else that.path != null) return false
            return if (displayName != null) (displayName == that.displayName) else that.displayName == null
        }

        override fun hashCode(): Int {
            var result = if (path != null) path.hashCode() else 0
            result = 31 * result + (if (internal) 1 else 0)
            result = 31 * result + (if (readonly) 1 else 0)
            result = 31 * result + display_number
            result = 31 * result + (freeSpace xor (freeSpace ushr 32)).toInt()
            result = 31 * result + (if (displayName != null) displayName.hashCode() else 0)
            return result
        }
    }
}
