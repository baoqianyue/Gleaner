package com.example.a6100890.gleaner.controller.tensorflow;

/**
 * Created by Administrator on 2017/10/13.
 */


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "TensorFlowImageClassifier";


    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.1f;


    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;


    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    private TensorFlowImageClassifier() {
    }


    public static Classifier create(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName)
            throws IOException {
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;

        String actualFilename = labelFilename.split("file:///android_asset/")[1];

        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
        String line;
        while ((line = br.readLine()) != null) {
            c.labels.add(line);
        }
        br.close();

        c.inferenceInterface = new TensorFlowInferenceInterface();
        if (c.inferenceInterface.initializeTensorFlow(assetManager, modelFilename) != 0) {
            throw new RuntimeException("TF initialization failed");
        }

        int numClasses =
                (int) c.inferenceInterface.graph().operation(outputName).output(0).shape().size(0);



        c.inputSize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;

        c.outputNames = new String[]{outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[numClasses];
        //inputsize = 224

        return c;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {

        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");

        //getPixels()函数把一张图片，从指定的偏移位置（offset），指定的位置（x,y）截取指定的宽高（width,height ），
        // 把所得图像的每个像素颜色转为int值，存入pixels。
        //stride 参数指定在行之间跳过的像素的数目。图片是二维的，存入一个一维数组中，那么就需要这个参数来指定多少个像素换一行。
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
           // Log.d(TAG, "getResult1: " + floatValues[i * 3 + 0] + "   " + floatValues[i * 3 + 1] + "   " + floatValues[i * 3 + 2] + "   ");
        }
        Trace.endSection();

        // input.
        Trace.beginSection("fillNodeFloat");
        inferenceInterface.fillNodeFloat(
                inputName, new int[]{1, inputSize, inputSize, 3}, floatValues);
        Trace.endSection();

        // 识别操作
        Trace.beginSection("runInference");
        inferenceInterface.runInference(outputNames);
        Trace.endSection();

        //将输出的tensor放到数组
        Trace.beginSection("readNodeFloat");
        inferenceInterface.readNodeFloat(outputName, outputs);
        Trace.endSection();

        // 使用PriorityQueue来遍历label寻找最好的标签
        //Comparator是在集合外部实现的排序
        PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        3,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // 将可能性最高的放在开头
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < outputs.length; ++i) {
            if (outputs[i] > THRESHOLD) {
                pq.add(
                        new Recognition(
                                "" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i], null));
            }
        }
        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    @Override
    public void enableStatLogging(boolean debug) {
        inferenceInterface.enableStatLogging(debug);
    }

    @Override
    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    @Override
    public void close() {
        inferenceInterface.close();
    }
}
