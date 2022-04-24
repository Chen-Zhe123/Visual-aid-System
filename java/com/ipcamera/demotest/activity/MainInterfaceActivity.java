package com.ipcamera.demotest.activity;

import vstc2.nativecaller.NativeCaller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamera.demotest.common.Constants;
import com.ipcamer.demotest.R;
import com.ipcamera.demotest.service.ConnectService;
import com.ipcamera.demotest.service.ConnectService.AddCameraInterface;
import com.ipcamera.demotest.service.ConnectService.CallBackMessageInterface;
import com.ipcamera.demotest.service.ConnectService.IpcamClientInterface;
import com.ipcamera.demotest.service.FaceDetectService;
import com.ipcamera.demotest.common.ContentCommon;
import com.ipcamera.demotest.common.SharedPreferenceUtil;
import com.ipcamera.demotest.service.LiveStreamState;
import com.ipcamera.demotest.servicetest.TestFaceDetectService;
import com.ipcamera.demotest.utils.MyStringUtils;
import com.ipcamera.demotest.utils.P;
import com.ipcamera.demotest.utils.SystemValue;
import com.ipcamera.demotest.utils.VuidUtils;
import com.ipcamera.demotest.utils.TTSPlayer;

public class MainInterfaceActivity extends Activity implements OnClickListener, AddCameraInterface
        , OnItemSelectedListener, IpcamClientInterface, CallBackMessageInterface {
    private EditText userEdit = null;
    private EditText pwdEdit = null;
    private EditText didEdit = null;
    private TextView textView_top_show = null;
    private Button connect;
    private Button faceDetectButton;
    private Button ttsButton;
    private Button faceManageButton;
    private Button settingsButton;
    private int option = ContentCommon.INVALID_OPTION;
    private int CameraType = ContentCommon.CAMERA_TYPE_MJPEG;
    private ProgressDialog progressdlg = null;
    private MyBroadCast receiver;
    private WifiManager manager = null;
    private ProgressBar progressBar = null;
    private static final String STR_DID = "did";
    private static final String STR_MSG_PARAM = "msgparam";
    private boolean blagg = false;
    private Intent intentbrod = null;
    private Button button_play = null;
    private int tag = 0;
    private RelativeLayout connectFragment;
    private Boolean conFraIsShow = false;

    // my addition
    private final Context mContext = MainInterfaceActivity.this;

    public void settings_button(View view) {
        startActivity(new Intent(mContext, SettingsActivity.class));
    }

    public void face_library(View view) {
        startActivity(new Intent(mContext, FaceLibraryActivity.class));
    }

    public void launch_web_camera(View view) {
        if (!conFraIsShow) {
            connectFragment.setVisibility(View.VISIBLE);
            conFraIsShow = true;
        } else {
            connectFragment.setVisibility(View.GONE);
            conFraIsShow = false;
        }
    }

    // 人脸识别服务状态
    private Boolean serviceState = Constants.SERVICE_CLOSED;
    private int clickNum = 0;
    private final Handler clickHandler = new Handler();

    /**
     * 单击预览摄像头画面
     * 双击开启人脸识别服务
     */
    public void play_video(View view) {
        clickNum++;
        clickHandler.postDelayed(() -> {
            if (clickNum == 1) {
                if (tag == 1) {
                    startActivity(new Intent(MainInterfaceActivity.this, PlayVideoActivity.class));
                }else{
                    TTSPlayer.getInstance().startSpeaking("请先连接摄像头");
                }
            }else if(clickNum==2){
                P.log(serviceState.toString());
                // TODO 这种获取服务状态的方式不安全
                if(!serviceState) {
                    if (tag == 1) {
                        P.log("开启服务");
                        Intent startIntent = new Intent(mContext, FaceDetectService.class);
//                        Intent startIntent = new Intent(mContext, TestFaceDetectService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(startIntent);
                        }else{
                            startService(startIntent);
                        }
                        serviceState = Constants.SERVICE_STARTED;
                        TTSPlayer.getInstance().startSpeaking("已开启人脸识别服务");
                    } else {
                        TTSPlayer.getInstance().startSpeaking("请先连接摄像头");
                    }
                }else {
                    P.log("关闭服务");
                    // TODO 关闭服务,释放资源
                    serviceState = Constants.SERVICE_CLOSED;
                    Intent stopIntent = new Intent(mContext,FaceDetectService.class);
//                    Intent stopIntent = new Intent(mContext,TestFaceDetectService.class);
                    stopService(stopIntent);
                    TTSPlayer.getInstance().startSpeaking("已关闭人脸识别服务");
                }
            }
            // 防止handler引起的内存泄漏
            clickHandler.removeCallbacksAndMessages(null);
            clickNum = 0;
        },300);
    }

    public void connect_camera(View view) {
//        Toast.makeText(MainInterfaceActivity.this, "您没有操作权限", Toast.LENGTH_SHORT).show();
        if (tag == 1) {
            Toast.makeText(MainInterfaceActivity.this, "设备已经是在线状态了", Toast.LENGTH_SHORT).show();
        } else if (tag == 2) {
            Toast.makeText(MainInterfaceActivity.this, "设备不在线", Toast.LENGTH_SHORT).show();
        } else {
            connect();
        }
    }

    // 模拟Dialog
    public void blank_space(View view) {
        if (conFraIsShow) {
            connectFragment.setVisibility(View.GONE);
            conFraIsShow = false;
        }
    }


    private class MyBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {

            MainInterfaceActivity.this.finish();
            Log.d("ip", "MainInterfaceActivity.this.finish()");
        }

    }

    class StartPPPPThread implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
                if (VuidUtils.isVuid(SystemValue.deviceId)) {
                    P.log("VuidUtils.isVuid(SystemValue.deviceId)");
                    int vuidStatus = NativeCaller.StartVUID("0", SystemValue.devicePass, 1, "", "", 0, SystemValue.deviceId, 0);
                    Log.e("vst", "vuidStatus" + vuidStatus);
                    if (vuidStatus == -2) {
                        // TODO: 2019-11-25 VUID  无效
                        Bundle bd = new Bundle();
                        Message msg = mPPPPMsgHandler.obtainMessage();
                        msg.what = ContentCommon.PPPP_MSG_VSNET_NOTIFY_TYPE_VUIDSTATUS;
                        bd.putInt(STR_MSG_PARAM, -2);
                        bd.putString(STR_DID, SystemValue.deviceId);
                        msg.setData(bd);
                        mPPPPMsgHandler.sendMessage(msg);
                    }
                } else {
                    P.log("startCameraPPPP();");
                    startCameraPPPP();
                }
            } catch (Exception e) {

            }
        }
    }

    // 开启p2p连接
    private void startCameraPPPP() {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }

        if (SystemValue.deviceId.toLowerCase().startsWith("vsta")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EFGFFBBOKAIEGHJAEDHJFEEOHMNGDCNJCDFKAKHLEBJHKEKMCAFCDLLLHAOCJPPMBHMNOMCJKGJEBGGHJHIOMFBDNPKNFEGCEGCBGCALMFOHBCGMFK", 0);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstd")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "HZLXSXIALKHYEIEJHUASLMHWEESUEKAUIHPHSWAOSTEMENSQPDLRLNPAPEPGEPERIBLQLKHXELEHHULOEGIAEEHYEIEK-$$", 1);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstf")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "HZLXEJIALKHYATPCHULNSVLMEELSHWIHPFIBAOHXIDICSQEHENEKPAARSTELERPDLNEPLKEILPHUHXHZEJEEEHEGEM-$$", 1);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("vste")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBAKKIOGNJHEGHMFEEDGLNOHJMPHAFPBEDLADILKEKPDLBDDNPOHKKCIFKJBNNNKLCPPPNDBFDL", 0);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("pisr")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EFGFFBBOKAIEGHJAEDHJFEEOHMNGDCNJCDFKAKHLEBJHKEKMCAFCDLLLHAOCJPPMBHMNOMCJKGJEBGGHJHIOMFBDNPKNFEGCEGCBGCALMFOHBCGMFK", 0);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstg")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBOKCIGGFJPECHIFNEBGJNLHOMIHEFJBADPAGJELNKJDKANCBPJGHLAIALAADMDKPDGOENEBECCIK:vstarcam2018", 0);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("vsth")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBLKGJIGEJLEKGOFMEDHAMHHJNAGGFABMCOBGJOLHLJDFAFCPPHGILKIKLMANNHKEDKOINIBNCPJOMK:vstarcam2018", 0);
        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstb") || SystemValue.deviceId.toLowerCase().startsWith("vstc")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "ADCBBFAOPPJAHGJGBBGLFLAGDBJJHNJGGMBFBKHIBBNKOKLDHOBHCBOEHOKJJJKJBPMFLGCPPJMJAPDOIPNL", 0);
        }

        else if (SystemValue.deviceId.toLowerCase().startsWith("vstj")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBLKGJIGEJNEOHEFBEIGANCHHMBHIFEAHDEAMJCKCKJDJAFDDPPHLKJIHLMBENHKDCHPHNJBODA:vstarcam2019", 0);

        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstk")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EBGDEJBJKGJFGJJBEFHPFCEKHGNMHNNMHMFFBICPAJJNLDLLDHACCNONGLLPJGLKANMJLDDHODMEBOCIJEMA:vstarcam2019", 0);

        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstm")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EBGEEOBOKHJNHGJGEAGAEPEPHDMGHINBGIECBBCBBJIKLKLCCDBBCFODHLKLJJKPBOMELECKPNMNAICEJCNNJH:vstarcam2019", 0);

        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstn")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBBKBIFGAIAFGHDFLFJGJNIGEMOHFFPAMDMAAIIKBKNCDBDDMOGHLKCJCKFBFMPLMCBPEMG:vstarcam2019", 0);

        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstl")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBLKGJIGEJIEIGNFPEEHGNMHPNBGOFIBECEBLJDLMLGDKAPCNPFGOLLJFLJAOMKLBDFOGMAAFCJJPNFJP:vstarcam2019", 0);

        } else if (SystemValue.deviceId.toLowerCase().startsWith("vstp")) {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "EEGDFHBLKGJIGEJLEIGJFLENHLNBHCNMGAFGBNCOAIJMLKKODNALCCPKGBLHJLLHAHMBKNDFOGNGBDCIJFMB:vstarcam2019", 0);

        } else {
            NativeCaller.StartPPPPExt(SystemValue.deviceId, SystemValue.deviceName,
                    SystemValue.devicePass, 1, "", "", 0);
        }
    }

    private void stopCameraPPPP() {
        NativeCaller.StopPPPP(SystemValue.deviceId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_interface);
        progressdlg = new ProgressDialog(this);
        progressdlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressdlg.setMessage(getString(R.string.searching_tip));
        findView();
        manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        InitParams();

        // SearchResult()/CallBack_SearchVUIDResult() 返回摄像头搜索结果
        ConnectService.setAddCameraInterface(this);
        // CallBackTransferMessage() 返回摄像头参数相关
        ConnectService.setCallBackMessage(this);
        receiver = new MyBroadCast();
        IntentFilter filter = new IntentFilter();
        filter.addAction("finish");
        registerReceiver(receiver, filter);
        intentbrod = new Intent("drop");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        P.log("onKeyDown");
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            MainInterfaceActivity.this.finish();
            return false;
        }
//        System.exit(0);
        return false;
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        // TODO 若视频流数据已终止,
        blagg = true;
    }

    private void InitParams() {
        connect.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        progressdlg.dismiss();
        NativeCaller.StopSearch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        NativeCaller.Free();
        Intent intent = new Intent();
        intent.setClass(this, ConnectService.class);
        stopService(intent);
        tag = 0;
    }

    private void findView() {
        connectFragment = findViewById(R.id.connect_camera_fragment);
        progressBar = findViewById(R.id.main_model_progressBar1);
        textView_top_show = findViewById(R.id.login_textView1);
        button_play = findViewById(R.id.play);
        button_play.setOnClickListener(this);
        faceDetectButton = findViewById(R.id.face_detect);
        faceDetectButton.setOnClickListener(this);
        ttsButton = findViewById(R.id.tts);
        ttsButton.setOnClickListener(this);
        faceManageButton = findViewById(R.id.face_manage);
        faceManageButton.setOnClickListener(this);
        connect = findViewById(R.id.connect);
        userEdit = findViewById(R.id.editUser);
        pwdEdit = findViewById(R.id.editPwd);
        pwdEdit.setText("021112Cz");
        didEdit = findViewById(R.id.editDID);
        settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(this);
    }

    /**
     * 摄像机在线时可以获取一张摄像机当前的画面图
     */
    private void getSnapshot() {
        String msg = "snapshot.cgi?loginuse=admin&loginpas=" + SystemValue.devicePass
                + "&user=admin&pwd=" + SystemValue.devicePass;
        NativeCaller.TransferMessage(SystemValue.deviceId, msg, 1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:

                break;
            default:
                break;
        }
    }




    private void connect() {
        Intent in = new Intent();
        String strUser = userEdit.getText().toString();
        String strPwd = pwdEdit.getText().toString();
        String strDID = didEdit.getText().toString();

        if (strDID.length() == 0) {
            Toast.makeText(MainInterfaceActivity.this,
                    getResources().getString(R.string.input_camera_id), Toast.LENGTH_SHORT).show();
            return;
        }
        if (strUser.length() == 0) {
            Toast.makeText(MainInterfaceActivity.this,
                    getResources().getString(R.string.input_camera_user), Toast.LENGTH_SHORT).show();
            return;
        }
        if (option == ContentCommon.INVALID_OPTION) {
            option = ContentCommon.ADD_CAMERA;
        }
        in.putExtra(ContentCommon.CAMERA_OPTION, option);
        in.putExtra(ContentCommon.STR_CAMERA_ID, strDID);
        in.putExtra(ContentCommon.STR_CAMERA_USER, strUser);
        in.putExtra(ContentCommon.STR_CAMERA_PWD, strPwd);
        in.putExtra(ContentCommon.STR_CAMERA_TYPE, CameraType);
        progressBar.setVisibility(View.VISIBLE);
        SystemValue.deviceName = strUser;
        SystemValue.deviceId = strDID;
        SystemValue.devicePass = strPwd;
        ConnectService.setIpcamClientInterface(this);
        // 初始化视频解码器
        NativeCaller.Init();
        new Thread(new StartPPPPThread()).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            didEdit.setText(scanResult);
        }
    }

    /**
     * ConnectService callback
     **/
    @Override
    public void callBackSearchResultData(int sysver, String strMac,
                                         String strName, String strDeviceID, String strIpAddr, int port) {
        Log.e("MainInterfaceActivity", strDeviceID + strName);

    }

    private PPPPMsgHandler mPPPPMsgHandler = new PPPPMsgHandler();

    private class PPPPMsgHandler extends Handler {
        public void handleMessage(Message msg) {
            Bundle bd = msg.getData();
            int msgParam = bd.getInt(STR_MSG_PARAM);
            int msgType = msg.what;
            Log.i("aaa", "====" + msgType + "--msgParam:" + msgParam);
            String did = bd.getString(STR_DID);
            int resid = R.string.pppp_status_connecting;
            switch (msgType) {
                case ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS:
                    P.log("ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS");
                    switch (msgParam) {
                        case ContentCommon.PPPP_STATUS_CONNECTING://0
                            resid = R.string.pppp_status_connecting;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.VISIBLE);
                            tag = 2;
                            break;
                        case ContentCommon.PPPP_STATUS_CONNECT_FAILED://3
                            resid = R.string.pppp_status_connect_failed;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_DISCONNECT://4
                            resid = R.string.pppp_status_disconnect;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_INITIALING://1
                            resid = R.string.pppp_status_initialing;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.VISIBLE);
                            tag = 2;
                            break;
                        case ContentCommon.PPPP_STATUS_INVALID_ID://5
                            resid = R.string.pppp_status_invalid_id;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_ON_LINE://2 在线状态
                            resid = R.string.pppp_status_online;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            //摄像机在线之后读取摄像机类型
                            String cmd = "get_status.cgi?loginuse=admin&loginpas=" + SystemValue.devicePass
                                    + "&user=admin&pwd=" + SystemValue.devicePass;
                            NativeCaller.TransferMessage(did, cmd, 1);
                            tag = 1;
                            break;
                        case ContentCommon.PPPP_STATUS_DEVICE_NOT_ON_LINE://6
                            resid = R.string.device_not_on_line;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_CONNECT_TIMEOUT://7
                            resid = R.string.pppp_status_connect_timeout;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_CONNECT_ERRER://8
                            resid = R.string.pppp_status_pwd_error;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        default:
                            LiveStreamState.isStarted = false;
                            resid = R.string.pppp_status_unknown;
                    }
                    textView_top_show.setText(getResources().getString(resid));
                    if (msgParam == ContentCommon.PPPP_STATUS_ON_LINE) {
                        NativeCaller.PPPPGetSystemParams(did, ContentCommon.MSG_TYPE_GET_PARAMS);
                        NativeCaller.TransferMessage(did,
                                "get_factory_param.cgi?loginuse=admin&loginpas="
                                        + SystemValue.devicePass + "&user=admin&pwd=" + SystemValue.devicePass, 1);// 检测push值
                    }
                    if (msgParam == ContentCommon.PPPP_STATUS_INVALID_ID
                            || msgParam == ContentCommon.PPPP_STATUS_CONNECT_FAILED
                            || msgParam == ContentCommon.PPPP_STATUS_DEVICE_NOT_ON_LINE
                            || msgParam == ContentCommon.PPPP_STATUS_CONNECT_TIMEOUT
                            || msgParam == ContentCommon.PPPP_STATUS_CONNECT_ERRER) {
                        NativeCaller.StopPPPP(did);
                    }
                    break;
                case ContentCommon.PPPP_MSG_TYPE_PPPP_MODE:

                    break;
                case ContentCommon.PPPP_MSG_VSNET_NOTIFY_TYPE_VUIDSTATUS:
                    P.log("ContentCommon.PPPP_MSG_VSNET_NOTIFY_TYPE_VUIDSTATUS");
                    switch (msgParam) {
                        case ContentCommon.PPPP_STATUS_CONNECTING://0
                            resid = R.string.pppp_status_connecting;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.VISIBLE);
                            tag = 2;
                            break;
                        case ContentCommon.PPPP_STATUS_CONNECT_FAILED://3
                            resid = R.string.pppp_status_connect_failed;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_DISCONNECT://4
                            resid = R.string.pppp_status_disconnect;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_INITIALING://1
                            resid = R.string.pppp_status_initialing;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.VISIBLE);
                            tag = 2;
                            break;
                        case ContentCommon.PPPP_STATUS_INVALID_ID://5
                            resid = R.string.pppp_status_invalid_id;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_ON_LINE://2 在线状态
                            resid = R.string.pppp_status_online;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            //摄像机在线之后读取摄像机类型
                            String cmd = "get_status.cgi?loginuse=admin&loginpas=" + SystemValue.devicePass
                                    + "&user=admin&pwd=" + SystemValue.devicePass;
                            NativeCaller.TransferMessage(did, cmd, 1);
                            tag = 1;
                            break;
                        case ContentCommon.PPPP_STATUS_DEVICE_NOT_ON_LINE://6
                            resid = R.string.device_not_on_line;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_CONNECT_TIMEOUT://7
                            resid = R.string.pppp_status_connect_timeout;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_CONNECT_ERRER://8
                            resid = R.string.pppp_status_pwd_error;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_INVALID_VUID:
                            resid = R.string.pppp_status_invalid_id;
                            LiveStreamState.isStarted = false;
                            progressBar.setVisibility(View.GONE);
                            tag = 0;
                            break;
                        case ContentCommon.PPPP_STATUS_ALLOT_VUID:

                            break;

                        default:
                            LiveStreamState.isStarted = false;
                            resid = R.string.pppp_status_unknown;
                    }
                    textView_top_show.setText(getResources().getString(resid));
                    if (msgParam == ContentCommon.PPPP_STATUS_ON_LINE) {
                        NativeCaller.PPPPGetSystemParams(did, ContentCommon.MSG_TYPE_GET_PARAMS);
                        NativeCaller.TransferMessage(did,
                                "get_factory_param.cgi?loginuse=admin&loginpas="
                                        + SystemValue.devicePass + "&user=admin&pwd=" + SystemValue.devicePass, 1);// 检测push值
                    }
                    if (msgParam == ContentCommon.PPPP_STATUS_INVALID_ID
                            || msgParam == ContentCommon.PPPP_STATUS_CONNECT_FAILED
                            || msgParam == ContentCommon.PPPP_STATUS_DEVICE_NOT_ON_LINE
                            || msgParam == ContentCommon.PPPP_STATUS_CONNECT_TIMEOUT
                            || msgParam == ContentCommon.PPPP_STATUS_CONNECT_ERRER) {
                        NativeCaller.StopPPPP(did);
                    }
                    break;

            }

        }
    }

    @Override
    public void BSMsgNotifyData(String did, int type, int param) {
        Log.d("ip", "type:" + type + " param:" + param);
        Bundle bd = new Bundle();
        Message msg = mPPPPMsgHandler.obtainMessage();
        msg.what = type;
        bd.putInt(STR_MSG_PARAM, param);
        bd.putString(STR_DID, did);
        msg.setData(bd);
        mPPPPMsgHandler.sendMessage(msg);
        if (type == ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS) {
            intentbrod.putExtra("ifdrop", param);
            sendBroadcast(intentbrod);
        }

    }

    @Override
    public void BSSnapshotNotify(String did, byte[] bImage, int len) {
        // TODO Auto-generated method stub
        Log.i("ip", "BSSnapshotNotify---len" + len);
    }

    @Override
    public void callBackUserParams(String did, String user1, String pwd1,
                                   String user2, String pwd2, String user3, String pwd3) {
        // TODO Auto-generated method stub

    }

    @Override
    public void CameraStatus(String did, int status) {

    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void CallBackGetStatus(String did, String resultPbuf, int cmd) {
        // TODO Auto-generated method stub
        if (cmd == ContentCommon.CGI_IEGET_STATUS) {
            String cameraType = spitValue(resultPbuf, "upnp_status=");

            String cameraSysver = MyStringUtils.spitValue(resultPbuf, "sys_ver=");
            SharedPreferenceUtil.saveSystemVer(MainInterfaceActivity.this, did, cameraSysver);
            int intType = Integer.parseInt(cameraType);
            int type14 = (int) (intType >> 16) & 1;// 14位 来判断是否报警联动摄像机
            if (intType == 2147483647) {// 特殊值
                type14 = 0;
            }


        }
    }

    private String spitValue(String name, String tag) {
        String[] strs = name.split(";");
        for (int i = 0; i < strs.length; i++) {
            String str1 = strs[i].trim();
            if (str1.startsWith("var")) {
                str1 = str1.substring(4, str1.length());
            }
            if (str1.startsWith(tag)) {
                String result = str1.substring(str1.indexOf("=") + 1);
                return result;
            }
        }
        return -1 + "";
    }

}
