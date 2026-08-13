package org.osmdroid.samplefragments.milstd2525

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import armyc2.c2sd.renderer.MilStdIconRenderer
import armyc2.c2sd.renderer.utilities.MilStdAttributes
import armyc2.c2sd.renderer.utilities.RendererSettings
import armyc2.c2sd.renderer.utilities.SymbolDefTable
import armyc2.c2sd.renderer.utilities.UnitDefTable
import org.osmdroid.R
import java.util.Collections
import java.util.Locale

/**
 * created on 1/15/2018.
 *
 * @author Alex O'Ree
 */
class MilStdAdapter(context: Context) : ArrayAdapter<SimpleSymbol?>(context, R.layout.milstd2525searchitem), Filterable, Comparator<SimpleSymbol?> {
    var values: MutableList<SimpleSymbol?>? = ArrayList<SimpleSymbol?>()

    var charAffil: String = "F"
    private val adapterContext: Context = context
    var density: Float = 240f

    init {
        density = context.resources.displayMetrics.density
        resetSymbols()
    }

    private fun resetSymbols() {
        synchronized(values!!) {
            values!!.clear()
            val stringSymbolDefMap = SymbolDefTable.getInstance().GetAllSymbolDefs(RendererSettings.getInstance().getSymbologyStandard())
            for (def in stringSymbolDefMap.values) {
                val from = SimpleSymbol.createFrom(def)
                if (from.canDraw()) values!!.add(SimpleSymbol.createFrom(def))
            }

            val allUnitDefs = UnitDefTable.getInstance().getAllUnitDefs(RendererSettings.getInstance().getSymbologyStandard())
            for (def in allUnitDefs.values) {
                val from = SimpleSymbol.createFrom(def)
                if (from.canDraw()) values!!.add(from)
            }
            Collections.sort<SimpleSymbol?>(values, this)
        }
    }

    override fun getCount(): Int {
        synchronized(this) {
            if (values != null) return values!!.size
            return 0
        }
    }

    override fun getItem(position: Int): SimpleSymbol? {
        return values!!.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = adapterContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.milstd2525searchitem, parent, false)
        val milstd_search_result_preview = rowView.findViewById<ImageView>(R.id.milstd_search_result_preview)
        val milstd_search_result_title = rowView.findViewById<TextView>(R.id.milstd_search_result_title)
        val milstd_search_result_hierarchy = rowView.findViewById<TextView>(R.id.milstd_search_result_hierarchy)
        val milstd_search_result_description = rowView.findViewById<TextView>(R.id.milstd_search_result_description)

        val def = getItem(position)

        if (def!!.description != null) milstd_search_result_description.setText(def.description)
        if (def.basicSymbolId != null) {
            milstd_search_result_title.setText(def.basicSymbolId)
        }
        if (def.hierarchy != null) milstd_search_result_hierarchy.setText(def.hierarchy)

        val attr = SparseArray<String?>()
        attr.put(MilStdAttributes.PixelSize, (45 * density).toInt().toString() + "")
        attr.put(MilStdAttributes.DrawAsIcon, "true")

        var code = requireNotNull(def.basicSymbolId)
        if (code.get(1) == '*') {
            code = code.substring(0, 1) + charAffil + code.substring(2)
        }


        //TODO mobility, country code, status, etc
        val ii = MilStdIconRenderer.getInstance().RenderIcon(code, SparseArray<String?>(), attr)
        if (ii != null) {
            val d: Drawable = BitmapDrawable(ii.getImage())
            milstd_search_result_preview.setImageDrawable(d)
        }
        return rowView
    }


    override fun getFilter(): Filter {
        return symbolFilter
    }

    private val symbolFilter: Filter = object : Filter() {
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            // NOTE: this function is *always* called from the UI thread.
            values = results.values as ArrayList<SimpleSymbol?>?
            notifyDataSetChanged()
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            // NOTE: this function is *always* called from a background thread, and
            // not the UI thread.
            var constraint = constraint
            val results = FilterResults()
            val filteredArrayNames = ArrayList<SimpleSymbol?>()

            resetSymbols()

            // perform your search here using the searchConstraint String.
            if (constraint == null || constraint.length == 0) {
                results.values = values
                results.count = values!!.size
            } else {
                constraint = constraint.toString().lowercase(Locale.getDefault())
                for (i in values!!.indices) {
                    try {
                        val dataNames = values!!.get(i)
                        val description = dataNames?.description
                        if (description != null && description.lowercase(Locale.getDefault()).contains(constraint)
                        ) {
                            filteredArrayNames.add(dataNames)
                        }
                    } catch (ex: Exception) {
                        break
                    }
                }

                results.count = filteredArrayNames.size
                results.values = filteredArrayNames
            }

            return results
        }
    }

    override fun compare(lhs: SimpleSymbol?, rhs: SimpleSymbol?): Int {
        if (lhs == null) return 0
        if (lhs.description == null) lhs.description = ""
        if (rhs == null) return 0
        if (rhs.description == null) rhs.description = ""
        return requireNotNull(lhs.description).compareTo(requireNotNull(rhs.description))
    }

    fun update(charAffiliation: String) {
        this.charAffil = charAffiliation
        notifyDataSetChanged()
    }
}
