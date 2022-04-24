package com.ipcamera.demotest.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.arcsoft.face.LivenessParam;
import com.ipcamera.demotest.activity.MainInterfaceActivity;
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
import com.ipcamera.demotest.model.LivenessAndFeature;
import com.ipcamera.demotest.utils.P;
import com.ipcamera.demotest.utils.SystemValue;
import com.ipcamera.demotest.utils.TTSPlayer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import vstc2.nativecaller.NativeCaller;

/**
 * 人脸识别服务,对摄像头返回的每帧数据实时处理,并进行语音通知
 */
public class FaceDetectService extends Service implements ConnectService.FaceDetectServiceInterface, FaceMonitor {

    private final String TAG = "FaceDetectService";
    private final Context mContext = FaceDetectService.this;
    private String strDID = null;//设备ID
    private int filter;

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于活体检测和特征提取的引擎
     */
    private FaceEngine frlEngine;
    /**
     * 用于属性检测的引擎
     */
    private FaceEngine attrEngine;
    /**
     * 初始化引擎
     */
    private int ftInitCode = -1;
    private int frlInitCode = -1;
    private int flInitCode = -1;
    private int attrInitCode = -1;

    /**
     * 活体检测的开关
     */
    private boolean livenessDetect;

    // 同一人脸判断为未注册用户的次数,当大于给定值时才确定为未注册用户
    int noRegCount = 0;
    // 每条语音通知的时间大约为3秒,作用如下
    final long TIME_INTERVAL = 3000;
    // 记录最近一次语音通知的时间
    long recentNoticeTime = System.currentTimeMillis();
    // 预计语音通知结束的时间
    long estimatedEndTime = System.currentTimeMillis();
    // 标记是否完成更新上述时间
    boolean isUpdated = true;
    // 本次服务中可识别的人脸名单
    private Map<String, FaceCardInfo> libFaceMap = new HashMap<>();
    // 已注册的同一人脸语音通知的时间间隔
    private int sameFaceInterval;
    // 语音通知的时间间隔
    private int commonInterval = 5000;
    /**
     * 在一次人脸识别服务中,记录每一次被探测到的时间,当同一人脸
     * 被再次探测到时,更新时间
     */
    private ConcurrentHashMap<String, Long> recentNoticeTimeMap = new ConcurrentHashMap<>();


    // 人脸相似度阈值
    final float SIMILARITY_THRESHOLD_YES = 0.75F;
    final float SIMILARITY_THRESHOLD_NO = 0.45F;

    //多人脸搜索中最多可显示的人脸个数
    private static final int MAX_DETECT_NUM = 10;

    private FaceDetectHelper faceHelper;

    public FaceDetectService() {
    }

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


    public void init() {
        sameFaceInterval = SharedPreferenceUtil.getVoiceReminderInterval(mContext);
        sameFaceInterval = sameFaceInterval * 1000;
        commonInterval = sameFaceInterval;
        // 人脸时识别过滤条件
        filter = SharedPreferenceUtil.getFilter(mContext);

        List<FaceCardInfo> faceInfoList = DBMaster.getLocalDB().getLocalFaceInfoTable().queryFace();
        for (FaceCardInfo faceCardInfo : faceInfoList) {
            if (filter == Constants.NO_FILTER) {
                libFaceMap.put(faceCardInfo.getId(), faceCardInfo);
            } else if (filter == Constants.ONLY_HIGH_MEDIUM) {
                if (faceCardInfo.getPriority().equals(Constants.HIGH_DETECT_PRIORITY) ||
                        faceCardInfo.getPriority().equals(Constants.MEDIUM_DETECT_PRIORITY)) {
                    libFaceMap.put(faceCardInfo.getId(), faceCardInfo);
                }
            } else if (filter == Constants.ONLY_HIGH) {
                if (faceCardInfo.getPriority().equals(Constants.HIGH_DETECT_PRIORITY)) {
                    libFaceMap.put(faceCardInfo.getId(), faceCardInfo);
                }
            }
        }
        P.log("本次服务人脸个数" + libFaceMap.size());
        livenessDetect = SharedPreferenceUtil.getLivenessDetectState(mContext);
        initEngine();
        ConnectService.setServiceInterface(this);
        faceHelper = new FaceDetectHelper.Builder()
                .ftEngine(ftEngine)
                .frEngine(frlEngine)
                .frQueueSize(MAX_DETECT_NUM)
                .flQueueSize(MAX_DETECT_NUM)
                .setContext(mContext)
                .faceMonitor(this)
                .livenessDetect(livenessDetect)
                .build();
        strDID = SystemValue.deviceId;
        try {
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

    private void initEngine() {
        // 人脸追踪探测引擎
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(mContext, DetectMode.ASF_DETECT_MODE_VIDEO, SharedPreferenceUtil.getFtOrient(mContext),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        // 活体检测和特征提取引擎
        frlEngine = new FaceEngine();
        frlInitCode = frlEngine.init(mContext, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_LIVENESS);
        // 设置活体阈值
        LivenessParam livenessParam = new LivenessParam(0.4F, 0.7F);
        frlEngine.setLivenessParam(livenessParam);

        // 人脸属性分析引擎
        attrEngine = new FaceEngine();
        attrInitCode = attrEngine.init(mContext, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_AGE | FaceEngine.ASF_GENDER);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
//            showToast(error);
        }
        if (frlInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "frEngine", frlInitCode);
            Log.i(TAG, "initEngine: " + error);
//            showToast(error);
        }
        if (attrInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "attrEngine", attrInitCode);
            Log.i(TAG, "initEngine: " + error);
//            showToast(error);
        }
    }

    /**
     * 销毁引擎，faceDetectHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frlInitCode == ErrorInfo.MOK && frlEngine != null) {
            synchronized (frlEngine) {
                int frUnInitCode = frlEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
    }

    /**
     * 1 人脸识别[已实现](考虑到网络摄像头的不稳定,只在人脸进入视野时进行通知,对于同一张人脸,可规定二次通知的时间间隔)
     * 2 根据人脸框大小变化判断人脸是否在向自己靠近
     * 3 根据人脸框大小通知对方到自己的距离
     * 4 根据人脸框位置变化判断对方的移动方向
     * 5 对未注册的人脸进行属性播报:年龄、性别[已实现]
     * 6 对探测到的所有人脸形成日志
     */


    private void updateTime(String str) {
        // 根据字符串长度估计语音通知时间(24s 92个汉字)一个汉字约261ms
        int stringLength = str.length();
        long duringTime = stringLength * 400;
        estimatedEndTime += duringTime;
        estimatedEndTime += commonInterval;
        isUpdated = true;
    }

    private void attributeAnalysis(StringBuilder noticeStr, byte[] bgr24, int width, int height, List<FaceInfo> faceInfoList) {
        // 处理图片,属性检测(faceInfoList只包含一张人脸)
        int faceProcessCode = attrEngine.process(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList, FaceEngine.ASF_AGE | FaceEngine.ASF_GENDER);
        if (faceProcessCode == ErrorInfo.MOK) {
            //年龄信息结果
            List<AgeInfo> ageInfoList = new ArrayList<>();
            //性别信息结果
            List<GenderInfo> genderInfoList = new ArrayList<>();
            //获取年龄、性别
            int ageCode = attrEngine.getAge(ageInfoList);
            int genderCode = attrEngine.getGender(genderInfoList);

            if ((ageCode & genderCode) == ErrorInfo.MOK) {
                if (ageInfoList.size() > 0 && genderInfoList.size() > 0 && genderInfoList.size() == ageInfoList.size()) {
                    for (int i = 0; i < ageInfoList.size(); i++) {
                        int age = ageInfoList.get(i).getAge();
                        String gender;
                        if (genderInfoList.get(i).getGender() == GenderInfo.MALE) {
                            gender = "男性";
                        } else if (genderInfoList.get(i).getGender() == GenderInfo.FEMALE) {
                            gender = "女性";
                        } else {
                            gender = "未知性别";
                        }
                        noticeStr.append("发现未知").append(gender).append("").append(age).append("岁，");
                    }
                }
            }
        } else {

        }
    }

    // 根据人脸框大小计算人脸距离
    // 人脸框宽355 距离50cm  宽593 距离30cm
    private String computeDistance(FaceInfo faceInfo, int width, int height) {
        int faceHeight = faceInfo.getRect().height();
        int faceWidth = faceInfo.getRect().width();
        // 单位米
        float dis = 17800.0F / (float) faceWidth / 100;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String disStr = decimalFormat.format(dis);
        return disStr;
    }

    @Override
    public void videoDataCallback(byte[] videobuf, int h264Data, int len, int width, int height) {
        // TODO 子线程进行人脸识别
        if (videobuf != null) {

            faceHelper.requestFaceDetect(videobuf, width, height);
        }

    }

    void printThreadNum(){
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        //activeCount()返回当前正在活动的线程的数量
        int total = Thread.activeCount();
        Thread[] threads = new Thread[total];
        //enumerate(threads)将当前线程组中的active线程全部复制到传入的线程数组threads中
        // 并且返回数组中元素个数，即线程组中active线程数量
        threadGroup.enumerate(threads);
        P.log("当前线程数:" + threads.length + "");
    }

    int detectFaceCount = 0;

    /**
     * 成功探测到人脸的回调
     */
    @Override
    public void onGetDetectedFace(List<FaceInfo> faceInfoList, byte[] bgr24, int width, int height) {
        if (faceInfoList != null && faceInfoList.size() > 0) {
//            P.log("人脸框，宽:" + faceInfoList.get(0).getRect().width() + "，高:" + faceInfoList.get(0).getRect().height() + "图像，宽:" + width + "，高:" + height);
            faceHelper.requestFeatureExtract(faceInfoList, bgr24, width, height);
        }
    }


    // 原算法是以人脸为单位检测
    // 改为了以一张图片为单位检测(一张图片可能有多个人脸),效率不好说......

    /**
     * 回传一帧图片上所有人脸的检测结果
     * <p>
     * 用于开启活体检测的回调
     */
    @Override
    public void onGetFeature(List<LivenessAndFeature> resultList, byte[] bgr24, int width, int height, List<FaceInfo> faceInfoList) {
        // 先判断是否可进行语音通知
        long nowTime = System.currentTimeMillis();
        if (isUpdated && estimatedEndTime < nowTime) {
            isUpdated = false;

//            printThreadNum();

            if (resultList != null && resultList.size() > 0) {
                int faceNumber = resultList.size();
                int notLiveNumber = 0;
                int notRegNumber = 0;
                StringBuilder noticeStr = new StringBuilder("");
                if (faceNumber == 1) {
                    // 只有一张人脸时
                    boolean isLive = resultList.get(0).getLivenessInfo();
                    FaceFeature faceFeature = resultList.get(0).getFaceFeature();
                    if (isLive) {
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(faceFeature);
                        if (libFaceMap.containsKey(compareResult.getUserName()) &&
                                compareResult != null && compareResult.getSimilar() > SIMILARITY_THRESHOLD_YES) {
                            String id = compareResult.getUserName();
                            P.log("最相似的人脸名称" + Objects.requireNonNull(libFaceMap.get(compareResult.getUserName())).getName() + "人脸相似度:" + compareResult.getSimilar());
                            int x = sameFaceInterval > commonInterval ? sameFaceInterval - commonInterval : 0;
                            if (!recentNoticeTimeMap.containsKey(id) || (recentNoticeTimeMap.containsKey(id) && (nowTime - estimatedEndTime) > commonInterval)) {
                                // 首次通知或达到了间隔时间后通知
                                String name = Objects.requireNonNull(libFaceMap.get(id)).getName();
                                String relationShip = Objects.requireNonNull(libFaceMap.get(id)).getFaceRelationShip();
                                noticeStr.append("发现").append(relationShip).append(name);
                                String distance = computeDistance(faceInfoList.get(0), width, height);
                                noticeStr.append(" 距您").append(distance).append("米");
                                TTSPlayer.getInstance().startSpeaking(noticeStr.toString());
                                // 更新语音预计结束时间
                                updateTime(noticeStr.toString());
                                // 更新时间为语音预计结束的时间+间隔时间
//                                recentNoticeTime = estimatedEndTime;
                                recentNoticeTimeMap.put(id, estimatedEndTime + sameFaceInterval);
                            }
                            isUpdated = true;
                        } else if (compareResult.getSimilar() <= SIMILARITY_THRESHOLD_NO) {
//                            P.log("开始属性提取");
                            attributeAnalysis(noticeStr, bgr24, width, height, Arrays.asList(faceInfoList.get(0)));
                            String distance = computeDistance(faceInfoList.get(0), width, height);
                            noticeStr.append("距您").append(distance).append("米");
                            TTSPlayer.getInstance().startSpeaking(noticeStr.toString());
                            updateTime(noticeStr.toString());
                        }
                    } else {
                        TTSPlayer.getInstance().startSpeaking("发现非活体人脸");
                        updateTime("发现非活体人脸");
                    }
                } else {
                    // 处理多人脸的情况
                    for (LivenessAndFeature result : resultList) {
                        if (result.getLivenessInfo()) {
                            // 若是活体
                            CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(result.getFaceFeature());
                            if (libFaceMap.containsKey(compareResult.getUserName()) && compareResult != null && compareResult.getSimilar() > SIMILARITY_THRESHOLD_YES) {
                                String id = compareResult.getUserName();
                                String name = Objects.requireNonNull(libFaceMap.get(id)).getName();
                                String relationShip = Objects.requireNonNull(libFaceMap.get(id)).getFaceRelationShip();
                                noticeStr.append("发现").append(relationShip).append(name);
                            } else {
                                // 对未注册人脸进行属性分析
                                notRegNumber++;
                                attributeAnalysis(noticeStr, bgr24, width, height, Arrays.asList(faceInfoList.get(resultList.indexOf(result))));
                            }
                        } else {
                            // 若是非活体人脸,不再进行任何操作
                            notLiveNumber++;
                        }
                    }
                    TTSPlayer.getInstance().startSpeaking(noticeStr.append("，共发现").append(faceNumber).append("张人脸，").append(notLiveNumber).append("张疑似非活体").toString());
                    updateTime(noticeStr.toString());
                }
            }
            isUpdated = true;
        }
    }

    /**
     * 回传一帧图片上所有人脸的检测结果
     * <p>
     * 用于未开启活体检测的回调
     */
    @Override
    public void onOnlyGetFeature(List<FaceFeature> featureList, byte[] bgr24, int width, int height, List<FaceInfo> faceInfoList) {
        // 先判断是否可进行语音通知
        long nowTime = System.currentTimeMillis();
        if (isUpdated && estimatedEndTime < nowTime) {
            isUpdated = false;
            if (featureList != null && featureList.size() > 0) {
                int faceNumber = featureList.size();
                int notRegNumber = 0;
                StringBuilder noticeStr = new StringBuilder("");
                if (faceNumber == 1) {
                    // 只有一张人脸时
                    FaceFeature faceFeature = featureList.get(0);
                    CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(faceFeature);
                    if (libFaceMap.containsKey(compareResult.getUserName()) &&
                            compareResult != null &&
                            compareResult.getSimilar() > SIMILARITY_THRESHOLD_YES) {
                        String id = compareResult.getUserName();
                        P.log("最相似的人脸名称" + Objects.requireNonNull(libFaceMap.get(compareResult.getUserName())).getName() + "人脸相似度:" + compareResult.getSimilar());
                        if (!recentNoticeTimeMap.containsKey(id) || (recentNoticeTimeMap.containsKey(id) && (nowTime - estimatedEndTime) > commonInterval)) {
                            String name = Objects.requireNonNull(libFaceMap.get(id)).getName();
                            String relationShip = Objects.requireNonNull(libFaceMap.get(id)).getFaceRelationShip();
                            noticeStr.append("发现").append(relationShip).append(name);
                            // TODO 计算距离
                            String distance = computeDistance(faceInfoList.get(0), width, height);
                            noticeStr.append(" 距您").append(distance).append("米");
                        }
                    } else if (compareResult.getSimilar() <= SIMILARITY_THRESHOLD_NO) {
                        // 下面代码可能需要加锁
                        attributeAnalysis(noticeStr, bgr24, width, height, Arrays.asList(faceInfoList.get(0)));
                        // TODO 计算距离
                        String distance = computeDistance(faceInfoList.get(0), width, height);
                        noticeStr.append("距您").append(distance).append("米");
                    }
                    TTSPlayer.getInstance().startSpeaking(noticeStr.toString());
                    updateTime(noticeStr.toString());
                } else {
                    // 处理多人脸
                    for (FaceFeature feature : featureList) {
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(feature);
                        if (compareResult != null && compareResult.getSimilar() > SIMILARITY_THRESHOLD_YES) {
                            String id = compareResult.getUserName();
                            String name = Objects.requireNonNull(libFaceMap.get(id)).getName();
                            String relationShip = Objects.requireNonNull(libFaceMap.get(id)).getFaceRelationShip();
                            noticeStr.append("发现").append(relationShip).append(name);
                        } else {
                            // 对未注册人脸进行属性分析
                            notRegNumber++;
                            attributeAnalysis(noticeStr, bgr24, width, height, Arrays.asList(faceInfoList.get(featureList.indexOf(feature))));
                        }
                    }
                    TTSPlayer.getInstance().startSpeaking(noticeStr.append("共").append(faceNumber)
                            .append("张人脸，").append(notRegNumber).append("张未注册").toString());
                }
                updateTime(noticeStr.toString());
            }
            isUpdated = true;
        }
    }
}


//                    if () {
//                        // 处理已注册用户
//
//                            noRegCount = 0;
//                            String id = compareResult.getUserName();
//                            String name = Objects.requireNonNull(libFaceMap.get(id)).getName();
//                            String relationShip = Objects.requireNonNull(libFaceMap.get(id)).getFaceRelationShip();
//                            long time = System.currentTimeMillis();
//                            if (!detectedTimeMap.containsKey(compareResult.getUserName())) {
//                                detectedTimeMap.put(compareResult.getUserName(), time);
//                                // 首次发现进行通知
//                                recentNoticeTime = time;
//                                TTSPlayer.getInstance().startSpeaking("发现您的" + relationShip + name);
//
//                            } else {
//                                // 时间间隔需大于给定值才通知,防止频繁通知
//                                if ((time - detectedTimeMap.get(id)) > timeInterval) {
//                                    recentNoticeTime = time;
//                                    TTSPlayer.getInstance().startSpeaking("发现您的" + relationShip + name);
//
//                                    // TODO 计算距离
//
//                                    // 更新时间
//                                    detectedTimeMap.put(compareResult.getUserName(), time);
//                                }
//                            }
//                        } else {
//                            // 处理未注册用户,连续5次判断为未注册用户才进行通知(好像只适用单人脸)
//                            noRegCount++;
//                            long time = System.currentTimeMillis();
//                            if (noRegCount >= 5 && (time - recentNoticeTime) > TIME_INTERVAL) {
//                                recentNoticeTime = time;
//                                TTSPlayer.getInstance().startSpeaking("发现未知人脸");

// 处理图片,属性检测(faceInfoList只包含一张人脸)
//                                int faceProcessCode = attrEngine.process(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList, FaceEngine.ASF_AGE | FaceEngine.ASF_GENDER);
//                                if (faceProcessCode == ErrorInfo.MOK) {
//                                    //年龄信息结果
//                                    List<AgeInfo> ageInfoList = new ArrayList<>();
//                                    //性别信息结果
//                                    List<GenderInfo> genderInfoList = new ArrayList<>();
//                                    //获取年龄、性别
//                                    int ageCode = attrEngine.getAge(ageInfoList);
//                                    int genderCode = attrEngine.getGender(genderInfoList);
//
//                                    if (ageCode == ErrorInfo.MOK && genderCode == ErrorInfo.MOK) {
//                                        if (ageInfoList.size() > 0 && genderInfoList.size() > 0 && genderInfoList.size() == ageInfoList.size()) {
//
//                                            StringBuilder stringBuffer = new StringBuilder();
//                                            for (int i = 0; i < ageInfoList.size(); i++) {
//                                                int age = ageInfoList.get(i).getAge();
//                                                String gender;
//                                                if (genderInfoList.get(i).getGender() == GenderInfo.MALE)
//                                                    gender = "男性";
//                                                else if (genderInfoList.get(i).getGender() == GenderInfo.FEMALE)
//                                                    gender = "女性";
//                                                else gender = "未知性别";
//                                                Log.d("FaceDetectHelper", "onExtractFeature: 属性提取成功" + age + gender);
//                                                stringBuffer.append(gender).append("  ").append(age).append("岁");
//                                                long time1 = System.currentTimeMillis();
////                                    if ((time1 - recentNoticeTime) > 5000) {
////                                        recentNoticeTime = time1;
//                                                TTSPlayer.getInstance().startSpeaking(stringBuffer.toString());
////                                    }
//                                            }
//
//                                        }
//                                    }
//                                }
//                            }
//
//                        }
//                    } else {
//
//                        long time = System.currentTimeMillis();
//                        if (noRegCount >= 5 && (time - recentNoticeTime) > TIME_INTERVAL) {
//
//                            TTSPlayer.getInstance().startSpeaking("发现未知人脸");
//                            recentNoticeTime = time;
//
//
//
////                long time = System.currentTimeMillis();
////                if((time - recentNoticeTime) > 3000 ) {
////                    recentNoticeTime = time;
////                    TTSPlayer.getInstance().startSpeaking("请检查人脸库是否为空");
////                }
//                            Log.d(TAG, "featureExtract: 比较结果为空！");
//                        }
//                    }
//                } else if (livenessCode == LivenessInfo.NOT_ALIVE) {
//                    long time = System.currentTimeMillis();
//                    if ((time - recentNoticeTime) > TIME_INTERVAL) {
//                        recentNoticeTime = time;
//                        TTSPlayer.getInstance().startSpeaking("检测到非活体人脸");
//                    }
//                }
//            }


/**
 * 通知类型:
 */
