package org.nocrala.tools.gis.data.esri.shapefile.exception

class InvalidShapeFileException : Exception {
    constructor() : super()

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)

    companion object {
        private const val serialVersionUID = 9052794347808071370L
    }
}
