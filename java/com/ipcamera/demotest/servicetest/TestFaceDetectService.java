package com.ipcamera.demotest.servicetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.LivenessParam;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.ipcamera.demotest.activity.MainInterfaceActivity;
import com.ipcamera.demotest.activity.VideoRegActivity;
import com.ipcamera.demotest.adapter.FaceSearchResultAdapter;
import com.ipcamera.demotest.common.Constants;
import com.ipcamera.demotest.common.SharedPreferenceUtil;
import com.ipcamera.demotest.database.CompareResult;
import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.model.FaceCardInfo;
import com.ipcamera.demotest.database.localdb.FaceServer;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.ipcamer.demotest.R;
import com.ipcamera.demotest.model.FaceInfoAndBGR24;
import com.ipcamera.demotest.model.FacePreviewInfo;
import com.ipcamera.demotest.model.FaceRectView;
import com.ipcamera.demotest.model.LivenessAndFeature;
import com.ipcamera.demotest.service.ConnectService;
import com.ipcamera.demotest.service.FaceMonitor;
import com.ipcamera.demotest.service.LiveStreamState;
import com.ipcamera.demotest.utils.DrawHelper;
import com.ipcamera.demotest.utils.P;
import com.ipcamera.demotest.utils.SystemValue;
import com.ipcamera.demotest.utils.TTSPlayer;
import com.ipcamera.demotest.utils.camera.CameraHelper;
import com.ipcamera.demotest.utils.face.FaceHelper;
import com.ipcamera.demotest.utils.face.FaceListener;
import com.ipcamera.demotest.utils.face.LivenessType;
import com.ipcamera.demotest.utils.face.RequestFeatureStatus;
import com.ipcamera.demotest.utils.face.RequestLivenessStatus;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import vstc2.nativecaller.NativeCaller;

/**
 * 人脸识别服务,对摄像头返回的每帧数据实时处理,并进行语音通知
 */
public class TestFaceDetectService extends Service implements ConnectService.FaceDetectServiceInterface, TestFaceDetectHelper.FaceMonitor {

    private final Context mContext = TestFaceDetectService.this;
    private final String TAG = "TestFaceDetectService";
    private String strDID = null;//设备ID

    //多人脸搜索中最多可显示的人脸个数
    private static final int MAX_DETECT_NUM = 10;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /**
     * 失败重试间隔时间（ms）
     */
    private static final long FAIL_RETRY_INTERVAL = 1000;
    /**
     * 出错重试最大次数
     */
    private static final int MAX_RETRY_TIME = 3;

    /**
     * 视频帧图像宽高
     */
    private int imageWidth = 1920;
    private int imageHeight = 1080;


    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;

    /**
     * IMAGE模式人脸属性分析引擎
     */
    private FaceEngine attrEngine;

    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private int attrInitCode = -1;
    private TestFaceDetectHelper faceHelper;

    private List<CompareResult> compareResultList = new ArrayList<>();

    private List<FaceCardInfo> faceInfoList1;

    /**
     * 当前帧图像中的人脸信息列表
     */
    private List<FaceInfo> faceInfoList = new ArrayList<>();
    private List<FacePreviewInfo> facePreviewInfoList = new ArrayList<>();

    /**
     * 是否开启了活体检测
     */
    private boolean livenessDetect;

    /**
     * 用于记录人脸识别相关状态
     * <p>
     * 只有状态为空status == null或者TO_RETRY或者时才发起人脸特征提取请求
     * 例如:1当特征提取成功后,且人脸匹配成功,且人脸始终在画面中,系统不会二次进行人脸特征提取请求,可以节省资源
     * 2如果特征提取失败,在最大重试次数内,每次帧回调时系统会立即发起特征提取请求,若超过最大重试次数,设置延时发起特征提取请求,并将重试次数清零,开始下一个循环
     * 3如果特征提取成功,但没有匹配到人脸,系统会设置延时发起特征提取请求,每隔1.1秒尝试一次,直到匹配到人脸,并将其状态设为SUCCEED(在这段延时中状态仍为SEARCHING,
     * 而且延时结束后系统也只是将状态改变为TO_RETRY以通知系统发起提取请求,以保证多线程有序进行)
     */
    private final ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private final ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体值(//用于储存活体值及相关检测状态)
     */
    private final ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体检测出错重试次数
     */
    private final ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();

    private final CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();

    private final CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();

    /**
     * 识别阈值
     */
    private static final float SIMILAR_THRESHOLD = 0.8F;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent nfIntent = new Intent(this, MainInterfaceActivity.class);
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext())
                .setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setSmallIcon(R.drawable.icon_1) // 设置状态栏内的小图标
//                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentTitle("已开启人脸识别服务")
                .setContentText("点击可关闭人脸识别服务") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //修改安卓8.1以上系统报错
            NotificationChannel notificationChannel = new NotificationChannel("1111", "1111", NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setShowBadge(false);//是否显示角标
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            builder.setChannelId("1111");
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startInd) {
        init();
        return super.onStartCommand(intent, flags, startInd);
    }

    public void init() {
//        sameFaceInterval = SharedPreferenceUtil.getVoiceReminderInterval(mContext);
//        sameFaceInterval = sameFaceInterval * 1000;
//        commonInterval = sameFaceInterval;
//        // 人脸时识别过滤条件
//        filter = SharedPreferenceUtil.getFilter(mContext);

//        List<FaceCardInfo> faceInfoList = DBMaster.getLocalDB().getLocalFaceInfoTable().queryFace();
//        for (FaceCardInfo faceCardInfo : faceInfoList) {
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
//        P.log("本次服务人脸个数" + libFaceMap.size());
        livenessDetect = SharedPreferenceUtil.getLivenessDetectState(mContext);
        P.log("活体检测开关："+livenessDetect);
        initEngine();
        ConnectService.setServiceInterface(this);
        // 初始化人脸识别帮助类
        faceHelper = new TestFaceDetectHelper.Builder()
                .ftEngine(ftEngine)
                .frEngine(frEngine)
                .flEngine(flEngine)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .frQueueSize(MAX_DETECT_NUM)
                .flQueueSize(MAX_DETECT_NUM)
                .faceMonitor(this)
                .build();
        strDID = SystemValue.deviceId;
        try {
            // 保证开启服务时的语音通知播放完
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 确保不能重复start!
        if (!LiveStreamState.isStarted) {
            NativeCaller.StartPPPPLivestream(strDID, 10, 1);
            LiveStreamState.isStarted = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        P.log("关闭人脸识别服务");
        // 停⽌请求视频数据(native)
        NativeCaller.StopPPPPLivestream(strDID);
        LiveStreamState.isStarted = false;
        // 接口置空,停止视频数据返回
        ConnectService.setServiceInterface(null);
        // 销毁引擎
        unInitEngine();
        faceHelper.release();
        faceHelper = null;
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, SharedPreferenceUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);

        attrEngine = new FaceEngine();
        attrInitCode = attrEngine.init(mContext, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_AGE | FaceEngine.ASF_GENDER);

        Log.i(TAG, "initEngine:  init: " + ftInitCode);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "frEngine", frInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "flEngine", flInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }

        if (attrInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "attrEngine", attrInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
    }

    private void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
        if (attrInitCode == ErrorInfo.MOK && attrEngine != null) {
            synchronized (attrEngine) {
                int attrInitCode = attrEngine.unInit();
                Log.i(TAG, "unInitEngine: " + attrInitCode);
            }
        }
    }

    @Override
    public void videoDataCallback(byte[] h264, int h264Data, int len, int width, int height) {
        // TODO 子线程进行人脸识别
        if (h264 != null) {
            /**
             * 原始视频流数据的粗加工
             */
            byte[] bgr24 = null;
            if (ftEngine != null) {
                facePreviewInfoList.clear();
                FaceInfoAndBGR24 processedInfo = faceHelper.trackFace(h264);
                facePreviewInfoList = processedInfo.getFaceInfoList();
                bgr24 = processedInfo.getBgr24();
            }
            // 清除离开视野的人脸
            clearLeftFace(facePreviewInfoList);

            if (facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
                for (int i = 0; i < facePreviewInfoList.size(); i++) {
                    Integer status = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());
                    /**
                     * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                     */
                    if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                        Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
                        if (liveness == null
                                || (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING)) {
                            livenessMap.put(facePreviewInfoList.get(i).getTrackId(), RequestLivenessStatus.ANALYZING);
                            faceHelper.requestFaceLiveness(bgr24, facePreviewInfoList.get(i).getFaceInfo(), facePreviewInfoList.get(i).getTrackId());
                        }
                    }
                    /**
                     * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                     * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                     */
                    if (status == null
                            || status == RequestFeatureStatus.TO_RETRY) {
                        requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                        faceHelper.requestFaceFeature(bgr24, facePreviewInfoList.get(i).getFaceInfo(), facePreviewInfoList.get(i).getTrackId());
                    }
                }
            }
        }

    }

    @Override
    public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
        Log.d(TAG, "onFaceFeatureInfoGet: ");
        //FR成功(特征提取成功)
        if (faceFeature != null) {
            Integer liveness = livenessMap.get(requestId);
            //不做活体检测的情况，直接搜索
            if (!livenessDetect) {
                searchFace(faceFeature, requestId);
            }
            //活体检测通过，搜索特征
            else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                searchFace(faceFeature, requestId);
            }
            //活体检测未出结果(fl线程还没执行完,还未回传结果),或者检测为非活体,延迟执行该函数
            else {
                if (requestFeatureStatusMap.containsKey(requestId)) {
                    Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                            .subscribe(new Observer<Long>() {
                                Disposable disposable;

                                @Override
                                public void onSubscribe(Disposable d) {
                                    disposable = d;
                                    getFeatureDelayedDisposables.add(disposable);
                                }

                                @Override
                                public void onNext(Long aLong) {
                                    onFaceFeatureInfoGet(faceFeature, requestId, errorCode);
                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onComplete() {
                                    getFeatureDelayedDisposables.remove(disposable);
                                }
                            });
                }
            }

        }
        //特征提取失败
        else {
            // 该判断的意义在于判断下一帧图像回调时(onPreview()),是否对重新进行特征提取的操作设置时延
            if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                // 没看懂为什么要置零(置零表示一次特征提取操作(尝试了多次后)结束,认为特征提取失败了,并将该trackID的状态置为FAILED
                // 并设置时延,重新进行特征提取
                extractErrorRetryMap.put(requestId, 0);

                String msg;
                // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                    msg = getString(R.string.low_confidence_level);
                } else {
                    msg = "ExtractCode:" + errorCode;
                }
                faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                retryRecognizeDelayed(requestId);
            } else {
                // 状态置为TO_RETRY,当onPreview()再次被回调时,可立即对该requestId对应的人脸再次进行特征提取
                // 这个TO_RETRY状态和活体检测的UNKNOWN状态类似
                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
            }
        }
    }

    @Override
    public void onError(Exception error) {
        Log.e(TAG, "onFail: " + error.getMessage());
    }

    // livenessInfo:活体检测结果(活体信息)
    // requestId:即trackID
    // errorCode:活体检测错误码

    // 调用过程同上
    //
    @Override
    public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode) {
        if (livenessInfo != null) {
            int liveness = livenessInfo.getLiveness();
            livenessMap.put(requestId, liveness);
            // 非活体，重试
            if (liveness == LivenessInfo.NOT_ALIVE) {
                faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                retryLivenessDetectDelayed(requestId);
            }
        } else {
            if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                livenessErrorRetryMap.put(requestId, 0);
                String msg;
                // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                    msg = getString(R.string.low_confidence_level);
                } else {
                    msg = "ProcessCode:" + errorCode;
                }
                faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                retryLivenessDetectDelayed(requestId);
            } else {
                // 状态设置为UNKNOWN,表示活体检测失败,且重试次数未达上限,下次图像帧回调时可立即重新进行活体检测
                livenessMap.put(requestId, LivenessInfo.UNKNOWN);
            }
        }
    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable.create((ObservableOnSubscribe<CompareResult>) emitter -> {
            CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
            emitter.onNext(compareResult);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.setName(requestId, "VISITOR " + requestId);
                            return;
                        }

                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.setName(requestId, "VISITOR " + requestId);
                                return;
                            }
                            for (CompareResult compareResult1 : compareResultList) {
                                if (compareResult1.getTrackId() == requestId) {
                                    isAdded = true;
                                    break;
                                }
                            }
                            if (!isAdded) {
                                // 若该人脸是首次进入视野，添加该人脸
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    compareResultList.remove(0);
                                }
                                //添加显示人员时，保存其trackId
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);
                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.setName(requestId, getString(R.string.recognize_success_notice, compareResult.getUserName()));
                            TTSPlayer.getInstance().startSpeaking("已注册用户");
                        } else {
                            // 暂无匹配人脸的情况下
                            faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                            retryRecognizeDelayed(requestId);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                        retryRecognizeDelayed(requestId);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private void retryLivenessDetectDelayed(final Integer requestId) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     * @param requestId 人脸ID
     */
    private void retryRecognizeDelayed(final Integer requestId) {
        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为TO_RETRY，帧回调处理时会重新进行特征提取
                        faceHelper.setName(requestId, Integer.toString(requestId));
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        // 遍历compareResultList中的trackID值,若不存在于requestFeatureStatusMap中,则将该trackID值从compareResultList中移除
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                if (!requestFeatureStatusMap.containsKey(compareResultList.get(i).getTrackId())) {
                    compareResultList.remove(i);
                }
            }
        }
        // 清空所有状态集合
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            livenessErrorRetryMap.clear();
            extractErrorRetryMap.clear();
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
            return;
        }
        // 对requestFeatureStatusMap中的trackID值遍历,判断值是否存在于facePreviewInfoList中,
        // 若不存在则将该trackID值从所有状态集合(Map)中移除
        Enumeration<Integer> keys = requestFeatureStatusMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(key);
                livenessMap.remove(key);
                livenessErrorRetryMap.remove(key);
                extractErrorRetryMap.remove(key);
            }
        }


    }

    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    public int increaseAndGetValue(Map<Integer, Integer> countMap, int key) {
        if (countMap == null) {
            return 0;
        }
        Integer value = countMap.get(key);
        if (value == null) {
            value = 0;
        }
        countMap.put(key, ++value);
        return value;
    }


}
