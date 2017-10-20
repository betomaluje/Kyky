package com.betomaluje.android.kyky;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by betomaluje on 4/16/16.
 */
public abstract class KykyActivity extends Activity {

    protected Kyky kyky;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kyky = new Kyky(KykyActivity.this, getPath());
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
