package com.ipcamera.demotest.model;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.LivenessInfo;

public class LivenessAndFeature {

    Boolean isliveness;
    FaceFeature faceFeature;

    public FaceFeature getFaceFeature() {
        return faceFeature;
    }

    public Boolean getLivenessInfo() {
        return isliveness;
    }

    public void setFaceFeature(FaceFeature faceFeature) {
        this.faceFeature = faceFeature;
    }

    public void setLivenessInfo(Boolean isliveness) {
        this.isliveness = isliveness;
    }
}
