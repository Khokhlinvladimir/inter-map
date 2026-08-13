package org.osmdroid.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Collections
import java.util.TreeSet

/**
 * GEMF File handler class.
 *
 *
 * Reference: https://sites.google.com/site/abudden/android-map-store
 *
 *
 * Do not reference any android specific code in this class, it is reused in the JRE
 * Tile Packager
 *
 * @author A. S. Budden
 * @author Erik Burrows
 */
class GEMFFile {
    // ===========================================================
    // Public Methods
    // ===========================================================
    /*
         * Returns the base name of the first file in the GEMF archive.
         */
    // ===========================================================
    // Fields
    // ===========================================================
    // Path to first GEMF file (additional files as <basename>-1, <basename>-2, ...
    val name: String

    // All GEMF file parts for this archive
    private val mFiles: MutableList<RandomAccessFile> = ArrayList<RandomAccessFile>()
    private val mFileNames: MutableList<String?> = ArrayList<String?>()

    // Tile ranges represented within this archive
    private val mRangeData: MutableList<GEMFRange> = ArrayList<GEMFRange>()

    // File sizes for offset calculation
    private val mFileSizes: MutableList<Long?> = ArrayList<Long?>()

    /*
     * Returns a LinkedHashMap of the sources in this archive, as names and indexes.
     */
    // List of tile sources within this archive
    val sources: LinkedHashMap<Int?, String?> = LinkedHashMap<Int?, String?>()

    // Fields to restrict to a single source for reading
    private var mSourceLimited = false
    private var mCurrentSource = 0


    // ===========================================================
    // Constructors
    // ===========================================================
    /*
     * Constructor to read existing GEMF archive
     *
     * @param pLocation
     * 		File object representing first GEMF archive file
     */
    constructor(pLocation: File) : this(pLocation.getAbsolutePath())


    /*
     * Constructor to read existing GEMF archive
     *
     * @param pLocation
     * 		String object representing path to first GEMF archive file
     */
    constructor(pLocation: String) {
        this.name = pLocation
        openFiles()
        readHeader()
    }


    /*
     * Constructor to create new GEMF file from directory of sources/tiles.
     *
     * @param pLocation
     * 		String object representing path to first GEMF archive file.
     * 		Additional files (if archive size exceeds FILE_SIZE_LIMIT
     * 		will be created with numerical suffixes, eg: test.gemf-1, test.gemf-2.
     * @param pSourceFolders
     * 		Each specified folder will be imported into the GEMF archive as a seperate
     * 		source. The name of the folder will be the name of the source in the archive.
     */
    constructor(pLocation: String, pSourceFolders: MutableList<File>) {
        /*
         * 1. For each source folder
         *   1. Create array of zoom levels, X rows, Y rows
         * 2. Build index data structure index[source][zoom][range]
         *   1. For each S-Z-X find list of Ys values
         *   2. For each S-Z-X-Ys set, find complete X ranges
         *   3. For each S-Z-Xr-Ys set, find complete Y ranges, create Range record
         * 3. Write out index
         *   1. Header
         *   2. Sources
         *   3. For each Range
         *     1. Write Range record
         * 4. For each Range record
         *   1. For each Range entry
         *     1. If over file size limit, start new data file
         *     2. Write tile data
         */

        this.name = pLocation

        // Create in-memory array of sources, X and Y values.
        val dirIndex =
            LinkedHashMap<String, LinkedHashMap<Int, LinkedHashMap<Int, LinkedHashMap<Int, File>>>>()

        for (sourceDir in pSourceFolders) {
            val zList =
                LinkedHashMap<Int, LinkedHashMap<Int, LinkedHashMap<Int, File>>>()

            for (zDir in sourceDir.listFiles()) {
                // Make sure the directory name is just a number
                try {
                    zDir.getName().toInt()
                } catch (e: NumberFormatException) {
                    continue
                }

                val xList =
                    LinkedHashMap<Int, LinkedHashMap<Int, File>>()

                for (xDir in zDir.listFiles()) {
                    // Make sure the directory name is just a number

                    try {
                        xDir.getName().toInt()
                    } catch (e: NumberFormatException) {
                        continue
                    }

                    val yList = LinkedHashMap<Int, File>()
                    for (yFile in xDir.listFiles()) {
                        try {
                            yFile.getName().substring(
                                0, yFile.getName().indexOf('.'.code.toChar())
                            ).toInt()
                        } catch (e: NumberFormatException) {
                            continue
                        }

                        yList.put(
                            yFile.getName().substring(
                                0, yFile.getName().indexOf('.'.code.toChar())
                            ).toInt(), yFile
                        )
                    }

                    xList.put(xDir.getName().toInt(), yList)
                }

                zList.put(zDir.getName().toInt(), xList)
            }

            dirIndex.put(sourceDir.getName(), zList)
        }

        // Create a source index list
        val sourceIndex = LinkedHashMap<String, Int>()
        val indexSource = LinkedHashMap<Int, String>()
        var si = 0
        for (source in dirIndex.keys) {
            sourceIndex.put(source, si)
            indexSource.put(si, source)
            ++si
        }

        // Create the range objects
        val ranges: MutableList<GEMFRange> = ArrayList<GEMFRange>()

        for (source in dirIndex.keys) {
            for (zoom in dirIndex.get(source)!!.keys) {
                // Get non-contiguous Y sets for each Z/X

                val ySets =
                    LinkedHashMap<MutableList<Int>, MutableList<Int>>()

                for (x in TreeSet<Int>(dirIndex.get(source)!!.get(zoom)!!.keys)) {
                    val ySet: MutableList<Int> = ArrayList<Int>()
                    for (y in dirIndex.get(source)!!.get(zoom)!!.get(x)!!.keys) {
                        ySet.add(y)
                    }

                    if (ySet.size == 0) {
                        continue
                    }

                    Collections.sort<Int>(ySet)

                    if (!ySets.containsKey(ySet)) {
                        ySets.put(ySet, ArrayList<Int>())
                    }

                    ySets.get(ySet)!!.add(x)
                }

                // For each Y set find contiguous X sets
                val xSets =
                    LinkedHashMap<MutableList<Int>, MutableList<Int>>()

                for (ySet in ySets.keys) {
                    val xList = TreeSet<Int>(ySets.get(ySet))

                    var xSet: MutableList<Int> = ArrayList<Int>()
                    for (i in xList.first() until xList.last() + 1) {
                        if (xList.contains(i)) {
                            xSet.add(i)
                        } else {
                            if (xSet.size > 0) {
                                xSets.put(ySet, xSet)
                                xSet = ArrayList<Int>()
                            }
                        }
                    }

                    if (xSet.size > 0) {
                        xSets.put(ySet, xSet)
                    }
                }

                // For each contiguous X set, find contiguous Y sets and create GEMFRange object
                for (xSet in xSets.keys) {
                    val yList = TreeSet<Int>(xSet)
                    val xList = TreeSet<Int>(ySets.get(xSet))

                    var range: GEMFRange = GEMFRange()
                    range.zoom = zoom
                    range.sourceIndex = sourceIndex.get(source)
                    range.xMin = xList.first()
                    range.xMax = xList.last()

                    for (i in yList.first() until yList.last() + 1) {
                        if (yList.contains(i)) {
                            if (range.yMin == null) {
                                range.yMin = i
                            }
                            range.yMax = i
                        } else {
                            if (range.yMin != null) {
                                ranges.add(range)

                                range = GEMFRange()
                                range.zoom = zoom
                                range.sourceIndex = sourceIndex.get(source)
                                range.xMin = xList.first()
                                range.xMax = xList.last()
                            }
                        }
                    }

                    if (range.yMin != null) {
                        ranges.add(range)
                    }
                }
            }
        }


        // Calculate size of header for computation of data offsets
        var source_list_size = 0
        for (source in sourceIndex.keys) {
            source_list_size += (U32_SIZE + U32_SIZE + source.length)
        }

        var offset =
            (U32_SIZE +  // GEMF Version
                    U32_SIZE +  // Tile size
                    U32_SIZE +  // Number of sources
                    source_list_size + ranges.size * ((U32_SIZE * 6) + U64_SIZE) +
                    U32_SIZE).toLong() // Number of ranges

        // Calculate offset for each range in the data set
        for (range in ranges) {
            range.offset = offset

            for (x in range.xMin!! until range.xMax!! + 1) {
                for (y in range.yMin!! until range.yMax!! + 1) {
                    offset += (U32_SIZE + U64_SIZE).toLong()
                }
            }
        }

        val headerSize = offset

        var gemfFile = RandomAccessFile(pLocation, "rw")

        // Write version header
        gemfFile.writeInt(VERSION)

        // Write file size header
        gemfFile.writeInt(TILE_SIZE)

        // Write number of sources
        gemfFile.writeInt(sourceIndex.size)

        // Write source list
        for (source in sourceIndex.keys) {
            gemfFile.writeInt(sourceIndex.get(source)!!)
            gemfFile.writeInt(source.length)
            gemfFile.write(source.toByteArray())
        }

        // Write number of ranges
        gemfFile.writeInt(ranges.size)

        // Write range objects
        for (range in ranges) {
            gemfFile.writeInt(range.zoom!!)
            gemfFile.writeInt(range.xMin!!)
            gemfFile.writeInt(range.xMax!!)
            gemfFile.writeInt(range.yMin!!)
            gemfFile.writeInt(range.yMax!!)
            gemfFile.writeInt(range.sourceIndex!!)
            gemfFile.writeLong(range.offset!!)
        }

        // Write file offset list
        for (range in ranges) {
            for (x in range.xMin!! until range.xMax!! + 1) {
                for (y in range.yMin!! until range.yMax!! + 1) {
                    gemfFile.writeLong(offset)
                    val fileSize = dirIndex.get(
                        indexSource.get(
                            range.sourceIndex
                        )
                    )!!.get(range.zoom!!)!!.get(x)!!.get(y)!!.length()
                    gemfFile.writeInt(fileSize.toInt())
                    offset += fileSize
                }
            }
        }

        //
        // Write tiles
        //
        val buf = ByteArray(FILE_COPY_BUFFER_SIZE)

        var currentOffset = headerSize
        var fileIndex = 0

        for (range in ranges) {
            for (x in range.xMin!! until range.xMax!! + 1) {
                for (y in range.yMin!! until range.yMax!! + 1) {
                    val fileSize = dirIndex.get(
                        indexSource.get(range.sourceIndex)
                    )!!.get(range.zoom!!)!!.get(x)!!.get(y)!!.length()

                    if (currentOffset + fileSize > FILE_SIZE_LIMIT) {
                        gemfFile.close()
                        ++fileIndex
                        gemfFile = RandomAccessFile(pLocation + "-" + fileIndex, "rw")
                        currentOffset = 0
                    } else {
                        currentOffset += fileSize
                    }

                    val tile = FileInputStream(
                        dirIndex.get(
                            indexSource.get(
                                range.sourceIndex
                            )
                        )!!.get(range.zoom!!)!!.get(x)!!.get(y)
                    )

                    var read = tile.read(buf, 0, FILE_COPY_BUFFER_SIZE)
                    while (read != -1) {
                        gemfFile.write(buf, 0, read)
                        read = tile.read(buf, 0, FILE_COPY_BUFFER_SIZE)
                    }

                    tile.close()
                }
            }
        }

        gemfFile.close()

        // Complete construction of GEMFFile object
        openFiles()
        readHeader()
    }


    // ===========================================================
    // Private Methods
    // ===========================================================
    /*
     * Close open GEMF file handles.
     */
    @Throws(IOException::class)
    fun close() {
        for (file in mFiles) {
            file.close()
        }
    }


    /*
     * Find all files composing this GEMF archive, open them as RandomAccessFile
     * and add to the mFiles list.
     */
    @Throws(FileNotFoundException::class)
    private fun openFiles() {
        // Populate the mFiles array

        val base = File(this.name)
        mFiles.add(RandomAccessFile(base, "r"))
        mFileNames.add(base.getPath())

        var i = 0
        while (true) {
            i = i + 1
            val nextFile = File(this.name + "-" + i)
            if (nextFile.exists()) {
                mFiles.add(RandomAccessFile(nextFile, "r"))
                mFileNames.add(nextFile.getPath())
            } else {
                break
            }
        }
    }


    /*
     * Read header of archive, cache Ranges.
     */
    @Throws(IOException::class)
    private fun readHeader() {
        val baseFile = mFiles.get(0)

        // Get file sizes
        for (file in mFiles) {
            mFileSizes.add(file.length())
        }

        // Version
        val version = baseFile.readInt()
        if (version != VERSION) {
            throw IOException("Bad file version: " + version)
        }

        // Tile Size
        val tile_size = baseFile.readInt()
        if (tile_size != TILE_SIZE) {
            throw IOException("Bad tile size: " + tile_size)
        }

        // Read Source List
        val sourceCount = baseFile.readInt()

        for (i in 0 until sourceCount) {
            val sourceIndex = baseFile.readInt()
            val sourceNameLength = baseFile.readInt()
            val nameData = ByteArray(sourceNameLength)
            baseFile.read(nameData, 0, sourceNameLength)

            val sourceName = String(nameData)
            sources.put(sourceIndex, sourceName)
        }

        // Read Ranges
        val num_ranges = baseFile.readInt()
        for (i in 0 until num_ranges) {
            val rs: GEMFRange = GEMFRange()
            rs.zoom = baseFile.readInt()
            rs.xMin = baseFile.readInt()
            rs.xMax = baseFile.readInt()
            rs.yMin = baseFile.readInt()
            rs.yMax = baseFile.readInt()
            rs.sourceIndex = baseFile.readInt()
            rs.offset = baseFile.readLong()
            mRangeData.add(rs)
        }
    }


    /*
     * Set single source for getInputStream() to use. Otherwise, first tile found
     * with specified Z/X/Y coordinates will be returned.
     */
    fun selectSource(pSource: Int) {
        if (sources.containsKey(pSource)) {
            mSourceLimited = true
            mCurrentSource = pSource
        }
    }

    /*
     * Allow getInputStream() to use any source in the archive.
     */
    fun acceptAnySource() {
        mSourceLimited = false
    }

    val zoomLevels: MutableSet<Int?>
        /*
              * Return list of zoom levels contained within this archive.
              */
        get() {
            val zoomLevels: MutableSet<Int?> = TreeSet<Int?>()

            for (rs in mRangeData) {
                zoomLevels.add(rs.zoom)
            }

            return zoomLevels
        }

    /*
     * Get an InputStream for the tile data specified by the Z/X/Y coordinates.
     *
     * @return InputStream of tile data, or null if not found.
     */
    fun getInputStream(pX: Int, pY: Int, pZ: Int): InputStream? {
        var range: GEMFRange? = null

        for (rs in mRangeData) {
            if ((pZ == rs.zoom)
                && (pX >= rs.xMin!!)
                && (pX <= rs.xMax!!)
                && (pY >= rs.yMin!!)
                && (pY <= rs.yMax!!)
                && ((!mSourceLimited) || (rs.sourceIndex == mCurrentSource))
            ) {
                range = rs
                break
            }
        }

        if (range == null) {
            return null
        }

        var dataOffset: Long
        val dataLength: Int
        var returnValue: InputStream? = null
        var stream: GEMFInputStream? = null
        var byteBuffer: ByteArrayOutputStream? = null
        try {
            // Determine offset to requested tile record in the header

            val numY = range.yMax!! + 1 - range.yMin!!
            val xIndex = pX - range.xMin!!
            val yIndex = pY - range.yMin!!
            var offset = ((xIndex * numY) + yIndex).toLong()
            offset *= (U32_SIZE + U64_SIZE).toLong()
            offset += range.offset!!


            // Read tile record from header, get offset and size of data record
            val baseFile = mFiles.get(0)
            baseFile.seek(offset)
            dataOffset = baseFile.readLong()
            dataLength = baseFile.readInt()

            // Seek to correct data file and offset.
            var pDataFile = mFiles.get(0)
            var index = 0
            if (dataOffset > mFileSizes.get(0)!!) {
                val fileListCount = mFileSizes.size

                while ((index < (fileListCount - 1)) &&
                    (dataOffset > mFileSizes.get(index)!!)
                ) {
                    dataOffset -= mFileSizes.get(index)!!
                    index += 1
                }

                pDataFile = mFiles.get(index)
            }

            // Read data block into a byte array
            pDataFile.seek(dataOffset)

            stream = GEMFInputStream(mFileNames.get(index), dataOffset, dataLength)
            // this dynamically extends to take the bytes you read
            byteBuffer = ByteArrayOutputStream()

            // this is storage overwritten on each iteration with bytes
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)

            // we need to know how may bytes were read to write them to the byteBuffer
            var len = 0
            while (stream!!.available() > 0) {
                len = stream.read(buffer)
                if (len > 0) byteBuffer.write(buffer, 0, len)
            }

            // and then we can return your byte array.
            val bits = byteBuffer.toByteArray()
            returnValue = ByteArrayInputStream(bits)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (byteBuffer != null) try {
                byteBuffer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (stream != null) try {
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return returnValue
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    // Class to represent a range of stored tiles within the archive.
    private inner class GEMFRange {
        var zoom: Int? = null
        var xMin: Int? = null
        var xMax: Int? = null
        var yMin: Int? = null
        var yMax: Int? = null
        var sourceIndex: Int? = null
        var offset: Long? = null

        override fun toString(): String {
            return String.format(
                "GEMF Range: source=%d, zoom=%d, x=%d-%d, y=%d-%d, offset=0x%08X",
                sourceIndex, zoom, xMin, xMax, yMin, yMax, offset
            )
        }
    }

    // InputStream class to hand to the tile loader system. It wants an InputStream, and it is more
    // efficient to create a new open file handle pointed to the right place, than to buffer the file
    // in memory.
    internal inner class GEMFInputStream(filePath: String?, offset: Long, length: Int) : InputStream() {
        var raf: RandomAccessFile
        var remainingBytes: Int

        init {
            this.raf = RandomAccessFile(filePath, "r")
            raf.seek(offset)

            this.remainingBytes = length
        }

        override fun available(): Int {
            return remainingBytes
        }

        @Throws(IOException::class)
        override fun close() {
            raf.close()
        }

        override fun markSupported(): Boolean {
            return false
        }

        @Throws(IOException::class)
        override fun read(buffer: ByteArray?, offset: Int, length: Int): Int {
            val read = raf.read(buffer, offset, if (length > remainingBytes) remainingBytes else length)

            remainingBytes -= read
            return read
        }

        @Throws(IOException::class)
        override fun read(): Int {
            if (remainingBytes > 0) {
                remainingBytes--
                return raf.read()
            } else {
                throw IOException("End of stream")
            }
        }

        override fun skip(byteCount: Long): Long {
            return 0
        }
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private val FILE_SIZE_LIMIT = (1 * 1024 * 1024 * 1024 // 1GB
                ).toLong()
        private const val FILE_COPY_BUFFER_SIZE = 1024

        private const val VERSION = 4
        private const val TILE_SIZE = 256

        private const val U32_SIZE = 4
        private const val U64_SIZE = 8
    }
}
