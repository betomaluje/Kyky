package com.betomaluje.android.kyky;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

/**
 * Created by betomaluje on 10/12/17.
 */

public abstract class KykyWearableActivity extends WearableActivity {

    protected Kyky kyky;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kyky = new Kyky(KykyWearableActivity.this, getPath());
    }

    @Override
    protected void onStart() {
        super.onStart();
        kyky.connect();
    }

    @Override
    protected void onStop() {
        kyky.disconnect();
        super.onStop();
    }

    public abstract String getPath();

}
