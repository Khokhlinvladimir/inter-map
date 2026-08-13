package org.osmdroid.samplefragments.milstd2525

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import armyc2.c2sd.renderer.utilities.ModifiersTG
import armyc2.c2sd.renderer.utilities.ModifiersUnits
import armyc2.c2sd.renderer.utilities.RendererSettings
import armyc2.c2sd.renderer.utilities.SymbolUtilities
import org.osmdroid.R
import org.osmdroid.samplefragments.milstd2525.SimpleSymbol.Echelon1
import org.osmdroid.samplefragments.milstd2525.SimpleSymbol.Echelon2
import java.util.Locale

/**
 * created on 1/30/2018.
 * 
 * @author Alex O'Ree
 */
class ModifierPicker : View.OnClickListener, TextWatcher {
    var picker: AlertDialog? = null
    var milstd_search_cancel: Button? = null

    var milstd_search: EditText? = null
    var milstd_search_affil_f: RadioButton? = null
    var milstd_search_affil_h: RadioButton? = null
    var milstd_search_affil_n: RadioButton? = null
    var milstd_search_affil_u: RadioButton? = null

    var charAffiliation: String = "F"

    fun destroy() {
        if (picker != null) {
            picker!!.dismiss()
        }
        picker = null

        milstd_search_cancel = null

        milstd_search = null
    }

    var symbol: SimpleSymbol? = null
    var AM_DISTANCE_edit: EditText? = null
    var AN_AZIMUTH_edit: EditText? = null
    var ANGLE_edit: EditText? = null
    var C_QUANTITY_edit: EditText? = null
    var H_ADDITIONAL_INFO_1_edit: EditText? = null
    var H1_ADDITIONAL_INFO_2_edit: EditText? = null
    var H2_ADDITIONAL_INFO_3_edit: EditText? = null
    var LENGTH_edit: EditText? = null
    var RADIUS_edit: EditText? = null
    var S_OFFSET_INDICATOR_edit: EditText? = null
    var W1_DTG_2_edit: EditText? = null
    var W_DTG_1_edit: EditText? = null
    var D_TASK_FORCE_INDICATOR_edit: EditText? = null
    var E_FRAME_SHAPE_MODIFIER_edit: EditText? = null
    var F_REINFORCED_REDUCED_edit: EditText? = null
    var G_STAFF_COMMENTS_edit: EditText? = null
    var J_EVALUATION_RATING_edit: EditText? = null
    var K_COMBAT_EFFECTIVENESS_edit: EditText? = null
    var L_SIGNATURE_EQUIP_edit: EditText? = null
    var M_HIGHER_FORMATION_edit: EditText? = null
    var N_HOSTILE_edit: EditText? = null
    var P_IFF_SIF_edit: EditText? = null
    var Q_DIRECTION_OF_MOVEMENT_edit: EditText? = null
    var R2_SIGNIT_MOBILITY_INDICATOR_edit: EditText? = null
    var T1_UNIQUE_DESIGNATION_2_edit: EditText? = null
    var T_UNIQUE_DESIGNATION_1_edit: EditText? = null
    var V_EQUIP_TYPE_edit: EditText? = null
    var X_ALTITUDE_DEPTH_edit: EditText? = null
    var Z_SPEED_edit: EditText? = null
    var AA_SPECIAL_C2_HQ_edit: EditText? = null
    var AB_FEINT_DUMMY_INDICATOR_edit: EditText? = null
    var AC_INSTALLATION_edit: EditText? = null
    var AD_PLATFORM_TYPE_edit: EditText? = null
    var AE_EQUIPMENT_TEARDOWN_TIME_edit: EditText? = null
    var AF_COMMON_IDENTIFIER_edit: EditText? = null
    var AG_AUX_EQUIP_INDICATOR_edit: EditText? = null
    var AH_AREA_OF_UNCERTAINTY_edit: EditText? = null
    var AI_DEAD_RECKONING_TRAILER_edit: EditText? = null
    var AJ_SPEED_LEADER_edit: EditText? = null
    var AK_PAIRING_LINE_edit: EditText? = null
    var AL_OPERATIONAL_CONDITION_edit: EditText? = null
    var AO_ENGAGEMENT_BAR_edit: EditText? = null
    var SCC_SONAR_CLASSIFICATION_CONFIDENCE_edit: EditText? = null
    var CN_CPOF_NAME_LABEL_edit: EditText? = null
    var COUNTRY_CODE_edit: EditText? = null
    var milstd_modifier_apply: Button? = null
    var echelon1: Spinner? = null
    var echelon2: Spinner? = null

    fun show(activity: Activity, symbol: SimpleSymbol) {
        if (picker != null) {
            picker!!.show()
            return
        }
        this.symbol = symbol
        //prompt for input params
        val builder = AlertDialog.Builder(activity)

        val view = View.inflate(activity, R.layout.milstd2525modifiers, null)

        milstd_modifier_apply = view.findViewById<Button?>(R.id.milstd_modifier_apply)
        milstd_modifier_apply!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                applyModifiers()
            }
        })
        milstd_search_affil_f = view.findViewById<RadioButton?>(R.id.milstd_search_affil_f)
        milstd_search_affil_h = view.findViewById<RadioButton?>(R.id.milstd_search_affil_h)
        milstd_search_affil_n = view.findViewById<RadioButton?>(R.id.milstd_search_affil_n)
        milstd_search_affil_u = view.findViewById<RadioButton?>(R.id.milstd_search_affil_u)

        COUNTRY_CODE_edit = view.findViewById<EditText?>(R.id.COUNTRY_edit)
        AM_DISTANCE_edit = view.findViewById<EditText?>(R.id.AM_DISTANCE_edit)
        AN_AZIMUTH_edit = view.findViewById<EditText?>(R.id.AN_AZIMUTH_edit)
        ANGLE_edit = view.findViewById<EditText?>(R.id.ANGLE_edit)
        C_QUANTITY_edit = view.findViewById<EditText?>(R.id.C_QUANTITY_edit)
        H_ADDITIONAL_INFO_1_edit = view.findViewById<EditText?>(R.id.H_ADDITIONAL_INFO_1_edit)
        H1_ADDITIONAL_INFO_2_edit = view.findViewById<EditText?>(R.id.H1_ADDITIONAL_INFO_2_edit)
        H2_ADDITIONAL_INFO_3_edit = view.findViewById<EditText?>(R.id.H2_ADDITIONAL_INFO_3_edit)
        LENGTH_edit = view.findViewById<EditText?>(R.id.LENGTH_edit)
        N_HOSTILE_edit = view.findViewById<EditText?>(R.id.N_HOSTILE_edit)
        RADIUS_edit = view.findViewById<EditText?>(R.id.RADIUS_edit)
        Q_DIRECTION_OF_MOVEMENT_edit = view.findViewById<EditText?>(R.id.Q_DIRECTION_OF_MOVEMENT_edit)
        S_OFFSET_INDICATOR_edit = view.findViewById<EditText?>(R.id.S_OFFSET_INDICATOR_edit)
        V_EQUIP_TYPE_edit = view.findViewById<EditText?>(R.id.V_EQUIP_TYPE_edit)
        W1_DTG_2_edit = view.findViewById<EditText?>(R.id.W1_DTG_2_edit)
        W_DTG_1_edit = view.findViewById<EditText?>(R.id.W_DTG_1_edit)
        T1_UNIQUE_DESIGNATION_2_edit = view.findViewById<EditText?>(R.id.T1_UNIQUE_DESIGNATION_2_edit)
        T_UNIQUE_DESIGNATION_1_edit = view.findViewById<EditText?>(R.id.T_UNIQUE_DESIGNATION_1_edit)


        D_TASK_FORCE_INDICATOR_edit = view.findViewById<EditText?>(R.id.D_TASK_FORCE_INDICATOR_edit)
        E_FRAME_SHAPE_MODIFIER_edit = view.findViewById<EditText?>(R.id.E_FRAME_SHAPE_MODIFIER_edit)
        F_REINFORCED_REDUCED_edit = view.findViewById<EditText?>(R.id.F_REINFORCED_REDUCED_edit)
        G_STAFF_COMMENTS_edit = view.findViewById<EditText?>(R.id.G_STAFF_COMMENTS_edit)
        J_EVALUATION_RATING_edit = view.findViewById<EditText?>(R.id.J_EVALUATION_RATING_edit)
        K_COMBAT_EFFECTIVENESS_edit = view.findViewById<EditText?>(R.id.K_COMBAT_EFFECTIVENESS_edit)
        L_SIGNATURE_EQUIP_edit = view.findViewById<EditText?>(R.id.L_SIGNATURE_EQUIP_edit)


        M_HIGHER_FORMATION_edit = view.findViewById<EditText?>(R.id.M_HIGHER_FORMATION_edit)
        N_HOSTILE_edit = view.findViewById<EditText?>(R.id.N_HOSTILE_edit)
        P_IFF_SIF_edit = view.findViewById<EditText?>(R.id.P_IFF_SIF_edit)
        Q_DIRECTION_OF_MOVEMENT_edit = view.findViewById<EditText?>(R.id.Q_DIRECTION_OF_MOVEMENT_edit)
        R2_SIGNIT_MOBILITY_INDICATOR_edit = view.findViewById<EditText?>(R.id.R2_SIGNIT_MOBILITY_INDICATOR_edit)
        T1_UNIQUE_DESIGNATION_2_edit = view.findViewById<EditText?>(R.id.T1_UNIQUE_DESIGNATION_2_edit)
        T_UNIQUE_DESIGNATION_1_edit = view.findViewById<EditText?>(R.id.T_UNIQUE_DESIGNATION_1_edit)
        V_EQUIP_TYPE_edit = view.findViewById<EditText?>(R.id.V_EQUIP_TYPE_edit)
        X_ALTITUDE_DEPTH_edit = view.findViewById<EditText?>(R.id.X_ALTITUDE_DEPTH_edit)
        Z_SPEED_edit = view.findViewById<EditText?>(R.id.Z_SPEED_edit)
        AA_SPECIAL_C2_HQ_edit = view.findViewById<EditText?>(R.id.AA_SPECIAL_C2_HQ_edit)
        AB_FEINT_DUMMY_INDICATOR_edit = view.findViewById<EditText?>(R.id.AB_FEINT_DUMMY_INDICATOR_edit)
        AC_INSTALLATION_edit = view.findViewById<EditText?>(R.id.AC_INSTALLATION_edit)
        AD_PLATFORM_TYPE_edit = view.findViewById<EditText?>(R.id.AD_PLATFORM_TYPE_edit)
        AE_EQUIPMENT_TEARDOWN_TIME_edit = view.findViewById<EditText?>(R.id.AE_EQUIPMENT_TEARDOWN_TIME_edit)
        AF_COMMON_IDENTIFIER_edit = view.findViewById<EditText?>(R.id.AF_COMMON_IDENTIFIER_edit)
        AG_AUX_EQUIP_INDICATOR_edit = view.findViewById<EditText?>(R.id.AG_AUX_EQUIP_INDICATOR_edit)
        AH_AREA_OF_UNCERTAINTY_edit = view.findViewById<EditText?>(R.id.AH_AREA_OF_UNCERTAINTY_edit)
        AI_DEAD_RECKONING_TRAILER_edit = view.findViewById<EditText?>(R.id.AI_DEAD_RECKONING_TRAILER_edit)
        AJ_SPEED_LEADER_edit = view.findViewById<EditText?>(R.id.AJ_SPEED_LEADER_edit)
        AK_PAIRING_LINE_edit = view.findViewById<EditText?>(R.id.AK_PAIRING_LINE_edit)
        AL_OPERATIONAL_CONDITION_edit = view.findViewById<EditText?>(R.id.AL_OPERATIONAL_CONDITION_edit)
        AO_ENGAGEMENT_BAR_edit = view.findViewById<EditText?>(R.id.AO_ENGAGEMENT_BAR_edit)


        SCC_SONAR_CLASSIFICATION_CONFIDENCE_edit = view.findViewById<EditText?>(R.id.SCC_SONAR_CLASSIFICATION_CONFIDENCE_edit)


        CN_CPOF_NAME_LABEL_edit = view.findViewById<EditText?>(R.id.CN_CPOF_NAME_LABEL_edit)

        //TODO set spinner adapters for echelons
        echelon1 = view.findViewById<Spinner>(R.id.echelon1)
        echelon1!!.setAdapter(ArrayAdapter<Echelon1?>(activity, android.R.layout.simple_spinner_item, Echelon1.values()))

        echelon2 = view.findViewById<Spinner>(R.id.echelon2)
        echelon2!!.setAdapter(ArrayAdapter<Echelon2?>(activity, android.R.layout.simple_spinner_item, Echelon2.values()))
        val baseCode = symbol.basicSymbolId

        applyVisibility(baseCode!!, view)


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

    private fun applyModifiers() {
        val baseCode = symbol!!.basicSymbolId
        val modifiers = symbol!!.modifiers
        modifiers!!.clear()
        if (baseCode!!.get(0) != 'W') {
            //apply country code and echelons 1 and 2
            var code = symbol!!.symbolCode

            val e1 = echelon1!!.getSelectedItem() as Echelon1
            val e2 = echelon2!!.getSelectedItem() as Echelon2
            var countryCode = COUNTRY_CODE_edit!!.getText().toString()
            if (countryCode != null && countryCode.length == 2) {
                countryCode = countryCode.uppercase(Locale.getDefault())
            } else countryCode = "--"
            code = code!!.substring(0, 10) + e1.value + e2.value + countryCode + "-" //FIXME

            //index 10 = echelon 1
            //index 11 = echelon 2
            //index 12-13 country code
            //index 14 orderbat
        }

        if (baseCode.get(0) == 'G' || baseCode.get(0) == 'W') {
            if (Companion.isDefined(AM_DISTANCE_edit!!)) modifiers.put(ModifiersTG.AM_DISTANCE, AM_DISTANCE_edit!!.getText().toString())
            if (Companion.isDefined(AM_DISTANCE_edit!!)) modifiers.put(ModifiersTG.AN_AZIMUTH, AN_AZIMUTH_edit!!.getText().toString())
            if (Companion.isDefined(ANGLE_edit!!)) modifiers.put(ModifiersTG.ANGLE, ANGLE_edit!!.getText().toString())
            if (Companion.isDefined(C_QUANTITY_edit!!)) modifiers.put(ModifiersTG.C_QUANTITY, C_QUANTITY_edit!!.getText().toString())
            if (Companion.isDefined(H_ADDITIONAL_INFO_1_edit!!)) modifiers.put(
                ModifiersTG.H_ADDITIONAL_INFO_1,
                H_ADDITIONAL_INFO_1_edit!!.getText().toString()
            )
            if (Companion.isDefined(H1_ADDITIONAL_INFO_2_edit!!)) modifiers.put(
                ModifiersTG.H1_ADDITIONAL_INFO_2,
                H1_ADDITIONAL_INFO_2_edit!!.getText().toString()
            )
            if (Companion.isDefined(H2_ADDITIONAL_INFO_3_edit!!)) modifiers.put(
                ModifiersTG.H2_ADDITIONAL_INFO_3,
                H2_ADDITIONAL_INFO_3_edit!!.getText().toString()
            )
            if (Companion.isDefined(LENGTH_edit!!)) modifiers.put(ModifiersTG.LENGTH, LENGTH_edit!!.getText().toString())
            if (Companion.isDefined(N_HOSTILE_edit!!)) modifiers.put(ModifiersTG.N_HOSTILE, N_HOSTILE_edit!!.getText().toString())
            if (Companion.isDefined(Q_DIRECTION_OF_MOVEMENT_edit!!)) modifiers.put(
                ModifiersTG.Q_DIRECTION_OF_MOVEMENT,
                Q_DIRECTION_OF_MOVEMENT_edit!!.getText().toString()
            )
            if (Companion.isDefined(RADIUS_edit!!)) modifiers.put(ModifiersTG.RADIUS, RADIUS_edit!!.getText().toString())
            if (Companion.isDefined(S_OFFSET_INDICATOR_edit!!)) modifiers.put(
                ModifiersTG.S_OFFSET_INDICATOR,
                S_OFFSET_INDICATOR_edit!!.getText().toString()
            )
            if (Companion.isDefined(V_EQUIP_TYPE_edit!!)) modifiers.put(ModifiersTG.V_EQUIP_TYPE, V_EQUIP_TYPE_edit!!.getText().toString())
            if (Companion.isDefined(W1_DTG_2_edit!!)) modifiers.put(ModifiersTG.W1_DTG_2, W1_DTG_2_edit!!.getText().toString())
            if (Companion.isDefined(W_DTG_1_edit!!)) modifiers.put(ModifiersTG.W_DTG_1, W_DTG_1_edit!!.getText().toString())
            if (Companion.isDefined(T1_UNIQUE_DESIGNATION_2_edit!!)) modifiers.put(
                ModifiersTG.T1_UNIQUE_DESIGNATION_2,
                T1_UNIQUE_DESIGNATION_2_edit!!.getText().toString()
            )
            if (Companion.isDefined(T_UNIQUE_DESIGNATION_1_edit!!)) modifiers.put(
                ModifiersTG.T_UNIQUE_DESIGNATION_1,
                T_UNIQUE_DESIGNATION_1_edit!!.getText().toString()
            )
            if (Companion.isDefined(X_ALTITUDE_DEPTH_edit!!)) modifiers.put(
                ModifiersTG.X_ALTITUDE_DEPTH,
                X_ALTITUDE_DEPTH_edit!!.getText().toString()
            )
        } else {
            if (Companion.isDefined(C_QUANTITY_edit!!)) modifiers.put(ModifiersUnits.C_QUANTITY, C_QUANTITY_edit!!.getText().toString())

            if (Companion.isDefined(D_TASK_FORCE_INDICATOR_edit!!)) modifiers.put(
                ModifiersUnits.D_TASK_FORCE_INDICATOR,
                D_TASK_FORCE_INDICATOR_edit!!.getText().toString()
            )

            if (Companion.isDefined(E_FRAME_SHAPE_MODIFIER_edit!!)) modifiers.put(
                ModifiersUnits.E_FRAME_SHAPE_MODIFIER,
                E_FRAME_SHAPE_MODIFIER_edit!!.getText().toString()
            )

            if (Companion.isDefined(F_REINFORCED_REDUCED_edit!!)) modifiers.put(
                ModifiersUnits.F_REINFORCED_REDUCED,
                F_REINFORCED_REDUCED_edit!!.getText().toString()
            )

            if (Companion.isDefined(G_STAFF_COMMENTS_edit!!)) modifiers.put(
                ModifiersUnits.G_STAFF_COMMENTS,
                G_STAFF_COMMENTS_edit!!.getText().toString()
            )

            if (Companion.isDefined(H_ADDITIONAL_INFO_1_edit!!)) modifiers.put(
                ModifiersUnits.H_ADDITIONAL_INFO_1,
                H_ADDITIONAL_INFO_1_edit!!.getText().toString()
            )

            if (Companion.isDefined(H1_ADDITIONAL_INFO_2_edit!!)) modifiers.put(
                ModifiersUnits.H1_ADDITIONAL_INFO_2,
                H1_ADDITIONAL_INFO_2_edit!!.getText().toString()
            )

            if (Companion.isDefined(H2_ADDITIONAL_INFO_3_edit!!)) modifiers.put(
                ModifiersUnits.H2_ADDITIONAL_INFO_3,
                H2_ADDITIONAL_INFO_3_edit!!.getText().toString()
            )


            if (Companion.isDefined(J_EVALUATION_RATING_edit!!)) modifiers.put(
                ModifiersUnits.J_EVALUATION_RATING,
                J_EVALUATION_RATING_edit!!.getText().toString()
            )


            if (Companion.isDefined(K_COMBAT_EFFECTIVENESS_edit!!)) modifiers.put(
                ModifiersUnits.K_COMBAT_EFFECTIVENESS,
                K_COMBAT_EFFECTIVENESS_edit!!.getText().toString()
            )


            if (Companion.isDefined(L_SIGNATURE_EQUIP_edit!!)) modifiers.put(
                ModifiersUnits.L_SIGNATURE_EQUIP,
                L_SIGNATURE_EQUIP_edit!!.getText().toString()
            )


            if (Companion.isDefined(M_HIGHER_FORMATION_edit!!)) modifiers.put(
                ModifiersUnits.M_HIGHER_FORMATION,
                M_HIGHER_FORMATION_edit!!.getText().toString()
            )


            if (Companion.isDefined(N_HOSTILE_edit!!)) modifiers.put(ModifiersUnits.N_HOSTILE, N_HOSTILE_edit!!.getText().toString())


            if (Companion.isDefined(P_IFF_SIF_edit!!)) modifiers.put(ModifiersUnits.P_IFF_SIF, P_IFF_SIF_edit!!.getText().toString())


            if (Companion.isDefined(Q_DIRECTION_OF_MOVEMENT_edit!!)) modifiers.put(
                ModifiersUnits.Q_DIRECTION_OF_MOVEMENT,
                Q_DIRECTION_OF_MOVEMENT_edit!!.getText().toString()
            )


            if (Companion.isDefined(R2_SIGNIT_MOBILITY_INDICATOR_edit!!)) modifiers.put(
                ModifiersUnits.R2_SIGNIT_MOBILITY_INDICATOR,
                R2_SIGNIT_MOBILITY_INDICATOR_edit!!.getText().toString()
            )


            if (Companion.isDefined(T1_UNIQUE_DESIGNATION_2_edit!!)) modifiers.put(
                ModifiersUnits.T1_UNIQUE_DESIGNATION_2,
                T1_UNIQUE_DESIGNATION_2_edit!!.getText().toString()
            )

            if (Companion.isDefined(T_UNIQUE_DESIGNATION_1_edit!!)) modifiers.put(
                ModifiersUnits.T_UNIQUE_DESIGNATION_1,
                T_UNIQUE_DESIGNATION_1_edit!!.getText().toString()
            )


            if (Companion.isDefined(V_EQUIP_TYPE_edit!!)) modifiers.put(ModifiersUnits.V_EQUIP_TYPE, V_EQUIP_TYPE_edit!!.getText().toString())

            if (Companion.isDefined(V_EQUIP_TYPE_edit!!)) modifiers.put(ModifiersUnits.V_EQUIP_TYPE, V_EQUIP_TYPE_edit!!.getText().toString())

            if (Companion.isDefined(X_ALTITUDE_DEPTH_edit!!)) modifiers.put(
                ModifiersUnits.X_ALTITUDE_DEPTH,
                X_ALTITUDE_DEPTH_edit!!.getText().toString()
            )


            if (Companion.isDefined(Z_SPEED_edit!!)) modifiers.put(ModifiersUnits.Z_SPEED, Z_SPEED_edit!!.getText().toString())


            if (Companion.isDefined(AA_SPECIAL_C2_HQ_edit!!)) modifiers.put(
                ModifiersUnits.AA_SPECIAL_C2_HQ,
                AA_SPECIAL_C2_HQ_edit!!.getText().toString()
            )


            if (Companion.isDefined(AB_FEINT_DUMMY_INDICATOR_edit!!)) modifiers.put(
                ModifiersUnits.AB_FEINT_DUMMY_INDICATOR,
                AB_FEINT_DUMMY_INDICATOR_edit!!.getText().toString()
            )


            if (Companion.isDefined(AC_INSTALLATION_edit!!)) modifiers.put(
                ModifiersUnits.AC_INSTALLATION,
                AC_INSTALLATION_edit!!.getText().toString()
            )


            if (Companion.isDefined(AD_PLATFORM_TYPE_edit!!)) modifiers.put(
                ModifiersUnits.AD_PLATFORM_TYPE,
                AD_PLATFORM_TYPE_edit!!.getText().toString()
            )


            if (Companion.isDefined(AE_EQUIPMENT_TEARDOWN_TIME_edit!!)) modifiers.put(
                ModifiersUnits.AE_EQUIPMENT_TEARDOWN_TIME,
                AE_EQUIPMENT_TEARDOWN_TIME_edit!!.getText().toString()
            )


            if (Companion.isDefined(AF_COMMON_IDENTIFIER_edit!!)) modifiers.put(
                ModifiersUnits.AF_COMMON_IDENTIFIER,
                AF_COMMON_IDENTIFIER_edit!!.getText().toString()
            )


            if (Companion.isDefined(AG_AUX_EQUIP_INDICATOR_edit!!)) modifiers.put(
                ModifiersUnits.AG_AUX_EQUIP_INDICATOR,
                AG_AUX_EQUIP_INDICATOR_edit!!.getText().toString()
            )


            if (Companion.isDefined(AH_AREA_OF_UNCERTAINTY_edit!!)) modifiers.put(
                ModifiersUnits.AH_AREA_OF_UNCERTAINTY,
                AH_AREA_OF_UNCERTAINTY_edit!!.getText().toString()
            )


            if (Companion.isDefined(AI_DEAD_RECKONING_TRAILER_edit!!)) modifiers.put(
                ModifiersUnits.AI_DEAD_RECKONING_TRAILER,
                AI_DEAD_RECKONING_TRAILER_edit!!.getText().toString()
            )


            if (Companion.isDefined(AJ_SPEED_LEADER_edit!!)) modifiers.put(
                ModifiersUnits.AJ_SPEED_LEADER,
                AJ_SPEED_LEADER_edit!!.getText().toString()
            )


            if (Companion.isDefined(AK_PAIRING_LINE_edit!!)) modifiers.put(
                ModifiersUnits.AK_PAIRING_LINE,
                AK_PAIRING_LINE_edit!!.getText().toString()
            )


            if (Companion.isDefined(AL_OPERATIONAL_CONDITION_edit!!)) modifiers.put(
                ModifiersUnits.AL_OPERATIONAL_CONDITION,
                AL_OPERATIONAL_CONDITION_edit!!.getText().toString()
            )


            if (Companion.isDefined(AL_OPERATIONAL_CONDITION_edit!!)) modifiers.put(
                ModifiersUnits.AL_OPERATIONAL_CONDITION,
                AL_OPERATIONAL_CONDITION_edit!!.getText().toString()
            )


            if (Companion.isDefined(AL_OPERATIONAL_CONDITION_edit!!)) modifiers.put(
                ModifiersUnits.AL_OPERATIONAL_CONDITION,
                AL_OPERATIONAL_CONDITION_edit!!.getText().toString()
            )


            if (Companion.isDefined(AL_OPERATIONAL_CONDITION_edit!!)) modifiers.put(
                ModifiersUnits.AL_OPERATIONAL_CONDITION,
                AL_OPERATIONAL_CONDITION_edit!!.getText().toString()
            )
            if (Companion.isDefined(AO_ENGAGEMENT_BAR_edit!!)) modifiers.put(
                ModifiersUnits.AO_ENGAGEMENT_BAR,
                AO_ENGAGEMENT_BAR_edit!!.getText().toString()
            )

            if (Companion.isDefined(SCC_SONAR_CLASSIFICATION_CONFIDENCE_edit!!)) modifiers.put(
                ModifiersUnits.SCC_SONAR_CLASSIFICATION_CONFIDENCE,
                SCC_SONAR_CLASSIFICATION_CONFIDENCE_edit!!.getText().toString()
            )

            if (Companion.isDefined(CN_CPOF_NAME_LABEL_edit!!)) modifiers.put(
                ModifiersUnits.CN_CPOF_NAME_LABEL,
                CN_CPOF_NAME_LABEL_edit!!.getText().toString()
            )
        }

        //apply modifier
        picker!!.dismiss()
    }

    private fun applyVisibility(baseCode: String, view: View) {
        if (baseCode.get(0) != 'W') {
            view.findViewById<View>(R.id.COUNTRY_CODE).setVisibility(View.VISIBLE)
            view.findViewById<View>(R.id.milstdspinner1).setVisibility(View.VISIBLE)
            view.findViewById<View>(R.id.milstdspinner2).setVisibility(View.VISIBLE)
        }
        if (baseCode.get(0) == 'G' || baseCode.get(0) == 'W') {
            //SymbolDef symbolDef = SymbolDefTable.getInstance().getSymbolDef(baseCode, RendererSettings.getInstance().getSymbologyStandard());
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.AM_DISTANCE)) {
                view.findViewById<View>(R.id.AM_DISTANCE).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.AN_AZIMUTH, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.AN_AZIMUTH).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.ANGLE, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.ANGLE).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.C_QUANTITY, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.C_QUANTITY).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.H_ADDITIONAL_INFO_1,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.H_ADDITIONAL_INFO_1).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.H1_ADDITIONAL_INFO_2,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.H1_ADDITIONAL_INFO_2).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.H2_ADDITIONAL_INFO_3,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.H2_ADDITIONAL_INFO_3).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.LENGTH, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.LENGTH).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.N_HOSTILE, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.N_HOSTILE).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.Q_DIRECTION_OF_MOVEMENT,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.Q_DIRECTION_OF_MOVEMENT).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.RADIUS, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.RADIUS).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.S_OFFSET_INDICATOR,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.S_OFFSET_INDICATOR).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.V_EQUIP_TYPE, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.V_EQUIP_TYPE).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.W1_DTG_2, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.W1_DTG_2).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(baseCode, ModifiersTG.W_DTG_1, RendererSettings.getInstance().getSymbologyStandard())) {
                view.findViewById<View>(R.id.W_DTG_1).setVisibility(View.VISIBLE)
            }


            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.T1_UNIQUE_DESIGNATION_2,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.T1_UNIQUE_DESIGNATION_2).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.T_UNIQUE_DESIGNATION_1,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.T_UNIQUE_DESIGNATION_1).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canSymbolHaveModifier(
                    baseCode,
                    ModifiersTG.X_ALTITUDE_DEPTH,
                    RendererSettings.getInstance().getSymbologyStandard()
                )
            ) {
                view.findViewById<View>(R.id.X_ALTITUDE_DEPTH).setVisibility(View.VISIBLE)
            }
        } else {
            //UnitDef def = UnitDefTable.getInstance().getUnitDef(baseCode, RendererSettings.getInstance().getSymbologyStandard());

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.C_QUANTITY)) {
                view.findViewById<View>(R.id.C_QUANTITY).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.D_TASK_FORCE_INDICATOR)) {
                view.findViewById<View>(R.id.D_TASK_FORCE_INDICATOR).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.E_FRAME_SHAPE_MODIFIER)) {
                view.findViewById<View>(R.id.E_FRAME_SHAPE_MODIFIER).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.F_REINFORCED_REDUCED)) {
                view.findViewById<View>(R.id.F_REINFORCED_REDUCED).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.G_STAFF_COMMENTS)) {
                view.findViewById<View>(R.id.G_STAFF_COMMENTS).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.H_ADDITIONAL_INFO_1)) {
                view.findViewById<View>(R.id.H_ADDITIONAL_INFO_1).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.H1_ADDITIONAL_INFO_2)) {
                view.findViewById<View>(R.id.H1_ADDITIONAL_INFO_2).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.H2_ADDITIONAL_INFO_3)) {
                view.findViewById<View>(R.id.H2_ADDITIONAL_INFO_3).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.J_EVALUATION_RATING)) {
                view.findViewById<View>(R.id.J_EVALUATION_RATING).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.K_COMBAT_EFFECTIVENESS)) {
                view.findViewById<View>(R.id.K_COMBAT_EFFECTIVENESS).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.L_SIGNATURE_EQUIP)) {
                view.findViewById<View>(R.id.L_SIGNATURE_EQUIP).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.M_HIGHER_FORMATION)) {
                view.findViewById<View>(R.id.M_HIGHER_FORMATION).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.N_HOSTILE)) {
                view.findViewById<View>(R.id.N_HOSTILE).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.P_IFF_SIF)) {
                view.findViewById<View>(R.id.P_IFF_SIF).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.Q_DIRECTION_OF_MOVEMENT)) {
                view.findViewById<View>(R.id.Q_DIRECTION_OF_MOVEMENT).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.R2_SIGNIT_MOBILITY_INDICATOR)) {
                view.findViewById<View>(R.id.R2_SIGNIT_MOBILITY_INDICATOR).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.T1_UNIQUE_DESIGNATION_2)) {
                view.findViewById<View>(R.id.T1_UNIQUE_DESIGNATION_2).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.T_UNIQUE_DESIGNATION_1)) {
                view.findViewById<View>(R.id.T_UNIQUE_DESIGNATION_1).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.V_EQUIP_TYPE)) {
                view.findViewById<View>(R.id.V_EQUIP_TYPE).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.X_ALTITUDE_DEPTH)) {
                view.findViewById<View>(R.id.X_ALTITUDE_DEPTH).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.Z_SPEED)) {
                view.findViewById<View>(R.id.Z_SPEED).setVisibility(View.VISIBLE)
            }
            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AA_SPECIAL_C2_HQ)) {
                view.findViewById<View>(R.id.AA_SPECIAL_C2_HQ).setVisibility(View.VISIBLE)
            }


            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AB_FEINT_DUMMY_INDICATOR)) {
                view.findViewById<View>(R.id.AB_FEINT_DUMMY_INDICATOR).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AC_INSTALLATION)) {
                view.findViewById<View>(R.id.AC_INSTALLATION).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AD_PLATFORM_TYPE)) {
                view.findViewById<View>(R.id.AD_PLATFORM_TYPE).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AE_EQUIPMENT_TEARDOWN_TIME)) {
                view.findViewById<View>(R.id.AE_EQUIPMENT_TEARDOWN_TIME).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AF_COMMON_IDENTIFIER)) {
                view.findViewById<View>(R.id.AF_COMMON_IDENTIFIER).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AG_AUX_EQUIP_INDICATOR)) {
                view.findViewById<View>(R.id.AG_AUX_EQUIP_INDICATOR).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AH_AREA_OF_UNCERTAINTY)) {
                view.findViewById<View>(R.id.AH_AREA_OF_UNCERTAINTY).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AI_DEAD_RECKONING_TRAILER)) {
                view.findViewById<View>(R.id.AI_DEAD_RECKONING_TRAILER).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AJ_SPEED_LEADER)) {
                view.findViewById<View>(R.id.AJ_SPEED_LEADER).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AK_PAIRING_LINE)) {
                view.findViewById<View>(R.id.AK_PAIRING_LINE).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AL_OPERATIONAL_CONDITION)) {
                view.findViewById<View>(R.id.AL_OPERATIONAL_CONDITION).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.AO_ENGAGEMENT_BAR)) {
                view.findViewById<View>(R.id.AO_ENGAGEMENT_BAR).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.SCC_SONAR_CLASSIFICATION_CONFIDENCE)) {
                view.findViewById<View>(R.id.SCC_SONAR_CLASSIFICATION_CONFIDENCE).setVisibility(View.VISIBLE)
            }

            if (SymbolUtilities.canUnitHaveModifier(baseCode, ModifiersUnits.CN_CPOF_NAME_LABEL)) {
                view.findViewById<View>(R.id.CN_CPOF_NAME_LABEL).setVisibility(View.VISIBLE)
            }
        }
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.milstd_search_cancel -> picker!!.dismiss()
            R.id.milstd_search_affil_f -> charAffiliation = "F"
            R.id.milstd_search_affil_h -> charAffiliation = "H"
            R.id.milstd_search_affil_n -> charAffiliation = "N"

            R.id.milstd_search_affil_u -> charAffiliation = "U"

        }
    }


    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
    }

    companion object {
        fun isDefined(e: EditText): Boolean {
            val content = e.getText().toString()
            if (content == null || content.length == 0) return false
            return true
        }
    }
}
