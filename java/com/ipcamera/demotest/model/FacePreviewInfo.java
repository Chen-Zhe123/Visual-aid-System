package com.ipcamera.demotest.model;

import com.arcsoft.face.FaceInfo;

/**
 * 封装了FaceInfo和trackId
 */
public class FacePreviewInfo {
    private FaceInfo faceInfo;
    private int trackId;

    public FacePreviewInfo(FaceInfo faceInfo, int trackId) {
        this.faceInfo = faceInfo;
        this.trackId = trackId;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }


    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

}
