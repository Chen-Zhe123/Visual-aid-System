package com.ipcamera.demotest.common;

public class Constants {

    public static final String APP_ID = "9u56P7R7WN8NyVxczH1debL5U2f5JfY123x7eY94YPay";
    public static final String SDK_KEY = "77gaCWQL3uH2oiX4iQC9EHJZ6CgDk8pjvrX6B2gmj6Ce";


    /**
     * 人脸识别服务开启状态
     */
    public static final Boolean SERVICE_STARTED = true;
    public static final Boolean SERVICE_CLOSED= false;

    /**
     * IR预览数据相对于RGB预览数据的横向偏移量，注意：是预览数据，一般的摄像头的预览数据都是 width > height
     */
    public static final int HORIZONTAL_OFFSET = 0;
    /**
     * IR预览数据相对于RGB预览数据的纵向偏移量，注意：是预览数据，一般的摄像头的预览数据都是 width > height
     */
    public static final int VERTICAL_OFFSET = 0;

    /**
     * 有关设置的常量
     */
    public final static String ENGINE_ACTIVATE_STATE = "EngineActivateState";
    public final static String FACE_DETECT_DEGREE = "faceDetectDegree";
    public final static String RECOGNITION_FILTER = "recognitionFilter";
    public final static int NO_FILTER = 0;
    public final static int ONLY_HIGH_MEDIUM = 1;
    public final static int ONLY_HIGH = 2;
    public final static String VOICE_REMIND_INTERVAL = "voice_remind_interval";
    public final static String VOICE_SYNTHETIC_WAY = "voiceSyntheticWay";
    public final static String LOCAL_PRONUNCIATION = "localPronunciation";
    public final static String CLOUD_PRONUNCIATION = "cloudPronunciation";
    public final static String VOICE_SPEED = "voiceSpeed";
    public final static String VOICE_TONES = "voiceTones";
    public final static String VOICE_VOLUME = "voiceVolume";
    public final static String AUDIO_STREAM_TYPE = "audioStreamType";
    public final static String LIVENESS_DETECT_STATE = "liveness_detect_state";


    /**
     * 人脸资料卡的常量
     */
    public final static String HIGH_DETECT_PRIORITY = "高";
    public final static String MEDIUM_DETECT_PRIORITY = "中";
    public final static String LOW_DETECT_PRIORITY = "低";


}
