package com.ipcamera.demotest.service;

import android.content.pm.FeatureInfo;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.ipcamera.demotest.model.LivenessAndFeature;

import java.util.List;

public interface FaceMonitor {

    void onGetDetectedFace(List<FaceInfo> faceInfoList, byte[] bgr24, int width, int height);
    // 用于开启活体检测的回调
    void onGetFeature(List<LivenessAndFeature> resultList, byte[] bgr24, int width, int height, List<FaceInfo> faceInfoList);
    // 用于未开启活体检测的回调
    void onOnlyGetFeature(List<FaceFeature> featureList, byte[] bgr24, int width, int height, List<FaceInfo> faceInfoList);
}
