package com.ipcamera.demotest.servicetest;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.util.Log;

import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.ipcamera.demotest.model.FaceInfoAndBGR24;
import com.ipcamera.demotest.model.FacePreviewInfo;
import com.ipcamera.demotest.utils.TrackUtil;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.LivenessInfo;
import com.ipcamera.demotest.utils.face.LivenessType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import vstc2.nativecaller.NativeCaller;

/**
 * 人脸操作辅助类
 */
public class TestFaceDetectHelper {
    private static final String TAG = "FaceHelper";
    /**
     * 线程池正在处理任务
     */
    private static final int ERROR_BUSY = -1;
    /**
     * 特征提取引擎为空
     */
    private static final int ERROR_FR_ENGINE_IS_NULL = -2;
    /**
     * 活体检测引擎为空
     */
    private static final int ERROR_FL_ENGINE_IS_NULL = -3;
    /**
     * 人脸追踪引擎
     */
    private FaceEngine ftEngine;
    /**
     * 特征提取引擎
     */
    private FaceEngine frEngine;
    /**
     * 活体检测引擎
     */
    private FaceEngine flEngine;

    private int width;
    private int height;

    private List<FaceInfo> faceInfoList = new ArrayList<>();
    /**
     * 特征提取线程池
     */
    private ExecutorService frExecutor;
    /**
     * 活体检测线程池
     */
    private ExecutorService flExecutor;
    /**
     * 特征提取线程队列
     */
    private LinkedBlockingQueue<Runnable> frThreadQueue = null;
    /**
     * 活体检测线程队列
     */
    private LinkedBlockingQueue<Runnable> flThreadQueue = null;

    private FaceMonitor faceMonitor;

    public List<Integer> currentTrackIdList = new ArrayList<>();
    private List<FacePreviewInfo> facePreviewInfoList = new ArrayList<>();
    /**
     * 用于存储人脸对应的ID，KEY为trackId，VALUE为ID
     */
    private ConcurrentHashMap<Integer, String> nameMap = new ConcurrentHashMap<>();

    public interface FaceMonitor {
        void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode);

        void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode);

        void onError(Exception error);
    }

    private TestFaceDetectHelper(Builder builder) {
        ftEngine = builder.ftEngine;
        frEngine = builder.frEngine;
        flEngine = builder.flEngine;
        faceMonitor = builder.faceMonitor;
        width = builder.width;
        height = builder.height;

        /**
         * fr 线程队列大小
         */
        int frQueueSize = 5;
        if (builder.frQueueSize > 0) {
            frQueueSize = builder.frQueueSize;
        } else {
            Log.e(TAG, "frThread num must > 0,now using default value:" + frQueueSize);
        }
        frThreadQueue = new LinkedBlockingQueue<>(frQueueSize);
        frExecutor = new ThreadPoolExecutor(1, frQueueSize, 0, TimeUnit.MILLISECONDS, frThreadQueue);

        /**
         * fl 线程队列大小
         */
        int flQueueSize = 5;
        if (builder.flQueueSize > 0) {
            flQueueSize = builder.flQueueSize;
        } else {
            Log.e(TAG, "flThread num must > 0,now using default value:" + flQueueSize);
        }
        flThreadQueue = new LinkedBlockingQueue<Runnable>(flQueueSize);
        flExecutor = new ThreadPoolExecutor(1, flQueueSize, 0, TimeUnit.MILLISECONDS, flThreadQueue);
        if (width == 0 || height == 0) {
            throw new RuntimeException("previewSize must be specified!");
        }
    }

    public byte[] h264ToBGR24(byte[] h264){
        // TODO 图片需要向右旋转90度,或设置为全方位人脸检测
        byte[] bgr24;
        /**
         * h264转rgb565
         */
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
        bgr24 = ArcSoftImageUtil.createImageData(mBitmap.getWidth(), mBitmap.getHeight(), ArcSoftImageFormat.BGR24);
        int transformCode = ArcSoftImageUtil.bitmapToImageData(mBitmap, bgr24, ArcSoftImageFormat.BGR24);
        if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            Log.e(TAG, "565转24失败，transform failed, code is " + transformCode);
        }
        return bgr24;
    }

    /**
     * 请求获取人脸特征数据
     *
     * @param bgr24     图像数据
     * @param faceInfo 人脸信息
     * @param trackId  请求人脸特征的唯一请求码，一般使用trackId
     */
    public void requestFaceFeature(byte[] bgr24, FaceInfo faceInfo, Integer trackId) {
        if (faceMonitor != null) {
            if (frEngine != null && frThreadQueue.remainingCapacity() > 0) {
                frExecutor.execute(new FaceRecognizeRunnable(bgr24, faceInfo, trackId));
            } else {
                faceMonitor.onFaceFeatureInfoGet(null, trackId, ERROR_BUSY);
            }
        }
    }

    /**
     * 请求获取活体检测结果，需要传入活体的参数，以下参数同
     *
     * @param bgr24         BGR24格式的图像数据
     * @param faceInfo     人脸信息
     * @param trackId      请求人脸特征的唯一请求码，一般使用trackId
     */
    public void requestFaceLiveness(byte[] bgr24, FaceInfo faceInfo,  Integer trackId) {
        if (faceMonitor != null) {
            if (flEngine != null && flThreadQueue.remainingCapacity() > 0) {
                flExecutor.execute(new FaceLivenessDetectRunnable(bgr24, faceInfo, trackId));
            } else {
                faceMonitor.onFaceLivenessInfoGet(null, trackId, ERROR_BUSY);
            }
        }
    }

    /**
     * 释放对象
     */
    public void release() {
        if (!frExecutor.isShutdown()) {
            frExecutor.shutdownNow();
            frThreadQueue.clear();
        }
        if (!flExecutor.isShutdown()) {
            flExecutor.shutdownNow();
            flThreadQueue.clear();
        }
        if (faceInfoList != null) {
            faceInfoList.clear();
        }
        if (frThreadQueue != null) {
            frThreadQueue.clear();
            frThreadQueue = null;
        }
        if (flThreadQueue != null) {
            flThreadQueue.clear();
            flThreadQueue = null;
        }
        if (nameMap != null) {
            nameMap.clear();
        }
        nameMap = null;
        faceMonitor = null;
        faceInfoList = null;
    }

    /**
     * 处理帧数据
     *
     * @param h264 摄像头回传的h264数据
     * @return 实时人脸处理结果，封装添加了一个trackId，trackId的获取依赖于faceId，用于记录人脸序号并保存
     */
    public FaceInfoAndBGR24 trackFace(byte[] h264) {

        byte[] bgr24 = null;
        if (ftEngine != null) {
            facePreviewInfoList.clear();
            faceInfoList.clear();
            // TODO 图片需要向右旋转90度,或设置为全方位人脸检测
            /**
             * h264转rgb565
             */
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
            bgr24 = ArcSoftImageUtil.createImageData(mBitmap.getWidth(), mBitmap.getHeight(), ArcSoftImageFormat.BGR24);
            int transformCode = ArcSoftImageUtil.bitmapToImageData(mBitmap, bgr24, ArcSoftImageFormat.BGR24);
            if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                Log.e(TAG, "565转24失败，transform failed, code is " + transformCode);
            }
            /**
             * 人脸追踪探测
             */
            int code = ftEngine.detectFaces(bgr24, mBitmapWidth, mBitmapHeight, FaceEngine.CP_PAF_BGR24, faceInfoList);
            refreshTrackId(faceInfoList);
        }
        for (int i = 0; i < faceInfoList.size(); i++) {
            facePreviewInfoList.add(new FacePreviewInfo(faceInfoList.get(i), currentTrackIdList.get(i)));
        }
        FaceInfoAndBGR24 processedInfo = new FaceInfoAndBGR24();
        processedInfo.setFaceInfoList(facePreviewInfoList);
        processedInfo.setBgr24(bgr24);
       return processedInfo;
    }

    /**
     * 人脸特征提取线程
     */
    public class FaceRecognizeRunnable implements Runnable {
        private FaceInfo faceInfo;
        private Integer trackId;
        private byte[] bgr24;

        private FaceRecognizeRunnable(byte[] bgr24, FaceInfo faceInfo, Integer trackId) {
            if (bgr24 == null) {
                return;
            }
            this.bgr24 = bgr24;
            this.faceInfo = new FaceInfo(faceInfo);
            this.trackId = trackId;
        }

        @Override
        public void run() {
            if (faceMonitor != null && bgr24 != null) {
                if (frEngine != null) {
                    FaceFeature faceFeature = new FaceFeature();
                    long frStartTime = System.currentTimeMillis();
                    int frCode;
                    synchronized (frEngine) {
                        frCode = frEngine.extractFaceFeature(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfo, faceFeature);
                    }
                    if (frCode == ErrorInfo.MOK) {
//                        Log.i(TAG, "run: fr costTime = " + (System.currentTimeMillis() - frStartTime) + "ms");
                        faceMonitor.onFaceFeatureInfoGet(faceFeature, trackId, frCode);
                    } else {
                        faceMonitor.onFaceFeatureInfoGet(null, trackId, frCode);
                        faceMonitor.onError(new Exception("fr failed errorCode is " + frCode));
                    }
                } else {
                    faceMonitor.onFaceFeatureInfoGet(null, trackId, ERROR_FR_ENGINE_IS_NULL);
                    faceMonitor.onError(new Exception("fr failed ,frEngine is null"));
                }
            }
            bgr24 = null;
        }
    }

    /**
     * 活体检测的线程
     */
    public class FaceLivenessDetectRunnable implements Runnable {
        private FaceInfo faceInfo;
        private Integer trackId;
        private byte[] bgr24;

        private FaceLivenessDetectRunnable(byte[] bgr24, FaceInfo faceInfo, Integer trackId) {
            if (bgr24 == null) {
                return;
            }
            this.bgr24 = bgr24;
            this.faceInfo = new FaceInfo(faceInfo);
            this.trackId = trackId;
        }

        @Override
        public void run() {
            if (faceMonitor != null && bgr24 != null) {
                if (flEngine != null) {
                    List<LivenessInfo> livenessInfoList = new ArrayList<>();
                    int flCode;
                    synchronized (flEngine) {

                            flCode = flEngine.process(bgr24, width, height, FaceEngine.CP_PAF_BGR24, Arrays.asList(faceInfo), FaceEngine.ASF_LIVENESS);
                    }
                    if (flCode == ErrorInfo.MOK) {
                            flCode = flEngine.getLiveness(livenessInfoList);
                    }

                    if (flCode == ErrorInfo.MOK && livenessInfoList.size() > 0) {
                        faceMonitor.onFaceLivenessInfoGet(livenessInfoList.get(0), trackId, flCode);
                    } else {
                        faceMonitor.onFaceLivenessInfoGet(null, trackId, flCode);
                        faceMonitor.onError(new Exception("fl failed errorCode is " + flCode));
                    }
                } else {
                    faceMonitor.onFaceLivenessInfoGet(null, trackId, ERROR_FL_ENGINE_IS_NULL);
                    faceMonitor.onError(new Exception("fl failed ,frEngine is null"));
                }
            }
            bgr24 = null;
        }
    }


    /**
     * 刷新trackId
     *
     * @param ftFaceList 传入的人脸列表
     */
    public void refreshTrackId(List<FaceInfo> ftFaceList) {
        currentTrackIdList.clear();
        for (FaceInfo faceInfo : ftFaceList) {
            currentTrackIdList.add(faceInfo.getFaceId());
        }
        //刷新nameMap
        clearLeftName(currentTrackIdList);
    }

    /**
     * 新增搜索成功的人脸
     *
     * @param trackId 指定的trackId
     * @param name    trackId对应的人脸
     */
    public void setName(int trackId, String name) {
        if (nameMap != null) {
            nameMap.put(trackId, name);
        }
    }

    public String getName(int trackId) {
        return nameMap == null ? null : nameMap.get(trackId);
    }

    /**
     * 清除map中已经离开的人脸
     *
     * @param trackIdList 最新的trackIdList
     */
    private void clearLeftName(List<Integer> trackIdList) {
        Enumeration<Integer> keys = nameMap.keys();
        while (keys.hasMoreElements()) {
            int value = keys.nextElement();
            if (!trackIdList.contains(value)) {
                nameMap.remove(value);
            }
        }
    }

    public static final class Builder {
        private FaceEngine ftEngine;
        private FaceEngine frEngine;
        private FaceEngine flEngine;
        private FaceMonitor faceMonitor;
        private int width;
        private int height;
        private int frQueueSize;
        private int flQueueSize;

        public Builder() {

        }


        public Builder ftEngine(FaceEngine val) {
            ftEngine = val;
            return this;
        }

        public Builder frEngine(FaceEngine val) {
            frEngine = val;
            return this;
        }

        public Builder flEngine(FaceEngine val) {
            flEngine = val;
            return this;
        }


        public Builder faceMonitor(FaceMonitor val) {
            faceMonitor = val;
            return this;
        }

        public Builder imageWidth(int val){
            width = val;
            return this;
        }

        public Builder imageHeight(int val){
            height = val;
            return this;
        }

        public Builder frQueueSize(int val) {
            frQueueSize = val;
            return this;
        }

        public Builder flQueueSize(int val) {
            flQueueSize = val;
            return this;
        }


        public TestFaceDetectHelper build() {
            return new TestFaceDetectHelper(this);
        }
    }
}
