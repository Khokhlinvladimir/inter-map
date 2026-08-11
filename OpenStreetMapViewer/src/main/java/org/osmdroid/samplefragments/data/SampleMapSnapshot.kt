package org.osmdroid.samplefragments.data

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.R
import org.osmdroid.data.DataRegion
import org.osmdroid.data.DataRegionLoader
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.data.SampleMapSnapshot.MyAdapter.MyViewHolder
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.util.TileSystem
import org.osmdroid.util.TileSystemWebMercator
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.MapSnapshotable
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import kotlin.math.min

/**
 * Demo with the new "MapSnapshot" feature - a RecyclerView with bitmap maps of all USA states
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleMapSnapshot : BaseSampleFragment() {
    private val mTileSystem: TileSystem = TileSystemWebMercator()
    private val mMapSnapshots: MutableMap<String?, MapSnapshot?> = HashMap<String?, MapSnapshot?>()
    private val mBitmaps: MutableMap<String?, Bitmap?> = HashMap<String?, Bitmap?>()

    private inner class MyAdapter internal constructor(private val mDataSet: MutableList<DataRegion?>) : RecyclerView.Adapter<MyViewHolder>() {
        private val mDefaultBitmap: Bitmap
        private val mOverlays: MutableList<Overlay?>

        inner class MyViewHolder(pLinearLayout: LinearLayout) : RecyclerView.ViewHolder(pLinearLayout) {
            val mImageView: ImageView
            val mTextView: TextView
            val mProgressBar: ProgressBar

            init {
                mImageView = pLinearLayout.getChildAt(0) as ImageView
                mTextView = pLinearLayout.getChildAt(1) as TextView
                mProgressBar = pLinearLayout.getChildAt(2) as ProgressBar

                pLinearLayout.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        Toast.makeText(getActivity(), mTextView.getText(), Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        init {
            mDefaultBitmap = Bitmap.createBitmap(mMapSize, mMapSize, Bitmap.Config.ARGB_8888)
            mOverlays = ArrayList<Overlay?>()
            mOverlays.add(mScaleBarOverlay)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val linearLayout = LinearLayout(getActivity())
            linearLayout.setOrientation(LinearLayout.VERTICAL)
            val imageView = ImageView(getActivity())
            imageView.setImageBitmap(mDefaultBitmap)
            linearLayout.addView(imageView)
            linearLayout.addView(TextView(getActivity()))
            val progressBar = ProgressBar(getActivity())
            progressBar.setIndeterminate(true)
            linearLayout.addView(progressBar)
            return MyViewHolder(linearLayout)
        }

        override fun onBindViewHolder(holder: MyViewHolder, pPosition: Int) {
            val dataRegion = mDataSet.get(pPosition)
            if (dataRegion == null) { // should never happen
                return
            }
            val key = dataRegion.iSO3166
            holder.mTextView.setText(dataRegion.name)
            val bitmap = mBitmaps.get(key)
            if (bitmap != null) {
                holder.mImageView.setImageBitmap(bitmap)
                holder.mProgressBar.setVisibility(View.INVISIBLE)
                return
            }
            holder.mImageView.setImageBitmap(mDefaultBitmap)
            holder.mProgressBar.setVisibility(View.VISIBLE)
            download(dataRegion)
        }

        override fun getItemCount(): Int {
            return mDataSet.size
        }

        private fun download(pDataRegion: DataRegion) {
            val key = pDataRegion.iSO3166
            if (mMapSnapshots.get(key) != null) {
                return  // pending
            }
            val zoom = mTileSystem.getBoundingBoxZoom(
                pDataRegion.box, mMapSize - 2 * mBorderSize, mMapSize - 2 * mBorderSize
            )
            val mapTileProvider: MapTileProviderBase = MapTileProviderBasic(getActivity())
            val mapSnapshot = MapSnapshot(
                object : MapSnapshotable {
                    override fun callback(pMapSnapshot: MapSnapshot) {
                        if (pMapSnapshot.getStatus() != MapSnapshot.Status.CANVAS_OK) {
                            return
                        }
                        val bitmap = Bitmap.createBitmap(pMapSnapshot.getBitmap())
                        mBitmaps.put(key, bitmap)
                        mMapSnapshots.get(key)!!.onDetach()
                        mMapSnapshots.remove(key)
                        if (mAdapter == null) {
                            return
                        }
                        getActivity()!!.runOnUiThread(object : Runnable {
                            override fun run() {
                                mAdapter!!.notifyDataSetChanged()
                            }
                        })
                    }
                }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProvider, mOverlays,
                Projection(zoom, mMapSize, mMapSize, pDataRegion.box!!.getCenterWithDateLine(), 0f, true, true, 0, 0)
            )
            mMapSnapshots.put(key, mapSnapshot)
            Thread(mapSnapshot).start() // TODO use AsyncTask, Executors instead?
        }
    }

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mMapSize = 0
    private var mBorderSize = 0

    override val sampleTitle: String
        get() = "MapSnapshot RecyclerView"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val displayMetrics = getActivity()!!.getResources().getDisplayMetrics()
        mMapSize = min(displayMetrics.widthPixels, displayMetrics.heightPixels)
        mBorderSize = mMapSize / 15

        mScaleBarOverlay = ScaleBarOverlay(getActivity(), mMapSize, mMapSize)
        mScaleBarOverlay!!.setCentred(true)
        mScaleBarOverlay!!.setScaleBarOffset(mMapSize / 2, 10)

        val recyclerView = RecyclerView(getActivity()!!)
        recyclerView.setHasFixedSize(true)

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(getActivity())
        recyclerView.setLayoutManager(layoutManager)

        try {
            val dataRegionLoader = DataRegionLoader(getActivity()!!, R.raw.data_region_usstates)
            mAdapter = MyAdapter(ArrayList<DataRegion?>(dataRegionLoader.list.values))
            recyclerView.setAdapter(mAdapter)
        } catch (e: Exception) {
            // DataRegionLoader KO, not supposed to happen
        }

        return recyclerView
    }

    override fun onDetach() {
        mAdapter = null
        mScaleBarOverlay!!.onDetach(null)
        for (key in mMapSnapshots.keys) {
            val mapSnapshot = mMapSnapshots.get(key)
            if (mapSnapshot != null) {
                mapSnapshot.onDetach()
            }
        }
        mMapSnapshots.clear()
        super.onDetach()
    }
}
