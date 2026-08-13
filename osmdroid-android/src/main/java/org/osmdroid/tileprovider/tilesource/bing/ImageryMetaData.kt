package org.osmdroid.tileprovider.tilesource.bing

import org.json.JSONObject

/**
 * ImageryMetaData storage. Class used to decode valid ImageryMetaData.
 */
object ImageryMetaData {
    // Useful fields found in ImageryMetaData response
    private const val STATUS_CODE = "statusCode"
    private const val AUTH_RESULT_CODE = "authenticationResultCode"
    private const val AUTH_RESULT_CODE_VALID = "ValidCredentials"
    private const val RESOURCE_SETS = "resourceSets"
    private const val ESTIMATED_TOTAL = "estimatedTotal"
    private const val RESOURCE = "resources"

    /**
     * Parse a JSON string containing ImageryMetaData response
     *
     * @param a_jsonContent the JSON content string
     * @throws Exception
     * @return ImageryMetaDataResource object containing parsed information
     */
    @Throws(Exception::class)
    fun getInstanceFromJSON(a_jsonContent: String): ImageryMetaDataResource {
        if (a_jsonContent == null) {
            throw Exception("JSON to parse is null")
        }

        /** response code should be 200 and authorization should be valid (valid BingMap key) */
        val jsonResult = JSONObject(a_jsonContent)
        val statusCode = jsonResult.getInt(STATUS_CODE)
        if (statusCode != 200) {
            throw Exception("Status code = " + statusCode)
        }

        if (AUTH_RESULT_CODE_VALID.compareTo(jsonResult.getString(AUTH_RESULT_CODE), ignoreCase = true) != 0) {
            throw Exception("authentication result code = " + jsonResult.getString(AUTH_RESULT_CODE))
        }

        // get first valid resource information
        val resultsSet = jsonResult.getJSONArray(RESOURCE_SETS)
        if (resultsSet == null || resultsSet.length() < 1) {
            throw Exception("No results set found in json response")
        }

        if (resultsSet.getJSONObject(0).getInt(ESTIMATED_TOTAL) <= 0) {
            throw Exception("No resource found in json response")
        }

        val resource = resultsSet.getJSONObject(0).getJSONArray(RESOURCE).getJSONObject(0)

        return ImageryMetaDataResource.Companion.getInstanceFromJSON(resource, jsonResult)
    }
}
