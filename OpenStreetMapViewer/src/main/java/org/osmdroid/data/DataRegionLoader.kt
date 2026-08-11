package org.osmdroid.data

import android.content.Context
import androidx.annotation.RawRes
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.BoundingBox

/**
 * [DataRegion] json loader
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class DataRegionLoader(pContext: Context, @RawRes pResId: Int) : DataLoader<DataRegion?>(pContext, pResId) {
    @Throws(JSONException::class)
    override fun getItem(pKey: String?, pJsonObject: JSONObject?): DataRegion? {
        pJsonObject!!
        val name = pJsonObject.getString("name")
        val north = pJsonObject.getDouble("N")
        val east = pJsonObject.getDouble("E")
        val south = pJsonObject.getDouble("S")
        val west = pJsonObject.getDouble("W")
        return DataRegion(pKey, name, BoundingBox(north, east, south, west))
    }
}
