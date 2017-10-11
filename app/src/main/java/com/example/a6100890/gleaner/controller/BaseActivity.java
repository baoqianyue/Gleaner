package com.example.a6100890.gleaner.controller;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.a6100890.gleaner.R;

import cn.bmob.v3.Bmob;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        //Bmob应用ID: e4623d0c00213bb18860e466c83b5791
//        Bmob.initialize(this,"e4623d0c00213bb18860e466c83b5791");
    }
}
