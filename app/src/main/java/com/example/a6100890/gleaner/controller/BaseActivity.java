package com.example.a6100890.gleaner.controller;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.a6100890.gleaner.R;
import com.example.a6100890.gleaner.controller.camera.CameraActivity;

import cn.bmob.v3.Bmob;

public class BaseActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 0;
    private Button mTestCameraButton;
    private TextView mTestResultsTextView;
    private final int REQUESTCODE = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String result = data.getStringExtra("result");
        switch (requestCode) {
            case 0:
                mTestResultsTextView.setText(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        mTestCameraButton = (Button) findViewById(R.id.camera_classifier_button);
        mTestResultsTextView = (TextView) findViewById(R.id.result_test_text_view);
        mTestCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(BaseActivity.this, CameraActivity.class);

                if (ActivityCompat.checkSelfPermission(BaseActivity.this, android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BaseActivity.this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
                } else {
                    startActivityForResult(i, REQUESTCODE);
                }
            }
        });

        //Bmob应用ID: e4623d0c00213bb18860e466c83b5791
//        Bmob.initialize(this,"e4623d0c00213bb18860e466c83b5791");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent i = new Intent(BaseActivity.this, CameraActivity.class);
                startActivityForResult(i, REQUESTCODE);
            }
        }
    }
}
