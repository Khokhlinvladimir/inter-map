package org.osmdroid.samplefragments.milstd2525

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import armyc2.c2sd.renderer.utilities.ModifiersTG
import armyc2.c2sd.renderer.utilities.ModifiersUnits
import armyc2.c2sd.renderer.utilities.RendererSettings
import armyc2.c2sd.renderer.utilities.SymbolUtilities
import org.osmdroid.R

/**
 * created on 1/15/2018.
 *
 * @author Alex O'Ree
 */
class ListPicker(cb: Callback?) : View.OnClickListener, OnItemClickListener, TextWatcher {
    interface Callback {
        fun selected(def: SimpleSymbol?)
    }

    var cb: Callback? = null
    var picker: AlertDialog? = null
    var milstd_search_cancel: Button? = null
    var milstd_search_results: ListView? = null
    var milstd_search: EditText? = null
    var milstd_search_affil_f: RadioButton? = null
    var milstd_search_affil_h: RadioButton? = null
    var milstd_search_affil_n: RadioButton? = null
    var milstd_search_affil_u: RadioButton? = null

    var charAffiliation: String = "F"

    init {
        this.cb = cb
    }

    fun destroy() {
        if (picker != null) {
            picker!!.dismiss()
        }
        picker = null
        cb = null
        milstd_search_cancel = null
        milstd_search_results = null
        milstd_search = null
    }

    fun show(activity: Activity) {
        if (picker != null) {
            picker!!.show()
            return
        }
        //prompt for input params
        val builder = AlertDialog.Builder(activity)

        val view = View.inflate(activity, R.layout.milstd2525list, null)

        milstd_search_affil_f = view.findViewById<RadioButton?>(R.id.milstd_search_affil_f)
        milstd_search_affil_h = view.findViewById<RadioButton?>(R.id.milstd_search_affil_h)
        milstd_search_affil_n = view.findViewById<RadioButton?>(R.id.milstd_search_affil_n)
        milstd_search_affil_u = view.findViewById<RadioButton?>(R.id.milstd_search_affil_u)

        milstd_search_affil_f!!.setOnClickListener(this)
        milstd_search_affil_h!!.setOnClickListener(this)
        milstd_search_affil_n!!.setOnClickListener(this)
        milstd_search_affil_u!!.setOnClickListener(this)

        milstd_search = view.findViewById<EditText?>(R.id.milstd_search)
        milstd_search!!.addTextChangedListener(this)
        milstd_search_results = view.findViewById<ListView?>(R.id.milstd_search_results)
        milstd_search_results!!.setAdapter(MilStdAdapter(activity))
        milstd_search_results!!.setOnItemClickListener(this)

        milstd_search_cancel = view.findViewById<Button?>(R.id.milstd_search_cancel)
        milstd_search_cancel!!.setOnClickListener(this)


        builder.setView(view)
        builder.setCancelable(true)
        builder.setOnCancelListener(object : DialogInterface.OnCancelListener {
            override fun onCancel(dialog: DialogInterface?) {
                picker!!.dismiss()
            }
        })
        picker = builder.create()
        picker!!.show()
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.milstd_search_cancel -> picker!!.dismiss()
            R.id.milstd_search_affil_f -> {
                charAffiliation = "F"
                (milstd_search_results!!.getAdapter() as MilStdAdapter).update(charAffiliation)
            }

            R.id.milstd_search_affil_h -> {
                charAffiliation = "H"
                (milstd_search_results!!.getAdapter() as MilStdAdapter).update(charAffiliation)
            }

            R.id.milstd_search_affil_n -> {
                charAffiliation = "N"
                (milstd_search_results!!.getAdapter() as MilStdAdapter).update(charAffiliation)
            }

            R.id.milstd_search_affil_u -> {
                charAffiliation = "U"
                (milstd_search_results!!.getAdapter() as MilStdAdapter).update(charAffiliation)
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val def = parent.getItemAtPosition(position) as SimpleSymbol
        if (cb != null) {
            //TODO this is a good place to show another dialog enabling the user to
            //symbol modifiers and attributes
            //modifiers are symbol specific
            //attributes are static and relatively simple

            val symbolCode = requireNotNull(def.symbolCode)
            if (symbolCode.startsWith("G") || symbolCode.startsWith("W")) {
                if (SymbolUtilities.canSymbolHaveModifier(
                        def.basicSymbolId,
                        ModifiersTG.A_SYMBOL_ICON,
                        RendererSettings.getInstance().getSymbologyStandard()
                    )
                ) {
                    //render some text input
                }
                //etc
            } else {
                if (SymbolUtilities.canSymbolHaveModifier(
                        def.basicSymbolId,
                        ModifiersUnits.A_SYMBOL_ICON,
                        RendererSettings.getInstance().getSymbologyStandard()
                    )
                ) {
                    //render some text input
                }
                //etc
            }


            picker!!.dismiss()
            var code = requireNotNull(def.basicSymbolId)
            if (code.get(1) == '*') {
                code = code.substring(0, 1) + charAffiliation + code.substring(2)
            }
            def.symbolCode = code
            cb!!.selected(def)
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        (milstd_search_results!!.getAdapter() as MilStdAdapter).getFilter().filter(s)
    }

    override fun afterTextChanged(s: Editable?) {
        (milstd_search_results!!.getAdapter() as MilStdAdapter).getFilter().filter(s)
    }
}
