package org.osmdroid.config

/**
 * Singleton class to get/set a configuration provider for osmdroid
 * [Issue 481](https://github.com/osmdroid/osmdroid/issues/481)
 * Created on 11/29/2016.
 *
 * @author Alex O'Ree
 * @see org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
 *
 * @since 5.6
 */
object Configuration {
    private var ref: IConfigurationProvider? = null

    /**
     * gets the current reference to the config provider.
     * if one hasn't been set yet, the default provider and default configuration will be used
     *
     * @return
     */
    @JvmStatic
    @get:Synchronized // TODO проверить не нужно ли синхронизировать сеттер?
    val instance: IConfigurationProvider?
        get() {
            if (ref == null) ref = DefaultConfigurationProvider()
            return ref
        }

    /**
     * Note, this should be called before any instances of MapView are created (either programmatically
     * or via android's inflater
     *
     * @param instance
     * @see android.view.LayoutInflater
     */
    @JvmStatic
    fun setConfigurationProvider(instance: IConfigurationProvider?) {
        ref = instance
    }
}