package org.nocrala.tools.gis.data.esri.shapefile.util

import java.util.Locale

object HexaUtil {
    @JvmStatic
    fun stringToByteArray(orig: String): ByteArray {
        val txt = orig.lowercase(Locale.getDefault())

        val sb = StringBuffer()
        for (i in 0 until txt.length) {
            if (txt.get(i) != ' ') {
                sb.append(txt.get(i))
            }
        }

        val packed = sb.toString()
        if (packed.length % 2 != 0) {
            throw RuntimeException(
                ("Must have even number of hexadigits, "
                        + "but has " + packed.length + ".")
            )
        }

        val result = ByteArray(packed.length / 2)
        var i = 0
        while (i < packed.length) {
            val left = hexaToDecimal(packed.get(i))
            val right = hexaToDecimal(packed.get(i + 1))
            val total = left * 16 + right
            result[i / 2] = if (total < 128) total.toByte() else (total - 256).toByte()
            i = i + 2
        }

        return result
    }

    @JvmStatic
    fun byteArrayToString(b: ByteArray): String {
        val sb = StringBuffer()
        for (i in b.indices) {
            val v = if (b[i] >= 0) b[i].toInt() else b[i] + 256
            val left = v / 16
            val right = v % 16
            sb.append(decimalToHexa(left))
            sb.append(decimalToHexa(right))
        }
        return sb.toString()
    }

    // Util
    private const val HEXA_DIGITS = "0123456789abcdef"

    private fun decimalToHexa(d: Int): Char {
        return HEXA_DIGITS.get(d)
    }

    private fun hexaToDecimal(c: Char): Int {
        for (i in 0 until HEXA_DIGITS.length) {
            if (c == HEXA_DIGITS.get(i)) {
                return i
            }
        }
        throw RuntimeException("Invalid hexa digit '" + c + "'.")
    }
}
