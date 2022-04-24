package com.ipcamera.demotest.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.ipcamera.demotest.common.Constants;


/**
 * store package Name
 *
 * @author Administrator
 */
public class SharedPreferenceUtil {

    private static SharedPreferences prefer;
    public static final String STR_CAMERA_SYSTEMFIRM="system_firm";

    private final static String SETTINGS = "settings";
    private static final String APP_NAME = "ArcFaceDemo";
    private static final String TRACKED_FACE_COUNT = "trackedFaceCount";

    // 保存是否开启活体检测活体
    public static void saveLivenessDetectState(Context context,Boolean isOpened){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putBoolean(Constants.LIVENESS_DETECT_STATE,isOpened).apply();
    }

    public static Boolean getLivenessDetectState(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getBoolean(Constants.LIVENESS_DETECT_STATE, true);
    }


    public static boolean setTrackedFaceCount(Context context, int trackedFaceCount) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.edit()
                .putInt(TRACKED_FACE_COUNT, trackedFaceCount)
                .commit();
    }

    public static int getTrackedFaceCount(Context context) {
        if (context == null) {
            return 0;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(TRACKED_FACE_COUNT, 0);
    }

    // 保存人脸识别引擎的激活状态
    public static void saveEngineActivateState(Context context,Boolean isActivated){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putBoolean(Constants.ENGINE_ACTIVATE_STATE,isActivated).apply();
    }

    public static Boolean getEngineActivateState(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
       return prefer.getBoolean(Constants.ENGINE_ACTIVATE_STATE, false);
    }

    // 保存人脸探测角度
    public static boolean setFtOrient(Context context, DetectFaceOrientPriority ftOrient) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return sharedPreferences.edit()
                .putString(Constants.FACE_DETECT_DEGREE, ftOrient.name())
                .commit();
    }

    public static DetectFaceOrientPriority getFtOrient(Context context) {
        if (context == null) {
            return DetectFaceOrientPriority.ASF_OP_270_ONLY;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return DetectFaceOrientPriority.valueOf(sharedPreferences.getString(Constants.FACE_DETECT_DEGREE, DetectFaceOrientPriority.ASF_OP_270_ONLY.name()));
    }

    // 保存人脸识别过滤条件
    public static void saveFilter(Context context,int filter){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putInt(Constants.RECOGNITION_FILTER, filter).apply();
    }

    public static int getFilter(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getInt(Constants.RECOGNITION_FILTER, 0);
    }


    // 保存同一人脸语音提醒时间间隔(单位:秒)
    public static void saveVoiceReminderInterval(Context context,int interval){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putInt(Constants.VOICE_REMIND_INTERVAL, interval).apply();
    }

    public static int getVoiceReminderInterval(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getInt(Constants.VOICE_REMIND_INTERVAL, 0);
    }

    // 保存TTS引擎类型(cloud/local)
    public static void saveVoiceSyntheticWay(Context context,String engineType){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putString(Constants.VOICE_SYNTHETIC_WAY, engineType).apply();
    }

    public static String getVoiceSyntheticWay(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getString(Constants.VOICE_SYNTHETIC_WAY, "local");
    }

    // 保存当前发音人(本地)
    public static void saveLocalPronunciation(Context context,int pronunciationIndex){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putInt(Constants.LOCAL_PRONUNCIATION, pronunciationIndex).apply();
    }

    public static int getLocalPronunciation(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getInt(Constants.LOCAL_PRONUNCIATION, 0);
    }

    // 保存当前发音人(云端)
    public static void saveCloudPronunciation(Context context,int pronunciationIndex){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putInt(Constants.CLOUD_PRONUNCIATION, pronunciationIndex).apply();
    }

    public static int getCloudPronunciation(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getInt(Constants.CLOUD_PRONUNCIATION, 0);
    }

    // 保存语速
    public static void saveVoiceSpeed(Context context,int speed){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putInt(Constants.VOICE_SPEED, speed).apply();
    }

    public static String getVoiceSpeed(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getString(Constants.VOICE_SPEED, "50");
    }
    // 保存语调
    public static void saveVoiceTones(Context context,String speed){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putString(Constants.VOICE_TONES, speed).apply();
    }

    public static String getVoiceTones(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getString(Constants.VOICE_TONES, "50");
    }
    // 保存音量
    public static void saveVoiceVolume(Context context,String volume){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putString(Constants.VOICE_VOLUME, volume).apply();
    }

    public static String getVoiceVolume(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getString(Constants.VOICE_VOLUME, "50");
    }
    // 保存音频流类型
    public static void saveAudioStreamType(Context context,String streamType){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        prefer.edit().putString(Constants.AUDIO_STREAM_TYPE, streamType).apply();
    }

    public static String getAudioStreamType(Context context){
        prefer = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        return prefer.getString(Constants.AUDIO_STREAM_TYPE, "3");
    }






    //保存设备 的信息
    public static void saveDeviceInformation(Context context, String uid, String informationType, String information) {
        prefer = context.getSharedPreferences(uid, Context.MODE_PRIVATE);
        prefer.edit().putString(informationType, information).apply();
    }

    //获取设备 的信息
    public static String getDeviceInformation(Context context, String uid, String informationType) {
        prefer = context.getSharedPreferences(uid, Context.MODE_PRIVATE);
        String information = prefer.getString(informationType, "");
        return information;
    }

    //保存设备版本 的信息
    public static void saveSystemVer(Context context, String did, String ver) {
        prefer = context.getSharedPreferences(
                STR_CAMERA_SYSTEMFIRM, Context.MODE_PRIVATE);
        prefer.edit().putString(did, ver).apply();
    }

    // 获取系统版本
    public static String getSystemVer(Context context, String did) {
        prefer = context.getSharedPreferences(
                STR_CAMERA_SYSTEMFIRM, Context.MODE_PRIVATE);
        String path = prefer.getString(did, "0");
        return path;
    }
}
