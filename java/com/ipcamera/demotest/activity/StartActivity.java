package com.ipcamera.demotest.activity;


import com.ipcamer.demotest.R;
import com.ipcamera.demotest.service.ConnectService;
import com.ipcamera.demotest.utils.P;

import vstc2.nativecaller.NativeCaller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class StartActivity extends Activity {
    private static final String LOG_TAG = "StartActivity";
    private MyHandler mHandler = new MyHandler();

    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            Intent in = new Intent(StartActivity.this, MainInterfaceActivity.class);
            startActivity(in);
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        P.log("StartActivity:onCreate()方法执行");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.start);
        // 开启服务
        Intent intent = new Intent();
        intent.setClass(StartActivity.this, ConnectService.class);
        startService(intent);
        new Thread(() -> {
            try {
                // 初始化服务器
                NativeCaller.PPPPInitialOther("ADCBBFAOPPJAHGJGBBGLFLAGDBJJHNJGGMBFBKHIBBNKOKLDHOBHCBOEHOKJJJKJBPMFLGCPPJMJAPDOIPNL");
                Thread.sleep(2000);
                Message msg = new Message();
                mHandler.sendMessage(msg);
                Log.e("vst", "path" + getApplicationContext().getFilesDir().getAbsolutePath());
                NativeCaller.SetAPPDataPath(getApplicationContext().getFilesDir().getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            return true;
        return super.onKeyDown(keyCode, event);
    }

}
