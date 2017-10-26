package com.example.a6100890.gleaner.communication;

import android.content.Context;
import android.util.Log;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

/**
 * Created by a6100890 on 2017/10/11.
 */

/**
 * 单例模式,使用此类的方法时应该先set 用户名和密码
 */
public class EasemobUser {
    private static EasemobUser sEasemobUser;
    private Context mContext;

    private String mUsername;
    private String mPassword;

    private static final String TAG = "EasemobUser";

    public void setUsername(String username) {
        mUsername = username;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getUsername() {
        return mUsername;
    }

    private EasemobUser(Context context) {
        mContext = context.getApplicationContext();
    }

    public static EasemobUser getInstance(Context context) {
        if (sEasemobUser == null) {
            sEasemobUser = new EasemobUser(context);
        }
        return sEasemobUser;
    }


    //必须在子线程运行
    public void signUp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().createAccount(mUsername, mPassword);
                    Log.d(TAG, "signUp: " + mUsername + " " + mPassword);
                } catch (HyphenateException e) {
                    Log.d(TAG, "EasemobUser sign up failed! " + e.getMessage() + " code: " + e.getErrorCode() + " " + e.getDescription());
                    e.printStackTrace();
                }

                login();
            }
        });
    }

    public void login() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EMClient.getInstance().login(mUsername, mPassword, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        EMClient.getInstance().groupManager().loadAllGroups();
                        EMClient.getInstance().chatManager().loadAllConversations();
                    }

                    @Override
                    public void onError(int i, String s) {
                        Log.d(TAG, "onError: login_edit failed!" + s);
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
            }
        });
    }

    public void logout() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EMClient.getInstance().logout(true);

            }
        });
    }

}
