package org.osmdroid.samplefragments.data.utils

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

/**
 * Based an a SO answer, modified to meet needs
 *
 *
 * only suitable for small objects
 * http://stackoverflow.com/a/13196451/1203182
 *
 * @since 5.6.3
 */
class JSONParser  // constructor
{
    // function get json from url
    // by making HTTP POST or GET method
    @Throws(IOException::class)
    fun makeHttpRequest(url: String?): JSONObject? {
        var `is`: InputStream? = null
        var jObj: JSONObject? = null
        var json: String? = null
        // Making HTTP request
        try {
            `is` = URL(url).openStream()
        } catch (ex: Exception) {
            Log.d("Networking", ex.getLocalizedMessage())
            throw IOException("Error connecting")
        }

        try {
            val reader = BufferedReader(
                InputStreamReader(
                    `is`, "iso-8859-1"
                ), 8
            )
            val sb = StringBuilder()
            var line: String? = null
            while ((reader.readLine().also { line = it }) != null) {
                sb.append(line + "\n")
            }
            json = sb.toString()
            reader.close()
        } catch (e: Exception) {
            Log.e("Buffer Error", "Error converting result " + e.toString())
        } finally {
            try {
                `is`.close()
            } catch (ex: Exception) {
            }
        }

        // try parse the string to a JSON object
        try {
            jObj = JSONObject(json)
        } catch (e: JSONException) {
            Log.e("JSON Parser", "Error parsing data " + e.toString())
        }

        // return JSON String
        return jObj
    }
}