package com.example.a6100890.gleaner;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.a6100890.gleaner.imageRecognition.ImageBody;
import com.example.a6100890.gleaner.imageRecognition.Tags;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import Decoder.BASE64Encoder;


/**
 * Created by a6100890 on 2017/10/15.
 */

public class HomeFragment extends Fragment implements View.OnClickListener {
    public static final int TAKE_PHOTO = 1;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "HomeFragment";


    private Button mBtnCameraRelease;
    private Uri mImageUri;
    private String returnJson;
    private List<Tags> mImageTags = new ArrayList<>();

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
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camara_release:
                selectPicFromCamera();
                break;

            default:
                break;
        }

    }

    private void selectPicFromCamera() {
        File outputImage = new File(getActivity().getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //将File转换为Uri
        if (Build.VERSION.SDK_INT >= 24) {
            mImageUri = FileProvider.getUriForFile(getActivity(), "com.example.a6100890.gleaner.fileprovider", outputImage);
        } else {
            mImageUri = Uri.fromFile(outputImage);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == getActivity().RESULT_OK) {

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
                    Log.d(TAG, "recognitionImage: " + result);
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
//        PostJson postJson = new PostJson(imageUrl);
//        String json = new Gson().toJson(postJson);
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
            Collections.sort(mImageTags, new Comparator<Tags>() {
                @Override
                public int compare(Tags t1, Tags t2) {
                    return t1.getConfidence() - t2.getConfidence();
                }
            });

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

//    public static String GetImageStrFromPath(String )

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
            statusCode = ((HttpURLConnection)conn).getResponseCode();
            if(statusCode != 200) {
                in = new BufferedReader(new InputStreamReader(((HttpURLConnection)conn).getErrorStream()));
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
            throw new IOException("\nHttp StatusCode: "+ statusCode + "\nErrorMessage: " + result);
        }
        return result;
    }

}
