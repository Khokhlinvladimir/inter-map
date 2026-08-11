package org.nocrala.tools.gis.data.esri.shapefile.util

object BAUtil {
    @JvmStatic
    fun displayByteArray(prompt: String?, b: ByteArray?) {
        if (b == null) {
            print(prompt + " byte array[]: null")
        } else {
            print(prompt + " byte array[" + b.size + "]: ")
            var isFirst = true
            for (i in b.indices) {
                if (isFirst) {
                    isFirst = false
                } else {
                    print(", ")
                }
                print(b[i])
            }
        }
        println()
    }
}
