package org.osmdroid.samplefragments.tilesources

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.MapBoxTileSource

/**
 * Example for accessing a MapBox map source
 *
 *
 * Created by alex on 10/18/15.
 */
class SampleMapBox : BaseSampleFragment() {
    var alertDialog: AlertDialog? = null
    var promptsView: View? = null

    override val sampleTitle: String
        get() = "MapBox"

    public override fun addOverlays() {
        //Since we distribute the sample app without any map box access tokens or maps, we prompt here for the user
        //to enter this information. If you're using this as a sample for your app, consider the following
        //this bit gets the key from the manifest

        /*
        MapBoxTileSource b=new MapBoxTileSource("MapBox",0,19,256, ".png");
        b.retrieveAccessToken(getContext());
        b.retrieveMapBoxMapId(getContext());
        //you can also programmatically set the token and map id here
        //b.setAppId("KEY");
        //b.setMapboxMapid("KEY");

        this.mMapView.setTileSource(b);
        */

        //End notes


        // get prompts.xml view


        val li = LayoutInflater.from(getActivity())
        promptsView = li.inflate(R.layout.mapbox_prompt, null)

        val alertDialogBuilder = AlertDialog.Builder(
            getActivity()
        )

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView)

        val userInputBoxId = promptsView!!
            .findViewById<EditText>(R.id.editTextDialogUserInputMapboxId)

        val userInputToken = promptsView!!
            .findViewById<EditText>(R.id.editTextDialogUserInputMapboxAccessToken)

        // set dialog message
        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton(
                "OK",
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, id: Int) {
                        // get user inputs and set the map source
                        //this bit gets the key from the manifest

                        val b = MapBoxTileSource("MapBox", 0, 19, 256, ".png")
                        b.setMapboxMapid(userInputBoxId.getText().toString())
                        b.accessToken = userInputToken.getText().toString()
                        mMapView!!.setTileSource(b)
                    }
                })
            .setNegativeButton(
                "Cancel",
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, id: Int) {
                        dialog.cancel()
                    }
                })

        // create alert dialog
        alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog!!.show()
    }

    public override fun onPause() {
        super.onPause()
        if (alertDialog != null && alertDialog!!.isShowing()) {
            alertDialog!!.dismiss()
        }
    }

    public override fun onDestroyView() {
        super.onDestroyView()
        if (alertDialog != null && alertDialog!!.isShowing()) {
            alertDialog!!.dismiss()
            alertDialog = null
        }
    }
}
