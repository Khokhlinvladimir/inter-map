package org.osmdroid.samplefragments.milstd2525

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import armyc2.c2sd.renderer.MilStdIconRenderer
import armyc2.c2sd.renderer.utilities.MilStdAttributes
import armyc2.c2sd.renderer.utilities.RendererSettings
import armyc2.c2sd.renderer.utilities.SymbolDefTable
import armyc2.c2sd.renderer.utilities.SymbolUtilities
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.data.SampleGridlines
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.DecimalFormat

/**
 * A sample that provides two ways to plot single point MIL-STD 2525 icons
 *
 *
 *  * Via direct user input, enter the symbol code, then the icon is plotted at screen center
 *  * Via searchable picker, icons can then be plotted via long press
 *
 *
 *
 * TODO
 *
 *  * More support for modifiers and attributes
 *  * Multipoint symbols
 *
 * created on 12/22/2017.
 *
 * @author Alex O'Ree
 */
class Plotter : SampleGridlines(), View.OnClickListener, TextWatcher, ListPicker.Callback {
    private val MENU_ADD_POINT = Menu.FIRST
    private val MENU_ADD_VIA_PICKER = MENU_ADD_POINT + 1
    var mir: MilStdIconRenderer? = null
    var painting: ImageButton? = null
    var panning: ImageButton? = null
    var paint: MilStdCustomPaintingSurface? = null
    var textViewCurrentLocation: TextView? = null
    var lastSelectedSymbol: SimpleSymbol? = null
    var plotter: MilStdPointPlottingOverlay = MilStdPointPlottingOverlay()
    var picker: AlertDialog? = null
    var canRender: TextView? = null
    var symbolCode: EditText? = null

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    var symbolSize: EditText? = null
    var radio_milstd2525c: RadioButton? = null
    var radio_milstd2525b: RadioButton? = null
    var addIcon: Button? = null
    var cancelAddIcon: Button? = null
    var dpi: Int = 0

    init {
        //init the renderer

        RendererSettings.getInstance().setSymbologyStandard(RendererSettings.Symbology_2525C)
        //Next lines are mandatory.  These tell the renderer where the cache folder is located which is needed to process the embedded xml files.
        mir = MilStdIconRenderer.getInstance()
    }

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.map_with_location_milstd, container, false)

        mMapView = v.findViewById<MapView?>(R.id.mapview)
        textViewCurrentLocation = v.findViewById<TextView>(R.id.textViewCurrentLocation)
        panning = v.findViewById<ImageButton>(R.id.enablePanning)
        panning!!.setOnClickListener(this)
        panning!!.setBackgroundColor(Color.BLACK)
        painting = v.findViewById<ImageButton>(R.id.enablePainting)
        painting!!.setOnClickListener(this)

        val metrics = DisplayMetrics()
        getActivity()!!.getWindowManager().getDefaultDisplay().getMetrics(metrics)
        dpi = metrics.densityDpi
        paint = v.findViewById<MilStdCustomPaintingSurface>(R.id.paintingSurface)
        paint!!.init(mMapView)
        return v
    }

    override val sampleTitle: String?
        get() = "Symbol Plotter"


    public override fun addOverlays() {
        super.addOverlays()

        val cacheDir = getActivity()!!.getApplicationContext().getCacheDir().getAbsoluteFile().getAbsolutePath()
        mir!!.init(getContext(), cacheDir)
        mMapView!!.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onScroll " + event.x + "," + event.y)
                //Toast.makeText(getActivity(), "onScroll", Toast.LENGTH_SHORT).show();
                updateInfo()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onZoom " + event.zoomLevel)
                updateInfo()
                return true
            }
        })
        mMapView!!.controller!!.setZoom(15.0)
        mMapView!!.controller!!.setCenter(GeoPoint(41.0, -77.0))
        updateInfo()
        mMapView!!.getOverlayManager().add(plotter)
    }

    private fun updateInfo() {
        val mapCenter = mMapView!!.mapCenter
        textViewCurrentLocation!!.setText(
            ((if (plotter.def != null) plotter.def!!.symbolCode + "\n" else "") +
                    df.format(mapCenter!!.latitude) + "," +
                    df.format(mapCenter.longitude)
                    + ",zoom=" + mMapView!!.zoomLevelDouble)
        )
    }

    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_ADD_POINT, Menu.NONE, "Add a symbol by code")
        menu.add(0, MENU_ADD_VIA_PICKER, Menu.NONE, "Add a symbol by picker")
        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            MENU_ADD_POINT -> {
                showPicker()
                return true
            }

            MENU_ADD_VIA_PICKER -> {
                showSelector()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSelector() {
        //opens a dialog with a searchable list view of all single point symbols
        val picker = ListPicker(this)
        picker.show(getActivity()!!)
    }

    private fun showPicker() {
        if (picker != null) {
            picker!!.show()
            return
        }
        //prompt for input params
        val builder = AlertDialog.Builder(getActivity())

        val view = View.inflate(getActivity(), R.layout.milstd2525single, null)


        canRender = view.findViewById<TextView?>(R.id.canRender)
        symbolCode = view.findViewById<EditText?>(R.id.symbolCode)
        symbolCode!!.addTextChangedListener(this)
        symbolSize = view.findViewById<EditText?>(R.id.symbolSize)
        radio_milstd2525c = view.findViewById<RadioButton?>(R.id.radio_milstd2525c)
        radio_milstd2525b = view.findViewById<RadioButton?>(R.id.radio_milstd2525b)
        radio_milstd2525b!!.setOnClickListener(this)
        radio_milstd2525c!!.setOnClickListener(this)
        addIcon = view.findViewById<Button?>(R.id.addIcon)
        addIcon!!.setOnClickListener(this)
        addIcon!!.setEnabled(false)

        cancelAddIcon = view.findViewById<Button?>(R.id.cancelAddIcon)
        cancelAddIcon!!.setOnClickListener(this)


        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext())
        symbolCode!!.setText(defaultSharedPreferences.getString("MILSTDCODE", "SFGPUCI-----US-"))
        symbolSize!!.setText(defaultSharedPreferences.getInt("MILSTDSIZE", 128).toString() + "")

        builder.setView(view)
        builder.setCancelable(true)
        builder.setOnCancelListener(object : DialogInterface.OnCancelListener {
            override fun onCancel(dialog: DialogInterface?) {
                closePicker()
            }
        })
        picker = builder.create()
        picker!!.show()
        validateSymbolCode(symbolCode!!.getText().toString())
    }

    public override fun onPause() {
        super.onPause()
        closePicker()
    }

    private fun closePicker() {
        if (picker != null) picker!!.dismiss()
        picker = null

        canRender = null
        symbolCode = null
        symbolSize = null
        radio_milstd2525c = null
        radio_milstd2525b = null
        addIcon = null
        cancelAddIcon = null
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.radio_milstd2525b, R.id.radio_milstd2525c -> if ((v as RadioButton).isChecked()) {
                RendererSettings.getInstance().setSymbologyStandard(RendererSettings.Symbology_2525C)
            } else RendererSettings.getInstance().setSymbologyStandard(RendererSettings.Symbology_2525B)

            R.id.cancelAddIcon -> picker!!.dismiss()
            R.id.addIcon -> {
                //from the menu, user entered code
                val code = symbolCode!!.getText().toString()
                var size = 128
                try {
                    size = symbolSize!!.getText().toString().toInt()
                } catch (ex: Exception) {
                }
                val baseCode = SymbolUtilities.getBasicSymbolID(code)
                val def = SymbolDefTable.getInstance().getSymbolDef(baseCode, RendererSettings.getInstance().getSymbologyStandard())

                val attr = SparseArray<String?>()
                attr.put(MilStdAttributes.PixelSize, size.toString() + "")

                val ii = mir!!.RenderIcon(code, SparseArray<String?>(), attr)
                val m = Marker(mMapView!!)
                m.position = mMapView!!.mapCenter as GeoPoint
                m.setTitle(code)
                if (def != null) {
                    m.setSubDescription(def.getFullPath())
                    m.setSnippet(def.getDescription() + "\n" + def.getHierarchy())
                }
                val d: Drawable = BitmapDrawable(ii.getImage())
                m.image = d
                m.icon = d
                val centerX = ii.getCenterPoint().x //pixel center position
                //calculate what percentage of the center this value is
                val realCenterX = centerX.toFloat() / ii.getImage().getWidth().toFloat()

                val centerY = ii.getCenterPoint().y
                val realCenterY = centerY.toFloat() / ii.getImage().getHeight().toFloat()
                m.setAnchor(realCenterX, realCenterY)
                mMapView!!.getOverlayManager().add(m)
                mMapView!!.invalidate()
                picker!!.dismiss()

                //TODO store the symbol code and size as an android preference
                val edit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                edit.putString("MILSTDCODE", code)
                RendererSettings.getInstance().setDefaultPixelSize(size)
                edit.putInt("MILSTDSIZE", size)
                edit.commit()
            }

            R.id.enablePanning -> enablePanning()

            R.id.enablePainting -> enablePainting()
        }
    }

    private fun enablePanning() {
        paint!!.setVisibility(View.GONE)
        panning!!.setBackgroundColor(Color.BLACK)
        painting!!.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun enablePainting() {
        paint!!.setVisibility(View.VISIBLE)
        painting!!.setBackgroundColor(Color.BLACK)
        panning!!.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        validateSymbolCode(s.toString())
    }

    private fun validateSymbolCode(code: String?) {
        //validate that the input is correct

        if (code == null || code.length == 15) {
            if (mir!!.CanRender(code, SparseArray<String?>(), SparseArray<String?>())) {
                canRender!!.setText("")
                addIcon!!.setEnabled(true)
            } else {
                canRender!!.setText("Invalid Input.")
                addIcon!!.setEnabled(false)
            }
        } else {
            canRender!!.setText("Wrong length, must be 15 characters.")
            addIcon!!.setEnabled(false)
        }
    }

    override fun selected(def: SimpleSymbol?) {
        if (def == null) {
            enablePanning()
        }
        if (def!!.canDraw()) {
            val picker = ModifierPicker()
            picker.show(requireActivity(), def)

            if (def.maxPoints == 1) {
                enablePanning()
                plotter.setSymbol(def)
                Toast.makeText(getActivity(), "Long press to plot!", Toast.LENGTH_SHORT).show()
            }
            if (def.minPoints > 1) {
                enablePainting()
                paint!!.setSymbol(def)
                Toast.makeText(getActivity(), "Draw on the screen!", Toast.LENGTH_SHORT).show()
            }
        } else {
            enablePanning()
            Toast.makeText(getActivity(), "Symbol cannot be plotted, try another!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        val df: DecimalFormat = DecimalFormat("#.000000")
    }
}
