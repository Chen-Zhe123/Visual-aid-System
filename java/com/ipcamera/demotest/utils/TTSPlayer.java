package com.ipcamera.demotest.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.ipcamera.demotest.common.SharedPreferenceUtil;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.ipcamer.demotest.R;
import com.tts.MainActivity1;

/**
 * 经过测试可用
 * 初始化位置
 * 什么时候调用:语音唤醒软件、发出提示音指引用户选择功能、
 * 检测到人脸时(包括未注册)、人脸离开视野时
 * 注册人脸完成时
 */
public class TTSPlayer {
    private static String TAG = "chenzhe";
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 云端发音人
    public static String voicerCloud;
    // 本地发音人
    public static String voicerLocal;
    // 云端发音人列表
    private String[] cloudVoicersEntries;
    private String[] cloudVoicersValue;

    // 本地发音人列表
    private String[] localVoicersEntries;
    private String[] localVoicersValue;
    // 引擎类型
    private String engineType;

    private String voiceSpeed;
    private String voiceTones;
    private String voiceVolume;
    private String audioStreamType;

    private Context mContext;
    private static TTSPlayer instance;

    public synchronized static TTSPlayer getInstance() {
        if (instance == null) {
            instance = new TTSPlayer();
        }
        return instance;
    }

    // 启动App、设置中更改参数后需初始化
    public void init(Context context) {
        mContext = context;
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(mContext, mTtsInitListener);
        // 云端发音人名称列表
        cloudVoicersEntries = mContext.getResources().getStringArray(R.array.voicer_cloud_entries);
        cloudVoicersValue = mContext.getResources().getStringArray(R.array.voicer_cloud_values);
        // 本地发音人名称列表
        localVoicersEntries = mContext.getResources().getStringArray(R.array.voicer_local_entries);
        localVoicersValue = mContext.getResources().getStringArray(R.array.voicer_local_values);

        engineType = SharedPreferenceUtil.getVoiceSyntheticWay(mContext);

        voicerLocal = localVoicersValue[SharedPreferenceUtil.getLocalPronunciation(mContext)];
        voicerCloud = cloudVoicersValue[SharedPreferenceUtil.getCloudPronunciation(mContext)];

        voiceSpeed = SharedPreferenceUtil.getVoiceSpeed(mContext);
        voiceTones = SharedPreferenceUtil.getVoiceTones(mContext);
        voiceVolume = SharedPreferenceUtil.getVoiceVolume(mContext);
        audioStreamType = SharedPreferenceUtil.getAudioStreamType(mContext);
        Log.d("chenzhe", "init: enginetype:"+engineType+"local"+voicerLocal+"cloud"+voicerCloud);
        setParam();
    }

    // 销毁对象、释放资源
    public void unInit(){

    }

    public void startSpeaking(String text) {
        Log.d(TAG, "准备点击： " + System.currentTimeMillis());
        int code = mTts.startSpeaking(text, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            Log.d(TAG, "startSpeaking: 语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
            Log.d(TAG, "开始播放：" + System.currentTimeMillis());
        }

        @Override
        public void onSpeakPaused() {
//            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
//            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {

        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {

        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
//                showTip("播放完成");
            } else {
//                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                Log.d(TAG, "session id =" + sid);
            }

            //实时音频流输出参考
			/*if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
				byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
				Log.e("MscSpeechLog", "buf is =" + buf);
			}*/
        }
    };


    /**
     * 参数设置
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        if (engineType.equals(SpeechConstant.TYPE_CLOUD)) {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerCloud);

        } else if (engineType.equals(SpeechConstant.TYPE_LOCAL)) {
            //设置使用本地引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerLocal);
        }
        //mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY,"1");//支持实时音频流抛出，仅在synthesizeToUri条件下支持
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, voiceSpeed);
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, voiceTones);
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, voiceVolume);
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, audioStreamType);
        //	mTts.setParameter(SpeechConstant.STREAM_TYPE, AudioManager.STREAM_MUSIC+"");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH,
                mContext.getExternalFilesDir("msc").getAbsolutePath() + "/tts.pcm");
    }

    //获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        String type = "tts";
        if (engineType.equals(SpeechConstant.TYPE_XTTS)) {
            type = "xtts";
        }
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        if (engineType.equals(SpeechConstant.TYPE_XTTS)) {
            tempBuffer.append(ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + MainActivity1.voicerXtts + ".jet"));
        } else {
            tempBuffer.append(ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + MainActivity1.voicerLocal + ".jet"));
        }

        return tempBuffer.toString();
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.d(TAG, "初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");

            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };
}

