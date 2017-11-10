package com.example.a6100890.gleaner.imageRecognition;

/**
 * Created by a6100890 on 2017/10/31.
 */

public class PostJson {
    private int type = 0;
    private String image_url;

    public PostJson(String image_url) {
        this.image_url = image_url;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
}
