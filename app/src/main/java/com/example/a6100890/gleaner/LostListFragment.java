package com.example.a6100890.gleaner;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.hyphenate.chat.EMClient;

/**
 * Created by a6100890 on 2017/10/15.
 */

public class LostListFragment extends Fragment {
    private Button mButtonAddContact;
    private ProgressDialog mProgressDialog;

    private static final String TAG = "LostListFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lost_list, container, false);

        mButtonAddContact = view.findViewById(R.id.add_contact);
        mButtonAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addContact("xiaobing");
            }
        });
        return view;
    }

    public static LostListFragment newInstance() {
        return new LostListFragment();
    }

    public void addContact(final String username) {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("正在发送请求...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().contactManager().addContact(username, "好友请求");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            Log.d(TAG, "run: 模拟添加好友成功");
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            Log.d(TAG, "run: 模拟添加好友失败");
                        }
                    });

                }
            }
        }).start();
    }




}
