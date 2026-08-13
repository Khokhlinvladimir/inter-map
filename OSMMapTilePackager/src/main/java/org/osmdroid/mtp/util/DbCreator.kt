package org.osmdroid.mtp.util

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.sql.DriverManager
import java.sql.SQLException

object DbCreator {
    @Throws(ClassNotFoundException::class, SQLException::class, FileNotFoundException::class, IOException::class)
    fun putFolderToDb(pDestinationFile: File, pFolderToPut: File) {
        pDestinationFile.delete()
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:" + pDestinationFile)
        val stat = conn.createStatement()
        stat.execute("CREATE TABLE tiles (key INTEGER PRIMARY KEY, provider TEXT, tile BLOB)")
        stat.close()
        val prep = conn.prepareStatement("insert into tiles values (?, ?, ?);")
        val listFiles = pFolderToPut.listFiles()
        if (listFiles != null) {
            for (zf in listFiles.indices) {
                val listFiles1 = listFiles[zf]!!.listFiles()
                if (listFiles1 != null) {
                    for (xf in listFiles1.indices)  //for(final File xf : zf.listFiles())
                    {
                        val listFiles2 = listFiles1[xf]!!.listFiles()
                        if (listFiles2 != null) {
                            for (yf in listFiles2.indices)  //for(final File yf : xf.listFiles())
                            {
                                // escaped path separator
                                // Windows -> \\
                                // Unix -> \/
                                val s: Array<String?> =
                                    listFiles2[yf].toString().split(("\\" + File.separator).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                val z = s[s.size - 3]!!.toLong()
                                val x = s[s.size - 2]!!.toLong()
                                val y = s[s.size - 1]!!.split(".png".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].toLong()
                                val index = (((z shl z.toInt()) + x) shl z.toInt()) + y
                                prep.setLong(1, index)
                                val provider = s[s.size - 4]
                                prep.setString(2, provider)
                                val image = ByteArray(listFiles2[yf]!!.length().toInt())
                                val str = FileInputStream(listFiles2[yf])
                                str.read(image)
                                str.close()
                                prep.setBytes(3, image)
                                prep.executeUpdate()
                            }
                        }
                    }
                }
            }
        }
        conn.setAutoCommit(false)
        prep.executeBatch()
        conn.setAutoCommit(true)
        conn.close()
    }
}
