package com.ipcamera.demotest.model;

import com.arcsoft.face.FaceInfo;

import java.util.List;

public class FaceInfoAndBGR24 {

    private List<FacePreviewInfo> faceInfoList;
    private byte[] bgr24;

    public byte[] getBgr24() {
        return bgr24;
    }

    public void setBgr24(byte[] bgr24) {
        this.bgr24 = bgr24;
    }

    public List<FacePreviewInfo> getFaceInfoList() {
        return faceInfoList;
    }

    public void setFaceInfoList(List<FacePreviewInfo> faceInfoList) {
        this.faceInfoList = faceInfoList;
    }
}
