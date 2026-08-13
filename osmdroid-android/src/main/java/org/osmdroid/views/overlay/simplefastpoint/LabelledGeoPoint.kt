package org.osmdroid.views.overlay.simplefastpoint

import android.location.Location
import android.os.Parcel
import android.os.Parcelable.Creator
import org.osmdroid.util.GeoPoint

/**
 * A [GeoPoint] with a label.
 * Created by Miguel Porto on 12-11-2016.
 */
open class LabelledGeoPoint : GeoPoint {
    var label: String? = null

    constructor(aLatitude: Double, aLongitude: Double) : super(aLatitude, aLongitude)

    constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double) : super(aLatitude, aLongitude, aAltitude)

    constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double, aLabel: String?) : super(aLatitude, aLongitude, aAltitude) {
        this.label = aLabel
    }

    constructor(aLocation: Location) : super(aLocation)

    constructor(aGeopoint: GeoPoint) : super(aGeopoint)

    constructor(aLatitude: Double, aLongitude: Double, aLabel: String?) : super(aLatitude, aLongitude) {
        this.label = aLabel
    }

    constructor(aLabelledGeopoint: LabelledGeoPoint) : this(
        aLabelledGeopoint.latitude, aLabelledGeopoint.longitude,
        aLabelledGeopoint.altitude, aLabelledGeopoint.label
    )

    override fun clone(): LabelledGeoPoint {
        return LabelledGeoPoint(
            this.latitude, this.longitude, this.altitude,
            this.label
        )
    }

    // ===========================================================
    // Parcelable
    // ===========================================================
    private constructor(`in`: Parcel) : super(`in`.readDouble(), `in`.readDouble(), `in`.readDouble()) {
        this.label = `in`.readString()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeString(this.label)
    }

    companion object {
        @JvmField
        val CREATOR: Creator<LabelledGeoPoint?> = object : Creator<LabelledGeoPoint?> {
            override fun createFromParcel(`in`: Parcel): LabelledGeoPoint {
                return LabelledGeoPoint(`in`)
            }

            override fun newArray(size: Int): Array<LabelledGeoPoint?> {
                return arrayOfNulls<LabelledGeoPoint>(size)
            }
        }
    }
}
