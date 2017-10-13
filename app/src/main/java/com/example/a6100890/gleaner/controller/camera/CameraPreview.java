package com.example.a6100890.gleaner.controller.camera;

/**
 * Created by Administrator on 2017/10/13.
 */

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;



public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context c, Camera camera) {
        super(c);
        mCamera = camera;
        mHolder = getHolder();

        //相当于是一个listener
        mHolder.addCallback(this);

        //SURFACE_TYPE_PUSH_BUFFER表明该Surface用到的数据由其他对象提供
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try{
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mHolder.getSurface() == null){
            return ;
        }
        try{
            mCamera.stopPreview();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}

