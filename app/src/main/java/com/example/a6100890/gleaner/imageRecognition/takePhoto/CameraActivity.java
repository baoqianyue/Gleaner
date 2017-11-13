package com.example.a6100890.gleaner.imageRecognition.takePhoto;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.a6100890.gleaner.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout mCameraLayout;
    private Button mTakePictureBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //启动相机
        if (!checkCameraHadrware(this)) {
            Toast.makeText(this, "不支持相机", Toast.LENGTH_SHORT).show();
        } else {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            mCameraLayout = (FrameLayout) findViewById(R.id.camera_preview);
            mCameraLayout.addView(mPreview);
        }

        mTakePictureBtn = (Button) findViewById(R.id.button_capture);
        mTakePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                        File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        File pictureFile;

                        if (pictureDir == null) {
                            Log.d(TAG, "onPictureTaken: Error creating media file, check storage permissions");
                            return;
                        }

                        try {
                            String pictureName = new DateFormat().format("yyyyMMddHHmmss", new Date()).toString() + ".png";
                            pictureFile = new File(pictureDir + File.separator + pictureName);

                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(bytes);
                            fos.close();

                            Log.d(TAG, "onPictureTaken: filelength: " + pictureFile.length() + "  " + pictureFile.getPath());

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }



    /**
     * 判断相机是否支持
     */
    private boolean checkCameraHadrware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }



}
