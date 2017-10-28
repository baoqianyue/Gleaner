package com.example.a6100890.gleaner.communication;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Window;

import com.example.a6100890.gleaner.R;
import com.hyphenate.easeui.ui.EaseChatFragment;

/**
 * 聊天界面
 */
public class ChatActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_chat);

        EaseChatFragment easeChatFragment = new EaseChatFragment();
        easeChatFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_chat, easeChatFragment)
                .commit();
    }
}
