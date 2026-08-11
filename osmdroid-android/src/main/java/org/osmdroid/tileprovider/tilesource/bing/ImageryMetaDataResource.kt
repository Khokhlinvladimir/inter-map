package org.osmdroid.tileprovider.tilesource.bing

import org.json.JSONObject

/**
 * ImageryMetaData storage. Class used to parse and store useful ImageryMetaData fields.
 */
class ImageryMetaDataResource {
    var copyright: String = ""

    /**
     * image height in pixels (256 as default value)
     */
    var m_imageHeight: Int = 256

    /**
     * image width in pixels (256 as default value)
     */
    var m_imageWidth: Int = 256

    /**
     * image url pattern
     */
    var m_imageUrl: String? = null

    /**
     * list of available sub domains. Can be null.
     */
    var m_imageUrlSubdomains: Array<String?>? = null

    /**
     * maximum zoom level (22 as default value for BingMap)
     */
    var m_zoomMax: Int = 22

    /**
     * minimum zoom level (1 as default value for BingMap)
     */
    var m_zoomMin: Int = 1

    /**
     * whether this imagery has been initialised
     */
    var m_isInitialised: Boolean = false

    // counter used to manage next available sub domain
    private var m_subdomainsCounter = 0

    @get:Synchronized
    val subDomain: String?
        /**
         * When several subdomains are available, get subdomain pointed by internal cycle counter on subdomains and increment this counter
         *
         * @return the subdomain string associated to current counter value.
         */
        get() {
            if (m_imageUrlSubdomains == null || m_imageUrlSubdomains!!.size <= 0) {
                return null
            }

            val result = m_imageUrlSubdomains!![m_subdomainsCounter]
            if (m_subdomainsCounter < m_imageUrlSubdomains!!.size - 1) {
                m_subdomainsCounter++
            } else {
                m_subdomainsCounter = 0
            }

            return result
        }

    companion object {
        // Useful fields
        private const val IMAGE_WIDTH = "imageWidth"
        private const val IMAGE_HEIGHT = "imageHeight"
        private const val IMAGE_URL = "imageUrl"
        private const val IMAGE_URL_SUBDOMAINS = "imageUrlSubdomains"
        private const val ZOOM_MIN = "ZoomMin"
        private const val ZOOM_MAX = "ZoomMax"
        private const val COPYRIGHT = "copyright"

        val defaultInstance: ImageryMetaDataResource
            /**
             * Get an instance with default values.
             *
             * @return
             */
            get() = ImageryMetaDataResource()

        /**
         * Parse a JSON string containing resource field of a ImageryMetaData response
         *
         * @param a_jsonObject the JSON content string
         * @throws Exception
         * @return ImageryMetaDataResource object containing parsed information
         */
        @Throws(Exception::class)
        fun getInstanceFromJSON(a_jsonObject: JSONObject, parent: JSONObject): ImageryMetaDataResource {
            val result = ImageryMetaDataResource()

            if (a_jsonObject == null) {
                throw Exception("JSON to parse is null")
            }
            result.copyright = parent.getString(COPYRIGHT)

            if (a_jsonObject.has(IMAGE_HEIGHT)) {
                result.m_imageHeight = a_jsonObject.getInt(IMAGE_HEIGHT)
            }
            if (a_jsonObject.has(IMAGE_WIDTH)) {
                result.m_imageWidth = a_jsonObject.getInt(IMAGE_WIDTH)
            }
            if (a_jsonObject.has(ZOOM_MIN)) {
                result.m_zoomMin = a_jsonObject.getInt(ZOOM_MIN)
            }
            if (a_jsonObject.has(ZOOM_MAX)) {
                result.m_zoomMax = a_jsonObject.getInt(ZOOM_MAX)
            }
            result.m_imageUrl = a_jsonObject.getString(IMAGE_URL)
            if (result.m_imageUrl != null && result.m_imageUrl!!.matches(".*?\\{.*?\\}.*?".toRegex())) {
                result.m_imageUrl = result.m_imageUrl!!.replace("\\{.*?\\}".toRegex(), "%s")
            }

            val subdomains = a_jsonObject.getJSONArray(IMAGE_URL_SUBDOMAINS)
            if (subdomains != null && subdomains.length() >= 1) {
                result.m_imageUrlSubdomains = arrayOfNulls<String>(subdomains.length())
                for (i in 0 until subdomains.length()) {
                    result.m_imageUrlSubdomains!![i] = subdomains.getString(i)
                }
            }

            result.m_isInitialised = true

            return result
        }
    }
}
