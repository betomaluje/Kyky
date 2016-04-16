package com.betomaluje.android.kyky;

import android.content.Context;
import android.support.v4.app.Fragment;

/**
 * Created by betomaluje on 4/16/16.
 */
public abstract class KykyFragment extends Fragment {

    protected Kyky kyky;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        kyky = new Kyky(context, getPath());
    }

    @Override
    public void onPause() {
        super.onPause();
        kyky.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        kyky.connect();
    }

    public abstract String getPath();

}
