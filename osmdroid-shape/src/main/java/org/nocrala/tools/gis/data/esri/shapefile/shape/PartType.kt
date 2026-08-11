package org.nocrala.tools.gis.data.esri.shapefile.shape

enum class PartType(
    /**
     * Returns the part type's numeric ID, as defined by the ESRI specification.
     *
     * @return
     */
    val id: Int
) {
    TRIANGLE_STRIP(0),  //
    TRIANGLE_FAN(1),  //
    OUTER_RING(2),  //
    INNER_RING(3),  //
    FIRST_RING(4),  //
    RING(5); //

    // Getters

    companion object {
        // parse
        @JvmStatic
        fun parse(tid: Int): PartType? {
            for (st in values()) {
                if (st.id == tid) {
                    return st
                }
            }
            return null
        }
    }
}
