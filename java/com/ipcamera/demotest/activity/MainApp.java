package com.ipcamera.demotest.activity;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;

import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.database.localdb.FaceServer;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.ipcamera.demotest.service.LiveStreamState;
import com.ipcamera.demotest.utils.P;
import com.ipcamera.demotest.utils.TTSPlayer;

public class MainApp extends Application {
    @Override
    public void onCreate() {
        P.log("MainApp:onCreate()执行");
        init();
        // 应用程序入口处调用,避免手机内存过小,杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
        // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
        // 参数间使用“,”分隔。
        // 设置你申请的应用appid

        // 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误

        StringBuffer param = new StringBuffer();
        param.append("appid=" + "9376a3b4");
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(MainApp.this, param.toString());

        // TODO see
        // 暂时先在这里初始化人脸库
        DBMaster.getLocalDB().init(MainApp.this);
        TTSPlayer.getInstance().init(MainApp.this);

        //本地人脸库初始化
        FaceServer.getInstance().init(this);

        // 初始化视频流状态
        LiveStreamState.isStarted = false;

        super.onCreate();
    }


    private void init() {


        // 打印日志
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated( Activity activity,  Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed( Activity activity) {
                Log.d("Activity Resumed ----- ", activity.getClass().getName());
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped( Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState( Activity activity,  Bundle outState) {

            }

            @Override
            public void onActivityDestroyed( Activity activity) {

            }
        });
    }
}


