package com.example.a6100890.gleaner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.a6100890.gleaner.imageRecognition.ImageBody;
import com.example.a6100890.gleaner.imageRecognition.Tags;
import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import Decoder.BASE64Encoder;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UploadFileListener;


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
    private ImageView mImageView;

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
        mImageView = view.findViewById(R.id.picture);
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camara_release:
                openCamera();
                break;
            default:
                break;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == getActivity().RESULT_OK) {
                    try {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mTextView.setText("正在识别...");

                        Bitmap bitmap = BitmapFactory.decodeStream(getContext().getContentResolver().openInputStream(mImageUri));
                        mImageView.setImageBitmap(bitmap);
                        mOutputImage = Bitmap2File(bitmap, mOutputImage.getPath());
                        Log.d(TAG, "onActivityResult: " + mOutputImage.length() + "  " + mOutputImage.getPath());
                        recognizeImageFile(mOutputImage, bitmap);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "onActivityResult: result != OK");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 自己构建的返回File形式的拍照,暂时弃用
     */
    private void openCamera2() {
        Intent getPhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        strImgPath = Environment.getExternalStorageDirectory().toString() + "/image/";
        String fileName = "output_image.jpg";
        File out = new File(strImgPath);

        if (!out.exists()) {
            out.mkdirs();
        }

        out = new File(strImgPath, fileName);
        strImgPath = strImgPath + fileName;

        mImageUri = Uri.fromFile(out);
        getPhoto.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        getPhoto.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(getPhoto, TAKE_PHOTO);
    }

    /**
     * Uri形式
     */
    private void openCamera() {
        if (mOutputImage == null) {
            mOutputImage = new File(getActivity().getExternalCacheDir(), "output_image.jpg");
        }
        try {
            if (mOutputImage.exists()) {
                mOutputImage.delete();
            }
            mOutputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= 24) {
            mImageUri = FileProvider.getUriForFile(getActivity(), "com.example.a6100890.gleaner.fileprovider", mOutputImage);
        } else {
            mImageUri = Uri.fromFile(mOutputImage);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    /**
     * 识别所给的图像url
     *
     * @param imageUrl 通识物品的网络url
     * @param bitmap   校园卡等特殊物品的bitmap
     */
    private void recognitizeImage(final String imageUrl, final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String postUrl = "https://dtplus-cn-shanghai.data.aliyuncs.com/image/tag";
                String body = constructJson(imageUrl);
                try {
                    final String resultjson = sendPost(postUrl, body);
                    Log.d(TAG, "recognitizeImage: Thread: " + Thread.currentThread() + resultjson);
                    final String resultName = parseImageJson(resultjson);

                    //更新UI
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日  HH:mm");
                            String curTime = format.format(new Date(System.currentTimeMillis()));

                            mProgressBar.setVisibility(View.GONE);
                            mImageView.setImageBitmap(bitmap);
                            mTextView.setText("识别结果为: " + resultName + "\n");
                            mTextView.append("当前时间为: " + curTime + "\n");
                            mTextView.append("地点为: "+ "中北大学主楼");

                            if (resultName.contains("名片")) {
                                mTextView.append("检测到校园卡,正在识别卡片信息...");
                            }

                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    /**
     * 把图像的url构造为jsonbody形式
     */
    private String constructJson(String imageUrl) {
        String json = "{\"type\":0,\"image_url\":" + "\"" + imageUrl + "\"" + "}";
        Log.d(TAG, "constructJson: " + json);
        return json;
    }


    /**
     * 结果保存在mImageTags中
     * 代码需要重构一下,降低耦合性
     *
     * @param json
     */
    private String parseImageJson(String json) {
        Gson gson = new Gson();
        String result = "";
        if (json != null) {
            mImageTags = gson.fromJson(json, ImageBody.class).getTags();

            if (mImageTags.size() == 0) {
                Log.d(TAG, "parseImageJson: something wrong: imageTags .size == 0");
            } else {
                for (Tags tag : mImageTags) {
                    Log.d(TAG, "parseImageJson: " + tag.getValue() + " " + tag.getConfidence());
                }
                result = mImageTags.get(0).getValue();
            }
        } else {
            Log.d(TAG, "parseImageJson: json is null!");
        }

        return result;
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

    /**
     * 压缩Bitmap并转换为File
     */
    public static File Bitmap2File(Bitmap bitmap, String filePath) {
        File file = new File(filePath);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            //压缩比例30
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    public static String Bitmap2String(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, baos);
        byte[] image = baos.toByteArray();
        return Base64.encodeToString(image, Base64.DEFAULT);
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

    /**
     * 把File文件转换为网络url,然后识别
     *
     * @param bitmap 传入reconizeImage使用,用于检测到校园卡等物品的后续识别
     * @param file   通识物品的File格式图片
     * @return 识别结果String json
     */
    private String recognizeImageFile(File file, final Bitmap bitmap) {
        final BmobFile bmobFile = new BmobFile(file);
        final String[] imageUrl = new String[1];

        bmobFile.uploadblock(new UploadFileListener() {
            @Override
            public void done(BmobException e) {
                mProgressBar.setVisibility(View.GONE);
                if (e == null) {
                    imageUrl[0] = bmobFile.getFileUrl();
                    recognitizeImage(imageUrl[0], bitmap);

                } else {
                    Log.d(TAG, "uploadFile failed: " + e.getMessage());
                    imageUrl[0] = "";
                }
            }

            @Override
            public void onProgress(Integer value) {
                super.onProgress(value);
            }
        });

        return imageUrl[0];
    }

    private void recognizeBusinessCard(Bitmap bitmap) {
        final String url = "http(s)://dm-57.data.aliyun.com/rest/160601/ocr/ocr_business_card.json";
        String base64Image = Bitmap2String(bitmap);
        final String jsonBody = "{\n" + "  \"inputs\": [\n" +
                "    {\n" +
                "      \"image\": {\n" +
                "        \"dataType\": 50,\n" +
                "        \"dataValue\":" + base64Image +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String resultJson = sendPost(url, jsonBody);
                    Log.d(TAG, "recognize 名片: " + resultJson);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).run();


    }


}
