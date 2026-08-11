// Created by plusminus on 2:27:27 AM - Mar 6, 2009
package org.osmdroid.mtp.util

import java.io.File

object FolderDeleter {
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
    /**
     * @return `true` on success, `false` otherwise.
     */
    fun deleteFolder(pFolder: File): Boolean {
        val children = pFolder.listFiles()

        for (c in children!!) {
            if (c.isDirectory()) {
                if (!deleteFolder(c)) {
                    System.err.println("Could not delete " + c.getAbsolutePath())
                    return false
                }
            } else {
                if (!c.delete()) {
                    System.err.println("Could not delete " + c.getAbsolutePath())
                    return false
                }
            }
        }
        return pFolder.delete()
    } // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
