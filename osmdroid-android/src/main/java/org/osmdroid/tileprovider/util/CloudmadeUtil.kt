package org.osmdroid.tileprovider.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class for implementing Cloudmade authorization. See
 * http://developers.cloudmade.com/projects/show/auth
 *
 *
 * The CloudMade token is persisted because it doesn't change:
 * http://support.cloudmade.com/answers/api-keys-and-authentication
 * "you will always get the same token for the unique user id"
 */
object CloudmadeUtil {
    var DEBUGMODE: Boolean = false

    /**
     * the meta data key in the manifest
     */
    private const val CLOUDMADE_KEY = "CLOUDMADE_KEY"

    /**
     * the key for the id preference
     */
    private const val CLOUDMADE_ID = "CLOUDMADE_ID"

    /**
     * the key for the token preference
     */
    private const val CLOUDMADE_TOKEN = "CLOUDMADE_TOKEN"

    private var mAndroidId: String? = Settings.Secure.ANDROID_ID // will get real id later

    /**
     * Get the key that was previously retrieved from the manifest.
     *
     * @return the key, or empty string if not found
     */
    /**
     * Get the key that was previously retrieved from the manifest.
     *
     * @return the key, or empty string if not found
     */
    /**
     * the key retrieved from the manifest
     */
    var cloudmadeKey: String = ""

    /**
     * the token
     */
    private var mToken = ""

    private var mPreferenceEditor: SharedPreferences.Editor? = null

    /**
     * Retrieve the key from the manifest and store it for later use.
     */
    fun retrieveCloudmadeKey(aContext: Context) {
        mAndroidId = Settings.Secure.getString(aContext.getContentResolver(), Settings.Secure.ANDROID_ID)

        // get the key from the manifest
        cloudmadeKey = ManifestUtil.retrieveKey(aContext, CLOUDMADE_KEY)

        // if the id hasn't changed then set the token to the previous token
        val pref = PreferenceManager.getDefaultSharedPreferences(aContext)
        mPreferenceEditor = pref.edit()
        val id: String = pref.getString(CLOUDMADE_ID, "")!!
        if (id == mAndroidId) {
            mToken = pref.getString(CLOUDMADE_TOKEN, "")!!
            // if we've got a token we don't need the editor any more
            if (mToken.length > 0) {
                mPreferenceEditor = null
            }
        } else {
            mPreferenceEditor!!.putString(CLOUDMADE_ID, mAndroidId)
            mPreferenceEditor!!.commit()
        }
    }

    val cloudmadeToken: String
        /**
         * Get the token from the Cloudmade server.
         *
         * @return the token returned from the server, or null if not found
         */
        get() {
            if (mToken.length == 0) {
                synchronized(mToken) {
                    // check again because it may have been set while we were blocking
                    if (mToken.length == 0) {
                        val url =
                            "https://auth.cloudmade.com/token/" + cloudmadeKey + "?userid=" + mAndroidId

                        var urlConnection: HttpURLConnection? = null
                        var br: BufferedReader? = null
                        var `is`: InputStreamReader? = null
                        try {
                            val urlToRequest = URL(url)
                            urlConnection = urlToRequest.openConnection() as HttpURLConnection?
                            urlConnection!!.setDoOutput(true)
                            urlConnection.setRequestMethod("POST")
                            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                            urlConnection.setRequestProperty(
                                instance!!.userAgentHttpHeader,
                                instance!!.userAgentValue
                            )
                            for (entry in instance!!.additionalHttpRequestProperties!!.entries) {
                                urlConnection.setRequestProperty(entry.key, entry.value)
                            }
                            urlConnection.connect()
                            if (DEBUGMODE) {
                                Log.d(IMapView.LOGTAG, "Response from Cloudmade auth: " + urlConnection.getResponseMessage())
                            }
                            if (urlConnection.getResponseCode() == 200) {
                                `is` = InputStreamReader(urlConnection.getInputStream(), "UTF-8")
                                br = BufferedReader(`is`, StreamUtils.IO_BUFFER_SIZE)
                                val line = br.readLine()
                                if (DEBUGMODE) {
                                    Log.d(IMapView.LOGTAG, "First line from Cloudmade auth: " + line)
                                }
                                mToken = line.trim { it <= ' ' }
                                if (mToken.length > 0) {
                                    mPreferenceEditor!!.putString(CLOUDMADE_TOKEN, mToken)
                                    mPreferenceEditor!!.commit()
                                    // we don't need the editor any more
                                    mPreferenceEditor = null
                                } else {
                                    Log.e(IMapView.LOGTAG, "No authorization token received from Cloudmade")
                                }
                            }
                        } catch (e: IOException) {
                            Log.e(IMapView.LOGTAG, "No authorization token received from Cloudmade: " + e)
                        } finally {
                            if (urlConnection != null) try {
                                urlConnection.disconnect()
                            } catch (ex: Exception) {
                            }
                            if (br != null) try {
                                br.close()
                            } catch (ex: Exception) {
                            }
                            if (`is` != null) try {
                                `is`.close()
                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
            }

            return mToken
        }
}
