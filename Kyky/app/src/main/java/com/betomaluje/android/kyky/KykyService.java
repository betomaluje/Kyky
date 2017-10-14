package com.betomaluje.android.kyky;

import android.support.wearable.watchface.CanvasWatchFaceService;

/**
 * Created by betomaluje on 4/16/16.
 */
public abstract class KykyService extends CanvasWatchFaceService {

    protected Kyky kyky;

    @Override
    public void onCreate() {
        super.onCreate();

        kyky = new Kyky(KykyService.this, getPath());
    }

    public class KykyEngine extends CanvasWatchFaceService.Engine {

        @Override
        public void onDestroy() {
            kyky.disconnect();
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                kyky.connect();
            } else {
                kyky.disconnect();
            }
        }
    }

    public abstract String getPath();
}