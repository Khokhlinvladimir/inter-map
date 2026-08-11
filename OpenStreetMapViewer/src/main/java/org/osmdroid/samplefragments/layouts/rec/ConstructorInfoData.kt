package org.osmdroid.samplefragments.layouts.rec

/**
 * created on 1/13/2017.
 *
 * @author PalilloKun
 */
class ConstructorInfoData {
    fun obtainData(): ArrayList<Info> {
        val data = ArrayList<Info>()

        data.add(Info("1", "Map", "Hello!"))
        data.add(Info("2", "Graphic", "Im Graphic!"))
        data.add(Info("3", "Information", "Im Info!"))
        data.add(Info("4", "Graphic", "Im Graphic!"))
        data.add(Info("5", "Information", "Im Info!"))
        data.add(Info("6", "Graphic", "Im Graphic!"))
        data.add(Info("7", "Information", "Im Info!"))
        data.add(Info("8", "Map", "Hello!"))

        data.add(Info("9", "Information", "Im Info!"))
        data.add(Info("10", "Graphic", "Im Graphic!"))
        data.add(Info("11", "Information", "Im Info!"))
        data.add(Info("12", "Graphic", "Im Graphic!"))
        data.add(Info("13", "Information", "Im Info!"))

        return data
    }
}
