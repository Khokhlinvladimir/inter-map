package org.osmdroid.views.drawing;

import android.os.Handler;
import android.os.Message;

import org.osmdroid.tileprovider.MapTileProviderBase;

public class MapSnapshotHandler extends Handler {

    private MapSnapshot mMapSnapshot;

    public MapSnapshotHandler(final MapSnapshot pMapSnapshot) {
        super();
        mMapSnapshot = pMapSnapshot;
    }

    @Override
    public void handleMessage(final Message msg) {
        if (msg.what == MapTileProviderBase.MAPTILE_SUCCESS_ID) {
            final MapSnapshot mapSnapshot = mMapSnapshot;
            if (mapSnapshot != null) { // in case it was destroyed just before
                mapSnapshot.refreshASAP();
            }
        }
    }

    public void destroy() {
        mMapSnapshot = null;
    }
}
