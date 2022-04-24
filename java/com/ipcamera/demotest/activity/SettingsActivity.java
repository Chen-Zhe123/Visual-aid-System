package com.ipcamera.demotest.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.ipcamera.demotest.common.Constants;
import com.ipcamera.demotest.common.SharedPreferenceUtil;
import com.ipcamera.demotest.database.localdb.FaceServer;
import com.ipcamera.demotest.dialog.DetectDegreeDialog;
import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.RuntimeABI;
import com.ipcamer.demotest.R;
import com.ipcamera.demotest.dialog.SetFilterDialog;
import com.ipcamera.demotest.utils.TTSPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SettingsActivity extends BaseActivity implements DetectDegreeDialog.ModifyDetectDegreeListener, SetFilterDialog.ModifyFilterListener {

    private final Context mContext = SettingsActivity.this;
    private final String TAG = "SettingsActivity";
    DetectDegreeDialog detectDegreeDialog;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    // 在线激活所需的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    boolean libraryExists = true;
    // Demo 所需的动态库文件
    private static final String[] LIBRARIES = new String[]{
            // 人脸相关
            "libarcsoft_face_engine.so",
            "libarcsoft_face.so",
            // 图像库相关
            "libarcsoft_image_util.so",
    };
    private Boolean isActivated;
    private String engineType;
    private DetectFaceOrientPriority detectDegree;
    private int detectDegreeIndex;
    private final String[] engineEntries = {"本地合成", "云端合成"};
    // 默认云端发音人
    public static String cloudPronunciation;
    // 默认本地发音人
    public static String localPronunciation;

    // 云端发音人列表
    private String[] cloudVoicersEntries;
    private String[] cloudVoicersValue;

    // 本地发音人列表
    private String[] localVoicersEntries;
    private String[] localVoicersValue;

    private TextView activateEngineText;
    private TextView detectDegreeText;
    private TextView voiceSyntheticWayText;
    private TextView pronunciationText;
    private TextView voiceSpeedText;
    private TextView voiceTonesText;
    private TextView voiceVolumeText;
    private TextView audioStreamTypeText;
    // 活体检测设置
    private Switch switchLivenessDetect;
    private TextView livenessState;
    // 过滤条件值
    TextView filterValue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        libraryExists = checkSoFile(LIBRARIES);
        ApplicationInfo applicationInfo = getApplicationInfo();
        Log.i(TAG, "onCreate: " + applicationInfo.nativeLibraryDir);
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
        } else {
            VersionInfo versionInfo = new VersionInfo();
            int code = FaceEngine.getVersion(versionInfo);
            Log.i(TAG, "onCreate: getVersion, code is: " + code + ", versionInfo is: " + versionInfo);
        }
        // 云端发音人名称列表
        cloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
        cloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);

        // 本地发音人名称列表
        localVoicersEntries = getResources().getStringArray(R.array.voicer_local_entries);
        localVoicersValue = getResources().getStringArray(R.array.voicer_local_values);

        isActivated = SharedPreferenceUtil.getEngineActivateState(mContext);
        engineType = SharedPreferenceUtil.getVoiceSyntheticWay(mContext);

        detectDegree = SharedPreferenceUtil.getFtOrient(mContext);
        detectDegreeIndex = detectDegree.getPriority();
        Log.d(TAG, "onCreate: index" + detectDegreeIndex);

        localPronunciation = localVoicersValue[SharedPreferenceUtil.getLocalPronunciation(mContext)];
        cloudPronunciation = cloudVoicersValue[SharedPreferenceUtil.getCloudPronunciation(mContext)];
        findView();
    }

    private void findView() {
        activateEngineText = findViewById(R.id.active_face_detect_engine_value);
        if (isActivated) activateEngineText.setText("已激活");
        else activateEngineText.setText("未激活");

        detectDegreeText = findViewById(R.id.detect_degree_value);
        switch (detectDegreeIndex) {
            case 1:
                detectDegreeText.setText("仅检测0度");
                break;
            case 2:
                detectDegreeText.setText("仅检测90度");
                break;
            case 3:
                detectDegreeText.setText("仅检测270度");
                break;
            case 4:
                detectDegreeText.setText("仅检测180度");
                break;
            case 5:
                detectDegreeText.setText("全方位人脸检测");
                break;
        }

        // 活体检测开关
        switchLivenessDetect = findViewById(R.id.set_liveness_detect);
        switchLivenessDetect.setChecked(SharedPreferenceUtil.getLivenessDetectState(mContext));
        switchLivenessDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            livenessState.setText(isChecked ? "开" : "关");
            SharedPreferenceUtil.saveLivenessDetectState(mContext, isChecked);
        });
        livenessState = findViewById(R.id.liveness_detect_state);
        livenessState.setText(SharedPreferenceUtil.getLivenessDetectState(mContext) ? "开" : "关");

        // 设置人脸识别过滤条件
        filterValue = findViewById(R.id.filter_value);
        int filter = SharedPreferenceUtil.getFilter(mContext);
        if (filter == Constants.NO_FILTER) {
            filterValue.setText("无过滤条件");
        } else {
            filterValue.setVisibility(View.GONE);
        }

        // 人脸识别语音提醒间隔
        int interval = SharedPreferenceUtil.getVoiceReminderInterval(mContext);
        TextView intervalValue = findViewById(R.id.voice_reminder_interval_value);
        intervalValue.setText(interval + "秒");
        ImageView subtract = findViewById(R.id.subtract_interval);
        subtract.setOnClickListener(v -> {
            int nowInterval = SharedPreferenceUtil.getVoiceReminderInterval(mContext);
            if(nowInterval == 0) {
                showToast("时间间隔不可小于0");
            } else if (nowInterval == 5) {
                showToast("时间间隔不建议小于5秒");
                SharedPreferenceUtil.saveVoiceReminderInterval(mContext, nowInterval - 5);
                intervalValue.setText((nowInterval - 5) + "秒");
            } else {
                SharedPreferenceUtil.saveVoiceReminderInterval(mContext, nowInterval - 5);
                intervalValue.setText((nowInterval - 5) + "秒");
            }
        });
        ImageView add = findViewById(R.id.add_interval);
        add.setOnClickListener(v -> {
            int nowInterval = SharedPreferenceUtil.getVoiceReminderInterval(mContext);
            SharedPreferenceUtil.saveVoiceReminderInterval(mContext, nowInterval + 5);
            intervalValue.setText((nowInterval + 5) + "秒");
        });

        voiceSyntheticWayText = findViewById(R.id.voice_synthetic_way_value);
        if (engineType.equals("local")) voiceSyntheticWayText.setText("本地合成");
        else voiceSyntheticWayText.setText("云端合成");

        pronunciationText = findViewById(R.id.pronunciation_value);
        if (engineType.equals("local")) pronunciationText.setText(localPronunciation);
        else pronunciationText.setText(cloudPronunciation);

        voiceSpeedText = findViewById(R.id.voice_speed_value);
        voiceTonesText = findViewById(R.id.voice_tones_value);
        voiceVolumeText = findViewById(R.id.voice_volume_value);
        audioStreamTypeText = findViewById(R.id.audio_stream_type_value);

    }

    // 请求权限后的回调
    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                active_face_detect_engine(null);
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    /**
     * 检查能否找到动态链接库，如果找不到，请修改工程配置
     *
     * @param libraries 需要的动态链接库
     * @return 动态库是否存在
     */
    private boolean checkSoFile(String[] libraries) {
        File dir = new File(getApplicationInfo().nativeLibraryDir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }
        List<String> libraryNameList = new ArrayList<>();
        for (File file : files) {
            libraryNameList.add(file.getName());
        }
        boolean exists = true;
        for (String library : libraries) {
            exists &= libraryNameList.contains(library);
        }
        return exists;
    }

    /**
     * 激活引擎
     *
     * @param view
     */
    public void active_face_detect_engine(View view) {
        // 检查so库
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
            return;
        }
        // 检查权限
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        // 猜测：在点击事件处理完之前禁止了二次点击
        if (view != null) {
            view.setClickable(false);
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) {
                RuntimeABI runtimeABI = FaceEngine.getRuntimeABI();
                Log.i(TAG, "subscribe: getRuntimeABI() " + runtimeABI);

                long start = System.currentTimeMillis();
                int activeCode = FaceEngine.activeOnline(mContext, Constants.APP_ID, Constants.SDK_KEY);
                Log.i(TAG, "subscribe cost: " + (System.currentTimeMillis() - start));
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            showToast(getString(R.string.active_success));
                            SharedPreferenceUtil.saveEngineActivateState(mContext, true);
                            FaceServer.getInstance().init(mContext);
                            activateEngineText.setText("已激活");

                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            showToast(getString(R.string.already_activated));
                        } else {
                            showToast(getString(R.string.active_failed, activeCode));
                            SharedPreferenceUtil.saveEngineActivateState(mContext, false);
                            activateEngineText.setText("未激活");
                        }

                        if (view != null) {
                            view.setClickable(true);
                        }
                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = FaceEngine.getActiveFileInfo(mContext, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast(e.getMessage());
                        if (view != null) {
                            view.setClickable(true);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 人脸识别角度设置
     *
     * @param view
     */
    public void set_detect_degree(View view) {
        if (detectDegreeDialog == null) {
            detectDegreeDialog = new DetectDegreeDialog();
            detectDegreeDialog.setListener(this);
        }
        if (detectDegreeDialog.isAdded()) {
            detectDegreeDialog.dismiss();
        }
        detectDegreeDialog.show(getSupportFragmentManager(), DetectDegreeDialog.class.getSimpleName());
    }

    public void set_filter(View view) {
        SetFilterDialog.getInstance(mContext).setListener(this);
        SetFilterDialog.getInstance(mContext).show();
    }

    @Override
    public void modifyFilterCallback(Boolean isNoFilter) {
        if (isNoFilter) {
            filterValue.setVisibility(View.VISIBLE);
            filterValue.setText("无过滤条件");
        } else {
            filterValue.setVisibility(View.GONE);
        }
    }

    public void set_voice_synthetic_way(View view) {
        engineType = SharedPreferenceUtil.getVoiceSyntheticWay(mContext);
        int engineIndex;
        if (engineType.equals("local")) engineIndex = 0;
        else engineIndex = 1;
        new AlertDialog.Builder(this).setTitle("选择语音合成引擎")
                .setSingleChoiceItems(engineEntries,// 单选框有几项,各是什么名字
                        engineIndex, // 默认的选项
                        new DialogInterface.OnClickListener() { // 点击单选框后的处理
                            public void onClick(DialogInterface dialog,
                                                int which) { // 点击了哪一项
                                if (which == 0) {
                                    // 保存所选的引擎
                                    SharedPreferenceUtil.saveVoiceSyntheticWay(mContext, "local");
                                    // 同步显示
                                    voiceSyntheticWayText.setText(engineEntries[0]);
                                } else if (which == 1) {
                                    // 保存所选的引擎
                                    SharedPreferenceUtil.saveVoiceSyntheticWay(mContext, "cloud");
                                    // 同步显示
                                    voiceSyntheticWayText.setText(engineEntries[1]);
                                }
                                dialog.dismiss();
                            }
                        }).show();
    }


    public void voice_reminder_interval(View view) {

        // TODO 滚轮数据选择器

    }

    /**
     * 发音人选择。
     */
    public void set_pronunciation(View view) {
        // 根据合成方式展示不同的对话框(云端发音人远多于本地发音人)
        String voiceSyntheticWay = SharedPreferenceUtil.getVoiceSyntheticWay(mContext);
        switch (voiceSyntheticWay) {
            // 选择本地合成
            case "local":
                // 获取保存的发音人
                int defaultIndex = SharedPreferenceUtil.getLocalPronunciation(mContext);
                new AlertDialog.Builder(this).setTitle("选择本地发音人")
                        .setSingleChoiceItems(localVoicersEntries, // 单选框有几项,各是什么名字
                                defaultIndex, // 默认的选项
                                new DialogInterface.OnClickListener() { // 点击单选框后的处理
                                    public void onClick(DialogInterface dialog,
                                                        int which) { // 点击了哪一项
                                        // 保存所选的发音人
                                        SharedPreferenceUtil.saveLocalPronunciation(mContext, which);
                                        // 同步显示
                                        pronunciationText.setText(localVoicersValue[which]);
                                        dialog.dismiss();
                                    }
                                }).show();
                break;

            // 选择在线合成
            case "cloud":
                // 获取保存的发音人
                int defaultIndex1 = SharedPreferenceUtil.getCloudPronunciation(mContext);
                new AlertDialog.Builder(this).setTitle("选择云端发音人")
                        .setSingleChoiceItems(cloudVoicersEntries, // 单选框有几项,各是什么名字
                                defaultIndex1, // 默认的选项
                                new DialogInterface.OnClickListener() { // 点击单选框后的处理
                                    public void onClick(DialogInterface dialog,
                                                        int which) { // 点击了哪一项
                                        // 保存所选的发音人
                                        SharedPreferenceUtil.saveCloudPronunciation(mContext, which);
                                        // 同步显示
                                        pronunciationText.setText(cloudVoicersValue[which]);
                                        dialog.dismiss();
                                    }
                                }).show();
                break;
            default:
                break;
        }

    }

    public void set_voice_speed(View view) {

    }

    public void set_voice_tones(View view) {
    }

    public void set_voice_volume(View view) {
    }

    public void set_audio_stream_type(View view) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TTSPlayer.getInstance().init(mContext);
    }

    @Override
    protected void onStop() {
        super.onStop();
        TTSPlayer.getInstance().init(mContext);
    }

    @Override
    public void modifyCallback(int index) {
        switch (index) {
            case 2:
                detectDegreeText.setText("仅检测90度");
                break;
            case 3:
                detectDegreeText.setText("仅检测270度");
                break;
            case 4:
                detectDegreeText.setText("仅检测180度");
                break;
            case 5:
                detectDegreeText.setText("全方位人脸检测");
                break;
            case 1:
            default:
                detectDegreeText.setText("仅检测0度");
                break;
        }

    }
}
