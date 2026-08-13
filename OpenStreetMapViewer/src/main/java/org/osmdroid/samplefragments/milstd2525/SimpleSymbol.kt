package org.osmdroid.samplefragments.milstd2525

import android.util.SparseArray
import armyc2.c2sd.renderer.utilities.SymbolDef
import armyc2.c2sd.renderer.utilities.UnitDef

/**
 * This class was created as a work around for
 * [this](https://github.com/missioncommand/mil-sym-android/issues/82).
 * Basically, we want to fill a list adapter with all available symbols. The renderer
 * unfortunately uses different but similar data structures. This class merges the
 * relevant parts of the two symbol defs
 * created on 1/16/2018.
 *
 * @author Alex O'Ree
 * @see SymbolDef
 *
 * @see UnitDef
 *
 * @since 6.0.0
 */
class SimpleSymbol {
    enum class Echelon2(val value: Char) {
        Null('-'),
        Team_Crew('A'),
        Squad('B'),
        Section('C'),
        Platoon_Detachment('D'),
        Company_Battery_Troop('E'),
        Battalion_Squadron('F'),
        Regiment_Group('G'),
        Bridage('H'),
        Divison('I'),
        Corps('J'),
        Army('K'),
        Army_Group_Front('L'),
        Region('M'),
        Command('N'),
        Wheeled('O'),
        Cross_Country('P'),
        Tracked('Q'),
        Wheeled_and_tracked('R'),
        Towed('S'),
        Rail('T'),
        Over_Snow('U'),
        Sled('V'),
        Pack_Animals('W'),
        Barge('X'),
        Amphibious('Y'),
    }

    enum class Echelon1(val value: Char) {
        Null('-'),
        Headquarters('A'),
        TaskForce_HQ('B'),
        Feint_Dummy_Hq('C'),
        Feint_Dummy_TaskForce_Hq('D'),
        Task_Force('E'),
        Feint_Dummy('F'),
        Feint_Dummy_TaskForce('G'),
        Installation('H'),
        Mobility('M'),
        Towed('N'),
    }

    enum class OrderOfBattle(val value: Char) {
        Null('-'),
        Air('A'),
        Electronic('B'),
        Civilian('C'),
        Ground('D'),
        Maritime('N'),
        Strategic_Force('S'),
        Control_Markings('X'),
    }

    var orderOfBattle: OrderOfBattle? = OrderOfBattle.Null
    var countryCode: String? = "--"
    var echelon2: Echelon2? = Echelon2.Null
    var echelon1: Echelon1? = Echelon1.Null

    @JvmField
    var modifiers: SparseArray<String?>? = SparseArray<String?>()
    var minPoints: Int = 1
    var maxPoints: Int = 1
    @JvmField
    var basicSymbolId: String? = ""
    var description: String? = ""
    var hierarchy: String? = ""
    private var canDraw = true
    var path: String? = ""
    @JvmField
    var symbolCode: String? = ""

    fun canDraw(): Boolean {
        return canDraw
    }

    companion object {
        fun createFrom(def: UnitDef): SimpleSymbol {
            val s = SimpleSymbol()
            s.basicSymbolId = def.getBasicSymbolId()
            s.description = def.getDescription()
            s.hierarchy = def.getHierarchy()
            s.path = def.getFullPath()
            s.canDraw = def.getDrawCategory() == UnitDef.DRAW_CATEGORY_POINT
            return s
        }

        fun createFrom(def: SymbolDef): SimpleSymbol {
            val s = SimpleSymbol()
            s.basicSymbolId = def.getBasicSymbolId()
            s.description = def.getDescription()
            s.hierarchy = def.getHierarchy()
            s.path = def.getFullPath()
            s.maxPoints = def.getMaxPoints()
            s.minPoints = def.getMinPoints()
            s.canDraw = def.getDrawCategory() != SymbolDef.DRAW_CATEGORY_DONOTDRAW
            return s
        }
    }
}
