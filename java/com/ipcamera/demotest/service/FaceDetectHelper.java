package com.ipcamera.demotest.service;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.ipcamera.demotest.common.Constants;
import com.ipcamera.demotest.database.CompareResult;
import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.model.FaceCardInfo;
import com.ipcamera.demotest.model.LivenessAndFeature;
import com.ipcamera.demotest.utils.P;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import vstc2.nativecaller.NativeCaller;

public class FaceDetectHelper {
    private final String TAG = "FaceDetectHelper";
    private static Context mContext;
    private Boolean livenessDetect;
    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;

    /**
     * 用于活体检测和特征提取的引擎
     */
    private final FaceEngine frlEngine;

    private FaceMonitor faceMonitor;


    FaceDetectHelper(FaceDetectHelper.Builder builder) {
        ftEngine = builder.ftEngine;
        frlEngine = builder.frEngine;
        faceMonitor = builder.faceMonitor;
        livenessDetect = builder.livenessDetect;
        /**
         * 初始化人脸探测线程池
         */
        int fdQueueSize = 5;
        fdThreadQueue = new LinkedBlockingQueue<>(fdQueueSize);
        faceDetectExecutor = new ThreadPoolExecutor(1, fdQueueSize, 0, TimeUnit.MILLISECONDS, fdThreadQueue);

        /**
         * 初始化特征提取和活体检测线程池
         */
        int frlQueueSize = 5;
        frlThreadQueue = new LinkedBlockingQueue<Runnable>(frlQueueSize);
        frlExecutor = new ThreadPoolExecutor(1, frlQueueSize, 0, TimeUnit.MILLISECONDS, frlThreadQueue);
    }
//    /**
//     * 在特征库中搜索
//     *
//     * @param faceFeature 传入特征数据
//     * @return 比对结果
//     */
//    public CompareResult getTopOfFaceMap(FaceFeature faceFeature, int filter) {
//        if (faceEngine == null || isProcessing || faceFeature == null || faceRegisterInfoList == null || faceRegisterInfoList.size() == 0) {
//            return null;
//        }
//        // 初始化人脸库列表,根据过滤条件转存在HashMap中,加快搜索速度
//        List<FaceCardInfo> libFaceList = DBMaster.getLocalDB().getLocalFaceInfoTable().queryFace();
//        for (FaceCardInfo faceCardInfo : libFaceList) {
//            if (filter == Constants.NO_FILTER) {
//                libFaceMap.put(faceCardInfo.getId(), faceCardInfo);
//            } else if (filter == Constants.ONLY_HIGH_MEDIUM) {
//                if (faceCardInfo.getPriority().equals(Constants.HIGH_DETECT_PRIORITY) ||
//                        faceCardInfo.getPriority().equals(Constants.MEDIUM_DETECT_PRIORITY)) {
//                    libFaceMap.put(faceCardInfo.getId(), faceCardInfo);
//                }
//            } else if (filter == Constants.ONLY_HIGH) {
//                if (faceCardInfo.getPriority().equals(Constants.HIGH_DETECT_PRIORITY)) {
//                    libFaceMap.put(faceCardInfo.getId(), faceCardInfo);
//                }
//            }
//        }
//        FaceFeature tempFaceFeature = new FaceFeature();
//        FaceSimilar faceSimilar = new FaceSimilar();
//        float maxSimilar = 0;
//        int maxSimilarIndex = -1;
//        isProcessing = true;
//        for (int i = 0; i < faceRegisterInfoList.size(); i++) {
//            tempFaceFeature.setFeatureData(faceRegisterInfoList.get(i).getFeatureData());
//            faceEngine.compareFaceFeature(faceFeature, tempFaceFeature, faceSimilar);
//            if (faceSimilar.getScore() > maxSimilar) {
//                maxSimilar = faceSimilar.getScore();
//                maxSimilarIndex = i;
//            }
//        }
//        isProcessing = false;
//        if (maxSimilarIndex != -1) {
//            return new CompareResult(faceRegisterInfoList.get(maxSimilarIndex).getName(), maxSimilar);
//        }
//        return null;
//    }


    /**
     * 人脸探测线程池
     */
    private ExecutorService faceDetectExecutor;
    /**
     * 人脸探测线程队列
     */
    private LinkedBlockingQueue<Runnable> fdThreadQueue;

    public void requestFaceDetect(byte[] h264, int width, int height) {
        if (faceMonitor != null) {
            if (ftEngine != null && fdThreadQueue.remainingCapacity() > 0) {
                faceDetectExecutor.execute(new FaceDetectHelper.FaceDetectRunnable(h264, width, height));
            }
        }
    }


    public class FaceDetectRunnable implements Runnable {
        byte[] h264;
        int width;
        int height;

        private FaceDetectRunnable(byte[] h264, int width, int height) {
            if (h264 == null) {
                return;
            }
            this.h264 = h264;
            this.width = width;
            this.height = height;
        }


        @Override
        public void run() {
//            P.log("人脸探测线程队列余量:"+fdThreadQueue.remainingCapacity());
            // TODO 图片需要向右旋转90度,或设置为全方位人脸检测
            /**
             * h264转rgb565
             */
            List<FaceInfo> faceInfoList = new ArrayList<>();
            faceInfoList.clear();
            byte[] rgb = new byte[width * height * 2];
            NativeCaller.YUV4202RGB565(h264, rgb, width, height);
            ByteBuffer buffer = ByteBuffer.wrap(rgb);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            bitmap.copyPixelsFromBuffer(buffer);

            /**
             * rgb565转bgr24
             */
            Bitmap mBitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true);
            int mBitmapWidth = mBitmap.getWidth();
            int mBitmapHeight = mBitmap.getHeight();
            byte[] bgr24 = ArcSoftImageUtil.createImageData(mBitmap.getWidth(), mBitmap.getHeight(), ArcSoftImageFormat.BGR24);
            int transformCode = ArcSoftImageUtil.bitmapToImageData(mBitmap, bgr24, ArcSoftImageFormat.BGR24);
            if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                Log.e(TAG, "565转24失败，transform failed, code is " + transformCode);
            }
            /**
             * 人脸探测
             */
            int code = ftEngine.detectFaces(bgr24, mBitmapWidth, mBitmapHeight, FaceEngine.CP_PAF_BGR24, faceInfoList);
            if (code != ErrorInfo.MOK) {
                return;
            }
            if (faceInfoList.size() > 0) {
                Log.d(TAG, "图片中探测到人脸");
                faceMonitor.onGetDetectedFace(faceInfoList, bgr24, width, height);
            }

        }
    }


    /**
     * 人脸探测线程池
     */
    private ExecutorService frlExecutor;
    /**
     * 特征提取线程队列
     */
    private LinkedBlockingQueue<Runnable> frlThreadQueue;

    public void requestFeatureExtract(List<FaceInfo> faceInfoList, byte[] bgr24, int width, int height) {
        if (faceMonitor != null) {
            if (frlEngine != null && frlThreadQueue.remainingCapacity() > 0) {
                frlExecutor.execute(new FaceDetectHelper.FeatureExtractRunnable(faceInfoList, bgr24, width, height));
            }
        }
    }

    public class FeatureExtractRunnable implements Runnable {
        List<FaceInfo> faceInfoList;
        byte[] bgr24;
        int width;
        int height;

        private FeatureExtractRunnable(List<FaceInfo> faceInfoList, byte[] bgr24, int width, int height) {
            if (faceInfoList == null || faceInfoList.size() <= 0) {
                return;
            }
            this.faceInfoList = faceInfoList;
            this.bgr24 = bgr24;
            this.width = width;
            this.height = height;
        }


        @Override
        public void run() {
//            P.log("特征提取线程队列余量:"+frlThreadQueue.remainingCapacity());
            if (frlEngine != null && faceInfoList != null && faceInfoList.size() > 0) {
                List<LivenessAndFeature> resultList = new ArrayList<>();
                if (livenessDetect) {
                    // 如果开启了活体检测,进行如下操作
                    for (int i = 0; i < faceInfoList.size(); i++) {
                        // 逐个人脸进行活体检测和特征提取
                        LivenessAndFeature result = new LivenessAndFeature();
                        List<LivenessInfo> livenessInfoList = new ArrayList<>();
                        int flCode;
                        synchronized (frlEngine) {
                            flCode = frlEngine.process(bgr24, width, height, FaceEngine.CP_PAF_BGR24, Arrays.asList(faceInfoList.get(i)), FaceEngine.ASF_LIVENESS);
                            if (flCode == ErrorInfo.MOK) {
                                flCode = frlEngine.getLiveness(livenessInfoList);
                            }
                        }
                        if (flCode == ErrorInfo.MOK && livenessInfoList.size() > 0) {
                            // 活体检测成功
                            if (livenessInfoList.get(0).getLiveness() == LivenessInfo.ALIVE) {// 只考虑ALIVE和NOT_ALIVE的情况
                                // 活体检测通过,进行特征提取
                                FaceFeature faceFeature = new FaceFeature();
                                int frCode;
                                synchronized (frlEngine) {
                                    frCode = frlEngine.extractFaceFeature(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList.get(i), faceFeature);
                                }
                                if (frCode == ErrorInfo.MOK) {
                                    Log.d(TAG, "featureExtract: 特征提取成功！" + frCode);
                                    result.setLivenessInfo(true);
                                    result.setFaceFeature(faceFeature);
                                    resultList.add(result);
                                } else {
                                    Log.d(TAG, "featureExtract: 特征提取失败！错误码是" + frCode);
                                }
                            } else if (livenessInfoList.get(0).getLiveness() == LivenessInfo.NOT_ALIVE) {// 只考虑ALIVE和NOT_ALIVE的情况
                                // 活体检测未通过,不再进行特征提取
                                Log.d(TAG, "初步检测到非活体");
                                result.setLivenessInfo(false);
                                result.setFaceFeature(null);
                                resultList.add(result);
                            }
                        }
                    }
                    // (主线程中人脸属性分析要用到这些参数)
                    faceMonitor.onGetFeature(resultList, bgr24, width, height, faceInfoList);
                } else {
                    // 只进行特征提取
                    List<FaceFeature> featureList = new ArrayList<>();
                    for (int i = 0; i < faceInfoList.size(); i++) {
                        FaceFeature faceFeature = new FaceFeature();
                        int frCode;
                        synchronized (frlEngine) {
                            frCode = frlEngine.extractFaceFeature(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList.get(i), faceFeature);
                        }
                        if (frCode == ErrorInfo.MOK) {
                            Log.d(TAG, "featureExtract: 特征提取成功！" + frCode);
                            featureList.add(faceFeature);
                        } else {
                            Log.d(TAG, "featureExtract: 特征提取失败！错误码是" + frCode);
                        }
                    }
                    faceMonitor.onOnlyGetFeature(featureList, bgr24, width, height, faceInfoList);
                }
            }
        }
    }

    public static final class Builder {
        private FaceEngine ftEngine;
        private FaceEngine frEngine;
        private FaceEngine flEngine;
        private FaceMonitor faceMonitor;
        Boolean livenessDetect;
        private int frQueueSize;
        private int flQueueSize;

        public Builder() {
        }

        public FaceDetectHelper.Builder setContext(Context val) {
            mContext = val;
            return this;
        }

        public FaceDetectHelper.Builder ftEngine(FaceEngine val) {
            ftEngine = val;
            return this;
        }

        public FaceDetectHelper.Builder frEngine(FaceEngine val) {
            frEngine = val;
            return this;
        }

        public FaceDetectHelper.Builder flEngine(FaceEngine val) {
            flEngine = val;
            return this;
        }


        public FaceDetectHelper.Builder frQueueSize(int val) {
            frQueueSize = val;
            return this;
        }

        public FaceDetectHelper.Builder flQueueSize(int val) {
            flQueueSize = val;
            return this;
        }


        public FaceDetectHelper.Builder faceMonitor(FaceMonitor monitor) {
            faceMonitor = monitor;
            return this;
        }

        public FaceDetectHelper.Builder livenessDetect(Boolean val) {
            livenessDetect = val;
            return this;
        }

        public FaceDetectHelper build() {
            return new FaceDetectHelper(this);
        }
    }

    /**
     * 释放对象
     */
    public void release() {
        if (!frlExecutor.isShutdown()) {
            frlExecutor.shutdownNow();
            frlThreadQueue.clear();
        }
        if (!faceDetectExecutor.isShutdown()) {
            faceDetectExecutor.shutdownNow();
            fdThreadQueue.clear();
        }

        if (frlThreadQueue != null) {
            frlThreadQueue.clear();
            frlThreadQueue = null;
        }
        if (fdThreadQueue != null) {
            fdThreadQueue.clear();
            fdThreadQueue = null;
        }
        // 等待线程处理完毕
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        faceMonitor = null;
    }

}

