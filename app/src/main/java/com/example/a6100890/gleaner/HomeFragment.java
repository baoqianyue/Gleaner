package com.example.a6100890.gleaner;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.a6100890.gleaner.imageRecognition.ImageBody;
import com.example.a6100890.gleaner.imageRecognition.Tags;
import com.example.a6100890.gleaner.imageRecognition.takePhoto.CameraActivity;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import Decoder.BASE64Encoder;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UploadFileListener;
import okhttp3.OkHttpClient;


/**
 * Created by a6100890 on 2017/10/15.
 */

public class HomeFragment extends Fragment implements View.OnClickListener {
    public static final int TAKE_PHOTO = 1;
    private static final String TAG = "HomeFragment";

    private Button mBtnCameraRelease;
    private Uri mImageUri;
    private List<Tags> mImageTags = new ArrayList<>();
    private File mOutputImage;
    private String strImgPath;
    private ProgressBar mProgressBar;
    private TextView mTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mBtnCameraRelease = view.findViewById(R.id.btn_camara_release);
        mBtnCameraRelease.setOnClickListener(this);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mTextView = view.findViewById(R.id.text_view);
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camara_release:
                String filePath = "/storage/emulated/0/1/sina/nike.jpg";
//                openCamera();

                Intent intent = new Intent(getActivity(), CameraActivity.class);
                startActivity(intent);
                break;

            default:
                break;
        }

    }

    private void openCamera() {
        Intent getPhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        strImgPath = Environment.getExternalStorageDirectory().toString() + "/image/";
        String fileName = "output_image.jpg";
        File out = new File(strImgPath);

        if (!out.exists()) {
            out.mkdirs();
        }

        out = new File(strImgPath, fileName);
        strImgPath = strImgPath + fileName;

        Uri uri = Uri.fromFile(out);
        getPhoto.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        getPhoto.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(getPhoto, TAKE_PHOTO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == getActivity().RESULT_OK) {
                    mOutputImage = new File(strImgPath);
                    recognizeImageFile(mOutputImage);

                } else {
                    Log.d(TAG, "onActivityResult: result != OK");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 一定要开启一个线程来执行网络协议
     */
    private void recognitionImage(final String imageUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String postUrl = "https://dtplus-cn-shanghai.data.aliyuncs.com/image/tag";
                String body = constructJson(imageUrl);
                try {
                    String result = sendPost(postUrl, body);
                    Log.d(TAG, "recognitionImage: Thread: " + Thread.currentThread() + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * 在sendRequest中调用
     *
     * @param imageUrl
     * @return
     */
    private String constructJson(String imageUrl) {
        String json = "{\"type\":0,\"image_url\":" + "\"" + imageUrl + "\"" + "}";
        Log.d(TAG, "constructJson: " + json);
        return json;
    }


    /**
     * 结果保存在mImageTags中
     *
     * @param json
     */
    private void paresJson(String json) {
        Gson gson = new Gson();
        if (json != null) {
            mImageTags = gson.fromJson(json, ImageBody.class).getTags();


            if (mImageTags.size() == 0) {
                Log.d(TAG, "paresJson: something wrong: imageTags .size == 0");
            } else {
                for (Tags tag : mImageTags) {
                    Log.d(TAG, "paresJson: " + tag.getValue() + " " + tag.getConfidence());
                }
            }
        } else {
            Log.d(TAG, "paresJson: json is null!");
        }
    }

    public static String toGMTString(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);
        df.setTimeZone(new java.util.SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    public static String MD5Base64(String s) {
        if (s == null)
            return null;
        String encodeStr = "";
        byte[] utfBytes = s.getBytes();
        MessageDigest mdTemp;
        try {
            mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(utfBytes);
            byte[] md5Bytes = mdTemp.digest();
            BASE64Encoder b64Encoder = new BASE64Encoder();
            encodeStr = b64Encoder.encode(md5Bytes);
        } catch (Exception e) {
            throw new Error("Failed to generate MD5 : " + e.getMessage());
        }
        return encodeStr;
    }

    public static String HMACSha1(String data, String key) {
        String result;
        try {

            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            result = (new BASE64Encoder()).encode(rawHmac);
        } catch (Exception e) {
            throw new Error("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
    }

    public static String File2String(File imageFile) {
        InputStream in = null;
        byte[] data = null;

        //读取图片字节数组
        try {
            in = new FileInputStream(imageFile);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BASE64Encoder encoder = new BASE64Encoder();
        String result = encoder.encode(data);
        return result;
    }

    /*
     * 发送POST请求
     */
    public static String sendPost(String url, String body) throws Exception {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        int statusCode = 200;
        try {
            URL realUrl = new URL(url);

            /*
             * http header 参数
             */

            String ak_id = "LTAI4CrS1MlxoaAL";
            String ak_secret = "uZ7xb6YE3kn9QKzBmXPUURklgKv0Ax";
            String method = "POST";
            String accept = "application/json";
            String content_type = "application/json";
            String path = realUrl.getFile();
            String date = toGMTString(new Date());

            // 1.对body做MD5+BASE64加密
            String bodyMd5 = MD5Base64(body);
            String stringToSign = method + "\n" + accept + "\n" + bodyMd5 + "\n" + content_type + "\n" + date + "\n"
                    + path;
            // 2.计算 HMAC-SHA1
            String signature = HMACSha1(stringToSign, ak_secret);
            // 3.得到 authorization header
            String authHeader = "Dataplus " + ak_id + ":" + signature;

            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", accept);
            conn.setRequestProperty("content-type", content_type);
            conn.setRequestProperty("date", date);
            conn.setRequestProperty("Authorization", authHeader);

            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(body);
            // flush输出流的缓冲
            out.flush();

            // 定义BufferedReader输入流来读取URL的响应
            statusCode = ((HttpURLConnection) conn).getResponseCode();
            if (statusCode != 200) {
                in = new BufferedReader(new InputStreamReader(((HttpURLConnection) conn).getErrorStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (statusCode != 200) {
            throw new IOException("\nHttp StatusCode: " + statusCode + "\nErrorMessage: " + result);
        }
        return result;
    }

    private String recognizeImageFile(File file) {
        final BmobFile bmobFile = new BmobFile(file);
        final String[] imageUrl = new String[1];

        bmobFile.uploadblock(new UploadFileListener() {
            @Override
            public void done(BmobException e) {
                mProgressBar.setVisibility(View.GONE);
                if (e == null) {
                    imageUrl[0] = bmobFile.getFileUrl();
                    recognitionImage(imageUrl[0]);

                } else {
                    Log.d(TAG, "uploadFile failed: " + e.getMessage());
                    imageUrl[0] = "";
                }
            }

            @Override
            public void onProgress(Integer value) {
                super.onProgress(value);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(value);
                mTextView.setText("正在识别...");
            }
        });

        return imageUrl[0];
    }


}
