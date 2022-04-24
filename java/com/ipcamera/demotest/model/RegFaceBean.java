package com.ipcamera.demotest.model;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;

public class RegFaceBean {

    private Bitmap faceImage;
    private FaceFeature feature;
    private String id;
    private FaceInfo faceInfo;
    private byte[] bgr24;
    private int width;
    private int height;

    public Bitmap getFaceImage() {
        return faceImage;
    }

    public String getId() {
        return id;
    }

    public FaceFeature getFeature() {
        return feature;
    }

    public void setFaceImage(Bitmap faceImage) {
        this.faceImage = faceImage;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFeature(FaceFeature feature) {
        this.feature = feature;
    }

    public void setBgr24(byte[] bgr24) {
        this.bgr24 = bgr24;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public byte[] getBgr24() {
        return bgr24;
    }
}
