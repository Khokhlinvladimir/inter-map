// Created by plusminus on 2:17:46 AM - Mar 6, 2009
package org.osmdroid.mtp.util

import org.osmdroid.tileprovider.util.StreamUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FolderZipper {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    // ===========================================================
    // Constructors
    // ===========================================================
    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    fun zipFolderToFile(pDestinationFile: File, pFolderToZip: File) {
        try {
            //create ZipOutputStream object
            val out = ZipOutputStream(FileOutputStream(pDestinationFile))

            var baseName = pFolderToZip.getParent()
            if (baseName == null) baseName = ""

            addFolderToZip(pFolderToZip, out, baseName)

            StreamUtils.closeStream(out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    @Throws(IOException::class)
    private fun addFolderToZip(folder: File, zip: ZipOutputStream, baseName: String) {
        val files = folder.listFiles()
        /* For each child (subdirectory/child-file). */
        for (file in files!!) {
            if (file.isDirectory()) {
                /* If the file is a folder, do recursrion with this folder.*/
                addFolderToZip(file, zip, baseName)
            } else {
                /* Otherwise zip it as usual. */
                var name = file.getPath().substring(baseName.length)
                if (name.startsWith(File.separator)) name = name.substring(1)
                println(name + " added")
                val zipEntry = ZipEntry(name)
                zip.putNextEntry(zipEntry)
                val fileIn = FileInputStream(file)
                StreamUtils.copy(fileIn, zip)
                StreamUtils.closeStream(fileIn)
                zip.closeEntry()
            }
        }
    } // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
