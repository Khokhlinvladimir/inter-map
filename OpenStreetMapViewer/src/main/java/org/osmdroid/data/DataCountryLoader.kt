package org.osmdroid.data

import android.content.Context
import androidx.annotation.RawRes
import org.json.JSONException
import org.json.JSONObject

/**
 * [DataCountry] json loader
 *
 * @author Fabrice Fontaine
 * @since 6.0.3
 */
class DataCountryLoader(pContext: Context, @RawRes pResId: Int) : DataLoader<DataCountry?>(pContext, pResId) {
    @Throws(JSONException::class)
    override fun getItem(pKey: String?, pJsonObject: JSONObject?): DataCountry {
        pJsonObject!!
        val name = pJsonObject.getString("name")
        val capital = pJsonObject.getJSONObject("capital")
        val capitalName = capital.getString("name")
        val latitude = capital.getDouble("latitude")
        val longitude = capital.getDouble("longitude")
        return DataCountry(pKey, name, capitalName, latitude, longitude)
    }
}
