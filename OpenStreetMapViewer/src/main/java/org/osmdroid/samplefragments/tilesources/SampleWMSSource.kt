package org.osmdroid.samplefragments.tilesources

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import org.osmdroid.R
import org.osmdroid.samplefragments.data.SampleGridlines
import org.osmdroid.wms.WMSEndpoint
import org.osmdroid.wms.WMSParser
import org.osmdroid.wms.WMSParser.parse
import org.osmdroid.wms.WMSTileSource
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A simple demo work for working with WMS endpoints. Tested and functional with geo server
 * and
 * created on 8/20/2017.
 *
 * @author Alex O'Ree
 * @see WMSLayer
 *
 * @see WMSParser
 *
 * @see WMSEndpoint
 *
 * @since 5.6.5
 */
open class SampleWMSSource : SampleGridlines() {
    var show: AlertDialog? = null
    var layerPicker: AlertDialog? = null
    var alertDialog: AlertDialog? = null
    var switchMenu: MenuItem? = null

    //this model represents our WMS server, it's "capabilities"
    var cap: WMSEndpoint? = null

    override val sampleTitle: String?
        get() = "WMS Source"

    protected open val defaultUrl: String
        get() =//"http://192.168.1.1:8080/geoserver/ows?service=wms&version=1.1.1&request=GetCapabilities"
            "http://localhost:8080/geoserver/ows?service=wms&version=1.1.1&request=GetCapabilities"

    public override fun addOverlays() {
        super.addOverlays()
        // prompt for a WMS server
        val alert = AlertDialog.Builder(getContext())
        val edittext = EditText(getContext())
        edittext.setText(this.defaultUrl)
        alert.setMessage("Enter WMS Server Location")
        alert.setTitle("WMS Demo")

        alert.setView(edittext)

        alert.setPositiveButton("OK", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, whichButton: Int) {
                val YouEditTextValue = edittext.getText().toString()

                downloadAndParse(YouEditTextValue)
                show!!.dismiss()
            }
        })

        alert.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, whichButton: Int) {
                // what ever you want to do with No option.
                show!!.dismiss()
            }
        })

        show = alert.show()
    }

    private fun downloadAndParse(youEditTextValue: String?) {
        Thread(object : Runnable {
            override fun run() {
                var ok = false
                var root: Exception? = null
                try {
                    var c: HttpURLConnection? = null
                    var `is`: InputStream? = null
                    try {
                        c = URL(youEditTextValue).openConnection() as HttpURLConnection?
                        `is` = c!!.getInputStream()
                        cap = parse(`is`)
                        ok = true
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        root = ex
                    } finally {
                        if (`is` != null) try {
                            `is`.close()
                        } catch (ex: Exception) {
                        }
                        if (c != null) try {
                            c.disconnect()
                        } catch (ex: Exception) {
                        }
                    }
                } catch (ex: Exception) {
                    root = ex
                    ex.printStackTrace()
                }

                if (ok) {
                    promptUserForLayerSelection()
                } else {
                    showErrorMessage(root!!)
                }
            }
        }).start()
    }

    private fun showErrorMessage(root: Exception) {
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                alertDialog = AlertDialog.Builder(getActivity()).create()
                alertDialog!!.setTitle("Error")
                alertDialog!!.setMessage("There was an error communicating with the server: \n" + root.message)
                alertDialog!!.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            dialog.dismiss()
                        }
                    })
                alertDialog!!.show()
            }
        })
    }

    private fun promptUserForLayerSelection() {
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                val builderSingle = AlertDialog.Builder(getActivity())
                builderSingle.setIcon(R.drawable.icon)
                builderSingle.setTitle("Select A Layer")

                val arrayAdapter = ArrayAdapter<String?>(getActivity()!!, android.R.layout.select_dialog_singlechoice)
                for (i in cap!!.layers.indices) {
                    arrayAdapter.add(cap!!.layers.get(i).title)
                }


                builderSingle.setNegativeButton("cancel", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        layerPicker!!.dismiss()
                    }
                })

                builderSingle.setAdapter(arrayAdapter, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        val strName = arrayAdapter.getItem(which)
                        for (layer in cap!!.layers) {
                            if (strName == layer.title) {
                                val source = WMSTileSource.createFrom(cap!!, layer)
                                if (layer.bbox != null) {
                                    //center map on this location
                                    try {
                                        //double centerLat = (Double.parseDouble(layer.getBbox().getMaxy()) + Double.parseDouble(layer.getBbox().getMiny())) / 2;
                                        //double centerLon = (Double.parseDouble(layer.getBbox().getMaxx()) + Double.parseDouble(layer.getBbox().getMinx())) / 2;
                                        //mMapView.getController().animateTo(new GeoPoint(centerLat, centerLon));

                                        mMapView!!.zoomToBoundingBox(layer.bbox, true)
                                        mMapView!!.zoomToBoundingBox(layer.bbox, true)
                                        mMapView!!.zoomToBoundingBox(layer.bbox, true)
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }

                                mMapView!!.setTileSource(source)

                                break
                            }
                        }
                        layerPicker!!.dismiss()
                    }
                })
                layerPicker = builderSingle.show()
            }
        })
    }

    public override fun onPause() {
        super.onPause()
        if (alertDialog != null && alertDialog!!.isShowing()) {
            alertDialog!!.dismiss()
        }
        if (show != null && show!!.isShowing()) {
            show!!.dismiss()
        }
        if (layerPicker != null && layerPicker!!.isShowing()) {
            layerPicker!!.dismiss()
        }
    }

    /* android context menu */ /* android context menu */ /* android context menu */
    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        switchMenu = menu.add("Switch WMS Layer")
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (switchMenu === item) {
            if (layerPicker != null) {
                layerPicker!!.show()
            }
        }

        return super.onOptionsItemSelected(item)
    } /* END android context menu */ /* END android context menu */ /* END android context menu */
}
