package com.example.a6100890.gleaner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText mEditUsername;
    private EditText mEditPassword;
    private Button mButtonLogin;
    private Boolean isLogin = false;
    String username = "xiaobing";
    String password = "123456";

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        initView();
    }

    private void initView() {
        mEditUsername = (EditText) findViewById(R.id.edit_username);
        mEditPassword = (EditText) findViewById(R.id.edit_password);
        mButtonLogin = (Button) findViewById(R.id.btn_login);

        mButtonLogin.setOnClickListener(this);
    }


    private void register() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().createAccount(username, password);
                    login();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                    Log.e(TAG, "register failed ", e);
                }
            }
        }).start();
    }

    private void login() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EMClient.getInstance().login(username, password, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "onSuccess: login succeed");
                        isLogin = true;

                        Intent intent = new Intent(LoginActivity.this, BaseActivity.class);
                        intent.putExtra("isLogin", isLogin);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(int i, String s) {
                        Log.d(TAG, "onError: login failed :" + s);
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                if (mEditUsername.getText().toString().equals("") || mEditPassword.getText().toString().equals("")) {
                    Toast.makeText(this, "用户名或密码不能为空!", Toast.LENGTH_SHORT).show();
                } else {
                    username = mEditUsername.getText().toString();
                    password = mEditPassword.getText().toString();
                    login();

                }
                break;

            default:
                break;
        }
    }
}
