/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.osmdroid.samplefragments.cache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.util.StorageUtils
import org.osmdroid.views.MapView

/**
 * @author alex
 * @see org.osmdroid.PreferenceActivity
 *
 * @see org.osmdroid.intro.StoragePreferenceFragment
 */
class SampleAlternateCacheDir : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Alt Cache Dir"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // This is how to manually change the cache dir, this is commented out since the changes
        // as of 5.6 reworked how the cache is managed at startup. If you change the cache dir
        // make sure ou do it BEFORE creating the map view, either programmatically or via
        // an inflater call.

        //get the list of all mount points

        val storageList = StorageUtils.getStorageList(getActivity())


        //loop over them to find a writable location
        //or do whatever you need to do to select a new tile cache path.

        //then set it to the current tile cache location. must be done BEFORE creating the map
        //note this is before setContentView. The other option is it bounce the tile provider
        //via mMapView.setTileProvider();

        //Configuration.getInstance().setOsmdroidTileCache(adrive);


        //use the next line to change where osmdroid looks for offline tile archives
        //Configuration.getInstance().setOsmdroidBasePath(adrive);
        val v = inflater.inflate(R.layout.activity_starter_mapview, null)

        mMapView = v.findViewById<MapView?>(R.id.mapview)

        return v
    }
}
