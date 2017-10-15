package com.example.a6100890.gleaner;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * Created by a6100890 on 2017/10/15.
 */

public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
    }

    public static MessageFragment newInsatnce() {
        return new MessageFragment();
    }
}
