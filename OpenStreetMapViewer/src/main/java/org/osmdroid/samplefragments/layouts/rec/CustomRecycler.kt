package org.osmdroid.samplefragments.layouts.rec

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * created on 1/13/2017.
 *
 * @author PalilloKun
 * https://github.com/PalilloKun/SampleMapWithRecyclerView/blob/master/app/src/main/java/com/palitokun/mapwrecyclerview/CustomRecycler/CustomRecycler.java
 */

/**
 * Custom Adapter for Recycler data
 *
 * @author PalilloKun
 */
class CustomRecycler(var data: ArrayList<Info>) : RecyclerView.Adapter<CustomRecycler.ViewHolder?>() {
    var contextActual: Context? = null
    var list: ArrayList<String?>? = null


    open class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    /*
     *  Class for map layout
     * */
    inner class MapViewHolder(v: View) : ViewHolder(v) {
        var mapaShow: MapView

        init {
            this.mapaShow = v.findViewById<MapView>(R.id.mapShow)
        }
    }

    /*
     * Class for infodata layout
     * */
    inner class InfoDataViewHolder(v: View) : ViewHolder(v) {
        var TitleInfoTxt: TextView
        var ContentInfodata: TextView


        init {
            this.TitleInfoTxt = v.findViewById<TextView>(R.id.TitleInfoTxt)
            this.ContentInfodata = v.findViewById<TextView>(R.id.ContentInfodata)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v: View

        /*
         *   viewType = 1, is a Map
         *   viewType = 2, is a Graphic
         *   viewType = 3, is a InfoData
         *
         *   In this example, only put two layouts: Map and Info
         * */
        if (viewType == 1 || viewType == 8) {
            v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recyclerviewcard, viewGroup, false)
            return MapViewHolder(v)
        } else {
            v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recyclercard2, viewGroup, false)
            return InfoDataViewHolder(v)
        }
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        //For Info data

        if (viewHolder.getItemViewType() != 1 && viewHolder.getItemViewType() != 8) {
            val dat = data.get(position)
            val Indicador = viewHolder as InfoDataViewHolder

            Indicador.TitleInfoTxt.setText(dat.title)
            Indicador.ContentInfodata.setText(dat.content)
        } else {
            val dat = data.get(position)
            val Indicador = viewHolder as MapViewHolder
            Indicador.mapaShow.setMultiTouchControls(true)
            Indicador.mapaShow.setClickable(false)


            //on osmdroid-android v5.6.5 and older AND API16 or newer, uncomment the following
            //Indicador.mapaShow.setHasTransientState(true);
            Indicador.mapaShow.controller!!.setZoom(14)
            Indicador.mapaShow.controller!!.setCenter(GeoPoint(-25.2961407, -57.6309129))
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        //return mDataSetTypes[position];
        return requireNotNull(data.get(position).typeLayout).toInt()
    }
}
