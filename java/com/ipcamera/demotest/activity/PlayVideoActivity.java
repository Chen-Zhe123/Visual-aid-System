package com.ipcamera.demotest.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import vstc2.nativecaller.NativeCaller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamer.demotest.R;
import com.ipcamera.demotest.service.ConnectService;
import com.ipcamera.demotest.service.ConnectService.PlayInterface;
import com.ipcamera.demotest.service.LiveStreamState;
import com.ipcamera.demotest.utils.AudioPlayer;
import com.ipcamera.demotest.common.ContentCommon;
import com.ipcamera.demotest.utils.CustomBuffer;
import com.ipcamera.demotest.utils.CustomBufferData;
import com.ipcamera.demotest.utils.CustomBufferHead;

import com.ipcamera.demotest.utils.MyRender;
import com.ipcamera.demotest.utils.SystemValue;
import com.ipcamera.demotest.utils.VideoFramePool;

public class PlayVideoActivity extends Activity implements OnClickListener, PlayInterface, ConnectService.CameraLightInterfaceInterface {

    private static final String LOG_TAG = "PlayVideoActivity";
    private static final int AUDIO_BUFFER_START_CODE = 0xff00ff;
    //surfaceView控件
    private GLSurfaceView playSurface = null;

    //视频数据
    private byte[] videoData = null;
    private int videoDataLen = 0;
    public int nVideoWidths = 0;
    public int nVideoHeights = 0;

    private View progressView = null;
    private boolean bProgress = true;
    private final int BRIGHT = 1;//亮度标志
    private final int CONTRAST = 2;//对比度标志
    private int nResolution = 0;//分辨率值
    private int nBrightness = 0;//亮度值
    private int nContrast = 0;//对比度

    private boolean bInitCameraParam = false;
    private boolean bManualExit = false;
    private TextView textosd = null;//显示用户（设备）名
    private String strName = null;//用户（设备）名
    private String strDID = null;//设备ID
    private View userNameView = null;
    private boolean bDisplayFinished = true;
    private CustomBuffer AudioBuffer = null;
    private AudioPlayer audioPlayer = null;
    private boolean bAudioStart = false;

    private boolean isUpDownPressed = false;
    private ImageView videoViewPortrait;
    private ImageView videoViewStandard;
    //顶部控件声明
    private HorizontalScrollView bottomView;
    private ImageView ptzAudio, ptzDefaultSet, ptzBrightness, ptzContrast, ptzTake_photos, ptzResolutoin;
    private int nStreamCodecType;//分辨率格式

    private PopupWindow mPopupWindowProgress;//进度条控件
    private PopupWindow resolutionPopWindow;//分辨率面板

    //正在控制设备
    private boolean isControlDevice = false;

    private String stqvga = "qvga";
    private String stvga = "vga";
    private String stqvga1 = "qvga1";
    private String stvga1 = "vga1";
    private String stp720 = "p720";
    private String sthigh = "high";
    private String stmiddle = "middle";
    private String stmax = "max";

    //分辨率标识符
    private boolean ismax = false;
    private boolean ishigh = false;
    private boolean isp720 = false;
    private boolean ismiddle = false;
    private boolean isqvga1 = false;
    private boolean isvga1 = false;
    private boolean isqvga = false;
    private boolean isvga = false;

    private Animation showAnim;
    private boolean isTakepic = false;
    private boolean isPictSave = false;
    private boolean isTalking = false;//是否在说话
    private boolean isMcriophone = false;//是否在
    public boolean isH264 = false;//是否是H264格式标志
    public boolean isJpeg = false;
    private boolean isTakeVideo = false;
    private long videotime = 0;// 录每张图片的时间

    private Animation dismissAnim;
    private int timeTag = 0;
    private int timeOne = 0;
    private int timeTwo = 0;
    private ImageButton button_back;
    private BitmapDrawable drawable = null;
    private boolean bAudioRecordStart = false;

    private MyRender myRender;

    private ImageButton lightBtn, sireBtn;

    private int i = 0;//拍照张数标志

    //默认视频参数
    private void defaultVideoParams() {
        nBrightness = 1;
        nContrast = 128;
        NativeCaller.PPPPCameraControl(strDID, 1, 0);
        NativeCaller.PPPPCameraControl(strDID, 2, 128);
        showToast(R.string.ptz_default_vedio_params);
    }

    private void showToast(int i) {
        Toast.makeText(PlayVideoActivity.this, i, Toast.LENGTH_SHORT).show();
    }

    //设置视频可见
    private void setViewVisible() {
        if (bProgress) {
            bProgress = false;
            progressView.setVisibility(View.INVISIBLE);
            userNameView.setVisibility(View.VISIBLE);
            getCameraParams();
        }
    }

    int disPlaywidth;
    private Bitmap mBmp;


    private void getCameraParams() {
        NativeCaller.PPPPGetSystemParams(strDID,
                ContentCommon.MSG_TYPE_GET_CAMERA_PARAMS);
    }

    private MsgHandler msgHandler = new MsgHandler();

    private class MsgHandler extends Handler {

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Log.d("tag", "断线了");
                Toast.makeText(getApplicationContext(),
                        R.string.pppp_status_disconnect, Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
    }

    private VideoFramePool framePool;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getDataFromOther();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.play);
        strName = SystemValue.deviceName;
        strDID = SystemValue.deviceId;
        ConnectService.setCameraLightInterfaceInterface(PlayVideoActivity.this);
        disPlaywidth = getWindowManager().getDefaultDisplay().getWidth();
        findView();
        AudioBuffer = new CustomBuffer();
        audioPlayer = new AudioPlayer(AudioBuffer);
        ConnectService.setPlayInterface(this);
        //确保不能重复start!
        Toast.makeText(PlayVideoActivity.this,"视频流状态:"+LiveStreamState.isStarted,Toast.LENGTH_SHORT).show();
        if(!LiveStreamState.isStarted) {
            NativeCaller.StartPPPPLivestream(strDID, 10, 1);
            LiveStreamState.isStarted = true;
        }

        getCameraParams();
        showAnim = AnimationUtils.loadAnimation(this,
                R.anim.ptz_otherset_anim_show);
        dismissAnim = AnimationUtils.loadAnimation(this,
                R.anim.ptz_otherset_anim_dismiss);

        myRender = new MyRender(playSurface);
        framePool = new VideoFramePool(playSurface, myRender);
        framePool.setFrameRate(15);
        framePool.start();
        playSurface.setRenderer(myRender);
        showBottom();
    }



    protected void setResolution(int Resolution) {
        Log.d("tag", "setResolution resolution:" + Resolution);
        NativeCaller.PPPPCameraControl(strDID, 16, Resolution);
    }

    private void findView() {
        //视频渲染画面控件
        playSurface = findViewById(R.id.my_surfaceview);

        videoViewPortrait = findViewById(R.id.video_view);
        videoViewStandard = findViewById(R.id.vedioview_standard);
        // 环形进度条
        progressView = findViewById(R.id.progressLayout);
        // 显示设备(用户)名称
        userNameView = findViewById(R.id.user_name_layout);
        textosd = findViewById(R.id.user_name_text);
        textosd.setText(strName);
        textosd.setVisibility(View.VISIBLE);
        //底部菜单
        bottomView = (HorizontalScrollView) findViewById(R.id.bottom_view);
        // 获取视频声音按钮
        ptzAudio = findViewById(R.id.ptz_audio);
        // 截图按钮
        ptzTake_photos = findViewById(R.id.ptz_take_photos);
        // 预览视频参数恢复默认值按钮
        ptzDefaultSet = findViewById(R.id.ptz_default_set);
        // 调节亮度按钮
        ptzBrightness = findViewById(R.id.ptz_brightness);
        // 调节对比度按钮
        ptzContrast = findViewById(R.id.ptz_contrast);
        // 调节清晰度按钮
        ptzResolutoin = findViewById(R.id.ptz_resolution);
        // 注册接口(点击事件监听器)
        ptzAudio.setOnClickListener(this);
        ptzTake_photos.setOnClickListener(this);
        ptzBrightness.setOnClickListener(this);
        ptzContrast.setOnClickListener(this);
        ptzResolutoin.setOnClickListener(this);
        ptzDefaultSet.setOnClickListener(this);
    }

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private int mode = NONE;
    private float oldDist;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF start = new PointF();
    private PointF mid = new PointF();
    float mMaxZoom = 2.0f;
    float mMinZoom = 0.3125f;
    float originalScale;
    float baseValue;
    protected Matrix mBaseMatrix = new Matrix();
    protected Matrix mSuppMatrix = new Matrix();
    private Matrix mDisplayMatrix = new Matrix();
    private final float[] mMatrixValues = new float[9];


    protected float getScale(Matrix matrix) {
        return getValue(matrix, Matrix.MSCALE_X);
    }


    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ptz_take_photos://拍照
                dismissBrightAndContrastProgress();
                if (existSdcard()) {// 判断sd卡是否存在
                    //takePicture(mBmp);
                    isTakepic = true;
                } else {
                    showToast(R.string.ptz_takepic_save_fail);
                }
                break;
            case R.id.ptz_audio:
                goAudio();
                break;
            case R.id.ptz_brightness:
                if (mPopupWindowProgress != null
                        && mPopupWindowProgress.isShowing()) {
                    mPopupWindowProgress.dismiss();
                    mPopupWindowProgress = null;
                }
                setBrightOrContrast(BRIGHT);
                break;
            case R.id.ptz_contrast:
                if (mPopupWindowProgress != null
                        && mPopupWindowProgress.isShowing()) {
                    mPopupWindowProgress.dismiss();
                    mPopupWindowProgress = null;
                }
                setBrightOrContrast(CONTRAST);
                break;
            case R.id.ptz_resolution:
                showResolutionPopWindow();
                break;
            case R.id.ptz_resolution_jpeg_qvga:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                nResolution = 1;
                setResolution(nResolution);
                Log.d("tag", "jpeg resolution:" + nResolution + " qvga");
                break;
            case R.id.ptz_resolution_jpeg_vga:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                nResolution = 0;
                setResolution(nResolution);
                Log.d("tag", "jpeg resolution:" + nResolution + " vga");
                break;
            case R.id.ptz_resolution_h264_qvga:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                ismax = false;
                ismiddle = false;
                ishigh = false;
                isp720 = false;
                isqvga1 = true;
                isvga1 = false;
                addReslution(stqvga1, isqvga1);
                nResolution = 5;
                setResolution(nResolution);
                break;
            case R.id.ptz_resolution_h264_vga:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                ismax = false;
                ismiddle = false;
                ishigh = false;
                isp720 = false;
                isqvga1 = false;
                isvga1 = true;
                addReslution(stvga1, isvga1);
                nResolution = 4;
                setResolution(nResolution);

                break;
            case R.id.ptz_resolution_h264_720p:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                ismax = false;
                ismiddle = false;
                ishigh = false;
                isp720 = true;
                isqvga1 = false;
                isvga1 = false;
                addReslution(stp720, isp720);
                nResolution = 3;
                setResolution(nResolution);
                break;
            case R.id.ptz_resolution_h264_middle:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                ismax = false;
                ismiddle = true;
                ishigh = false;
                isp720 = false;
                isqvga1 = false;
                isvga1 = false;
                addReslution(stmiddle, ismiddle);
                nResolution = 2;
                setResolution(nResolution);
                break;
            case R.id.ptz_resolution_h264_high:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                ismax = false;
                ismiddle = false;
                ishigh = true;
                isp720 = false;
                isqvga1 = false;
                isvga1 = false;
                addReslution(sthigh, ishigh);
                nResolution = 1;
                setResolution(nResolution);
                break;
            case R.id.ptz_resolution_h264_max:
                dismissBrightAndContrastProgress();
                resolutionPopWindow.dismiss();
                ismax = true;
                ismiddle = false;
                ishigh = false;
                isp720 = false;
                isqvga1 = false;
                isvga1 = false;
                addReslution(stmax, ismax);
                nResolution = 0;
                setResolution(nResolution);
                break;

            case R.id.ptz_default_set:
                dismissBrightAndContrastProgress();
                defaultVideoParams();
                break;
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPopupWindowProgress != null && mPopupWindowProgress.isShowing()) {
            mPopupWindowProgress.dismiss();
        }
        if (resolutionPopWindow != null && resolutionPopWindow.isShowing()) {
            resolutionPopWindow.dismiss();
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!bProgress) {
                Date date = new Date();
                if (timeTag == 0) {
                    timeOne = date.getSeconds();
                    timeTag = 1;
                    showToast(R.string.main_show_back);
                } else if (timeTag == 1) {
                    timeTwo = date.getSeconds();
                    if (timeTwo - timeOne <= 3) {
                        Intent intent = new Intent("finish");
                        sendBroadcast(intent);
                        PlayVideoActivity.this.finish();
                        timeTag = 0;
                    } else {
                        timeTag = 1;
                        showToast(R.string.main_show_back);
                    }
                }
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!bProgress) {
                showBottom();
            } else {
            }
            framePool.exit();
        }
        return super.onKeyDown(keyCode, event);
    }
    private void dismissBrightAndContrastProgress() {
        if (mPopupWindowProgress != null && mPopupWindowProgress.isShowing()) {
            mPopupWindowProgress.dismiss();
            mPopupWindowProgress = null;
        }
    }

    private void showBottom() {
        if (isUpDownPressed) {
            isUpDownPressed = false;
            bottomView.startAnimation(dismissAnim);
            bottomView.setVisibility(View.GONE);
        } else {
            isUpDownPressed = true;
            bottomView.startAnimation(showAnim);
            bottomView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void LightSireCallBack(String did, String command, String cmd, String siren, String light) {
        if (!did.equals(strDID)) {
            return;
        }

    }

    //判断sd卡是否存在
    private boolean existSdcard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    // 拍照
    private void takePicture(final Bitmap bmp) {
        if (!isPictSave) {
            isPictSave = true;
            new Thread() {
                public void run() {
                    File div = new File(Environment.getExternalStorageDirectory(),
                            "ipcamerademo/takepic");
                    if (!div.exists()) {
                        div.mkdirs();
                    }
                    String strDate = getStrDate();
                    File file = new File(div, strDate + "_" + strDID + "_" + i + ".jpg");

                    Uri uri = null;
                    try {
                        uri = saveImage(PlayVideoActivity.this, bmp, div.getAbsolutePath(), file.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("vst", "pic ext");
                    }
                    Log.e("vst", "pic url" + uri.getPath());
                }
            }.start();
        } else {
            return;
        }
    }


    private Uri saveImage(Context context, Bitmap bitmap, @NonNull String folderName, @NonNull String fileName) throws IOException {
        OutputStream fos;
        File imageFile = null;
        Uri imageUri = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + folderName);
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
            Log.e("vst", "imageUri url" + imageUri.getPath() + imageUri.toString());
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    showToast(R.string.ptz_takepic_ok);
                }
            });
        } else {
            // TODO: 2020/11/18  android <10,可以自定义图片的保存路径
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + folderName;
            imageFile = new File(imagesDir);
            if (!imageFile.exists()) {
                imageFile.mkdir();
            }
            imageFile = new File(imagesDir, fileName + ".png");
            fos = new FileOutputStream(imageFile);
        }

        boolean saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();

        if (imageFile != null)  // pre Q
        {
            MediaScannerConnection.scanFile(context, new String[]{imageFile.toString()}, null, null);
            imageUri = Uri.fromFile(imageFile);
        }
        return imageUri;
    }

    //时间格式
    private String getStrDate() {
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd_HH_mm");
        String strDate = f.format(d);
        return strDate;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void writeFile(String imagePath, ContentValues values, ContentResolver contentResolver, Uri item) {
        try (OutputStream rw = contentResolver.openOutputStream(item, "rw")) {
            // Write data into the pending image.
            Sink sink = Okio.sink(rw);
            BufferedSource buffer = Okio.buffer(Okio.source(new File(imagePath)));
            buffer.readAll(sink);
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            contentResolver.update(item, values, null, null);
            new File(imagePath).delete();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Cursor query = getContentResolver().query(item, null, null, null);
                if (query != null) {
                    int count = query.getCount();
                    Log.e("veepai", "writeFile result :" + count);
                    query.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //监听
    private void StartAudio() {
        synchronized (this) {
            AudioBuffer.ClearAll();
            audioPlayer.AudioPlayStart();
            NativeCaller.PPPPStartAudio(strDID);
        }
    }

    //停止监听
    private void StopAudio() {
        synchronized (this) {
            audioPlayer.AudioPlayStop();
            AudioBuffer.ClearAll();
            NativeCaller.PPPPStopAudio(strDID);
        }
    }

    /*
     * 监听
     */
    private void goAudio() {
        dismissBrightAndContrastProgress();
        if (!isMcriophone) {
            if (bAudioStart) {
                Log.d("info", "没有声音");
                isTalking = false;
                bAudioStart = false;
                ptzAudio.setImageResource(R.drawable.close_audio);
                StopAudio();
            } else {
                Log.d("info", "有声");
                isTalking = true;
                bAudioStart = true;
                ptzAudio.setImageResource(R.drawable.open_audio);
                StartAudio();
            }

        } else {
            isMcriophone = false;
            bAudioRecordStart = false;
            isTalking = true;
            bAudioStart = true;
            ptzAudio.setImageResource(R.drawable.ptz_audio_on);
            StartAudio();
        }

    }

    /*
     * 分辨率设置
     */
    private void showResolutionPopWindow() {

        if (resolutionPopWindow != null && resolutionPopWindow.isShowing()) {
            return;
        }
        if (nStreamCodecType == ContentCommon.PPPP_STREAM_TYPE_JPEG) {
            // jpeg
            LinearLayout layout = (LinearLayout) LayoutInflater.from(this)
                    .inflate(R.layout.ptz_resolution_jpeg, null);
            TextView qvga = (TextView) layout
                    .findViewById(R.id.ptz_resolution_jpeg_qvga);
            TextView vga = (TextView) layout
                    .findViewById(R.id.ptz_resolution_jpeg_vga);
            if (reslutionMap.size() != 0) {
                getReslution();
            }
            if (isvga) {
                vga.setTextColor(Color.RED);
            }
            if (isqvga) {
                qvga.setTextColor(Color.RED);
            }
            qvga.setOnClickListener(this);
            vga.setOnClickListener(this);
            resolutionPopWindow = new PopupWindow(layout, 100,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            int x_begin = getWindowManager().getDefaultDisplay().getWidth() / 6;
            int y_begin = ptzResolutoin.getTop();
            resolutionPopWindow.showAtLocation(findViewById(R.id.play),
                    Gravity.BOTTOM | Gravity.RIGHT, x_begin, y_begin);

        } else {
            // h264
            LinearLayout layout = (LinearLayout) LayoutInflater.from(this)
                    .inflate(R.layout.ptz_resolution_h264, null);
            TextView qvga1 = (TextView) layout
                    .findViewById(R.id.ptz_resolution_h264_qvga);
            TextView vga1 = (TextView) layout
                    .findViewById(R.id.ptz_resolution_h264_vga);
            TextView p720 = (TextView) layout
                    .findViewById(R.id.ptz_resolution_h264_720p);
            TextView middle = (TextView) layout
                    .findViewById(R.id.ptz_resolution_h264_middle);
            TextView high = (TextView) layout
                    .findViewById(R.id.ptz_resolution_h264_high);
            TextView max = (TextView) layout
                    .findViewById(R.id.ptz_resolution_h264_max);

            if (reslutionMap.size() != 0) {
                getReslution();
            }
            if (ismax) {
                max.setTextColor(Color.RED);
            }
            if (ishigh) {
                high.setTextColor(Color.RED);
            }
            if (ismiddle) {
                middle.setTextColor(Color.RED);
            }
            if (isqvga1) {
                qvga1.setTextColor(Color.RED);
            }
            if (isvga1) {
                vga1.setTextColor(Color.RED);
            }
            if (isp720) {
                p720.setTextColor(Color.RED);
            }
            high.setOnClickListener(this);
            middle.setOnClickListener(this);
            max.setOnClickListener(this);
            qvga1.setOnClickListener(this);
            vga1.setOnClickListener(this);
            p720.setOnClickListener(this);
            resolutionPopWindow = new PopupWindow(layout, 100,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            int oreation = display.getOrientation();
            int x_begin = getWindowManager().getDefaultDisplay().getWidth() / 6;
            int y_begin = ptzResolutoin.getTop();
            resolutionPopWindow.showAtLocation(findViewById(R.id.play),
                    Gravity.BOTTOM | Gravity.RIGHT, x_begin, y_begin + 60);

        }

    }

    /**
     * 获取reslution
     */
    public static Map<String, Map<Object, Object>> reslutionMap = new HashMap<>();

    /**
     * 增加reslution
     */
    private void addReslution(String mess, boolean isfast) {
        if (reslutionMap.size() != 0) {
            if (reslutionMap.containsKey(strDID)) {
                reslutionMap.remove(strDID);
            }
        }
        Map<Object, Object> map = new HashMap<>();
        map.put(mess, isfast);
        reslutionMap.put(strDID, map);
    }

    private void getReslution() {
        if (reslutionMap.containsKey(strDID)) {
            Map<Object, Object> map = reslutionMap.get(strDID);
            if (map.containsKey("qvga")) {
                isqvga = true;
            } else if (map.containsKey("vga")) {
                isvga = true;
            } else if (map.containsKey("qvga1")) {
                isqvga1 = true;
            } else if (map.containsKey("vga1")) {
                isvga1 = true;
            } else if (map.containsKey("p720")) {
                isp720 = true;
            } else if (map.containsKey("high")) {
                ishigh = true;
            } else if (map.containsKey("middle")) {
                ismiddle = true;
            } else if (map.containsKey("max")) {
                ismax = true;
            }
        }
    }

    /**
     * @param type 亮度饱和对比度
     */
    private void setBrightOrContrast(final int type) {

        if (!bInitCameraParam) {
            return;
        }
        int width = getWindowManager().getDefaultDisplay().getWidth();
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.brightprogress, null);
        SeekBar seekBar = (SeekBar) layout.findViewById(R.id.brightseekBar1);
        seekBar.setMax(255);
        switch (type) {
            case BRIGHT:
                seekBar.setProgress(nBrightness);
                break;
            case CONTRAST:
                seekBar.setProgress(nContrast);
                break;
            default:
                break;
        }
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                switch (type) {
                    case BRIGHT:// 亮度
                        nBrightness = progress;
                        NativeCaller.PPPPCameraControl(strDID, BRIGHT, nBrightness);
                        break;
                    case CONTRAST:// 对比度
                        nContrast = progress;
                        NativeCaller.PPPPCameraControl(strDID, CONTRAST, nContrast);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress,
                                          boolean arg2) {

            }
        });
        mPopupWindowProgress = new PopupWindow(layout, width / 2, 180);
        mPopupWindowProgress.showAtLocation(findViewById(R.id.play),
                Gravity.TOP, 0, 0);

    }

    @Override
    protected void onDestroy() {
//        NativeCaller.StopPPPPLivestream(strDID);
        StopAudio();
        super.onDestroy();
    }

    /***
     * ConnectService callback 视频参数回调
     *
     * **/
    @Override
    public void callBackCameraParamNotify(String did, int resolution,
                                          int brightness, int contrast, int hue, int saturation, int flip, int mode) {
        Log.e("设备返回的参数", resolution + "," + brightness + "," + contrast + "," + hue + "," + saturation + "," + flip + "," + mode);
        nBrightness = brightness;
        nContrast = contrast;
        nResolution = resolution;
        bInitCameraParam = true;
    }

    /**
     * ConnectService callback 视频数据流回调
     * 底层不断回调该函数,每回调一次就传来一帧h264Data
     * 首先：videoData = videoBuf,为帧数据赋值
     * 判断用户是否有拍照操作：若有,将该帧图像转化并输出
     * 通知主线程将该帧数据压入帧池framePool.pushBytes()
     */
    @Override
    public void callBackVideoData(byte[] videoBuf, int h264Data, int len, int width, int height) {
        //Log.d("底层返回数据", "videobuf:"+videobuf+"--"+"h264Data"+h264Data+"len"+len+"width"+width+"height"+height);
        if (!bDisplayFinished)
            return;
        bDisplayFinished = false;
        videoData = videoBuf;
        videoDataLen = len;
        Message msg = new Message();
        if (h264Data == 1) { // H264
            nVideoWidths = width;
            nVideoHeights = height;
            if (isTakepic) {
                isTakepic = false;
                byte[] rgb = new byte[width * height * 2];
                NativeCaller.YUV4202RGB565(videoBuf, rgb, width, height);
                ByteBuffer buffer = ByteBuffer.wrap(rgb);
                mBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                mBmp.copyPixelsFromBuffer(buffer);
                takePicture(mBmp);
            }
            isH264 = true;
            msg.what = 1;


            // TODO 子线程进行人脸识别,在服务中进行

//            faceHelper.faceDetect(videoBuf,width,height);



        } else { // JPEG
            isJpeg = true;
            msg.what = 2;
        }
        mHandler.sendMessage(msg);
    }

    /***
     * ConnectService callback
     *
     * **/
    @Override
    public void callBackMessageNotify(String did, int msgType, int param) {
        Log.d("tag", "MessageNotify did: " + did + " msgType: " + msgType
                + " param: " + param);
        if (bManualExit)
            return;

        if (msgType == ContentCommon.PPPP_MSG_TYPE_STREAM) {
            nStreamCodecType = param;
            return;
        }

        if (msgType != ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS) {
            return;
        }

        if (!did.equals(strDID)) {
            return;
        }

        Message msg = new Message();
        msg.what = 1;
        msgHandler.sendMessage(msg);
    }

    /***
     * ConnectService callback
     *
     * **/
    @Override
    public void callBackAudioData(byte[] pcm, int len) {
        Log.d(LOG_TAG, "AudioData: len :+ " + len);

        if (!audioPlayer.isAudioPlaying()) {
            return;
        }
        CustomBufferHead head = new CustomBufferHead();
        CustomBufferData data = new CustomBufferData();
        head.length = len;
        head.startcode = AUDIO_BUFFER_START_CODE;
        data.head = head;
        data.data = pcm;
        AudioBuffer.addData(data);
    }

    /**
     * ConnectService callback
     *
     */
    @Override
    public void callBackH264Data(byte[] h264, int type, int size) {
        Log.d("tag", "CallBack_H264Data" + " type:" + type + " size:" + size);
        if (isTakeVideo) {
            Date date = new Date();
            long time = date.getTime();
            int tspan = (int) (time - videotime);
            Log.d("tag", "play  tspan:" + tspan);
            videotime = time;
        }
    }

    MyHandler mHandler = new MyHandler();

    private class MyHandler extends Handler {

        public void handleMessage(Message msg) {
            if (msg.what == 1 || msg.what == 2) {
                setViewVisible();
            }
            int width = getWindowManager().getDefaultDisplay().getWidth();
            int height = getWindowManager().getDefaultDisplay().getHeight();
            switch (msg.what) {
                case 1: // h264
                {
                    // 枚举视频清晰度类型
                    if (reslutionMap.size() == 0) {
                        if (nResolution == 0) {
                            ismax = true;
                            ismiddle = false;
                            ishigh = false;
                            isp720 = false;
                            isqvga1 = false;
                            isvga1 = false;
                            addReslution(stmax, ismax);
                        } else if (nResolution == 1) {
                            ismax = false;
                            ismiddle = false;
                            ishigh = true;
                            isp720 = false;
                            isqvga1 = false;
                            isvga1 = false;
                            addReslution(sthigh, ishigh);
                        } else if (nResolution == 2) {
                            ismax = false;
                            ismiddle = true;
                            ishigh = false;
                            isp720 = false;
                            isqvga1 = false;
                            isvga1 = false;
                            addReslution(stmiddle, ismiddle);
                        } else if (nResolution == 3) {
                            ismax = false;
                            ismiddle = false;
                            ishigh = false;
                            isp720 = true;
                            isqvga1 = false;
                            isvga1 = false;
                            addReslution(stp720, isp720);
                            nResolution = 3;
                        } else if (nResolution == 4) {
                            ismax = false;
                            ismiddle = false;
                            ishigh = false;
                            isp720 = false;
                            isqvga1 = false;
                            isvga1 = true;
                            addReslution(stvga1, isvga1);
                        } else if (nResolution == 5) {
                            ismax = false;
                            ismiddle = false;
                            ishigh = false;
                            isp720 = false;
                            isqvga1 = true;
                            isvga1 = false;
                            addReslution(stqvga1, isqvga1);
                        }
                    } else {
                        if (reslutionMap.containsKey(strDID)) {
                            getReslution();
                        } else {
                            if (nResolution == 0) {
                                ismax = true;
                                ismiddle = false;
                                ishigh = false;
                                isp720 = false;
                                isqvga1 = false;
                                isvga1 = false;
                                addReslution(stmax, ismax);
                            } else if (nResolution == 1) {
                                ismax = false;
                                ismiddle = false;
                                ishigh = true;
                                isp720 = false;
                                isqvga1 = false;
                                isvga1 = false;
                                addReslution(sthigh, ishigh);
                            } else if (nResolution == 2) {
                                ismax = false;
                                ismiddle = true;
                                ishigh = false;
                                isp720 = false;
                                isqvga1 = false;
                                isvga1 = false;
                                addReslution(stmiddle, ismiddle);
                            } else if (nResolution == 3) {
                                ismax = false;
                                ismiddle = false;
                                ishigh = false;
                                isp720 = true;
                                isqvga1 = false;
                                isvga1 = false;
                                addReslution(stp720, isp720);
                                nResolution = 3;
                            } else if (nResolution == 4) {
                                ismax = false;
                                ismiddle = false;
                                ishigh = false;
                                isp720 = false;
                                isqvga1 = false;
                                isvga1 = true;
                                addReslution(stvga1, isvga1);
                            } else if (nResolution == 5) {
                                ismax = false;
                                ismiddle = false;
                                ishigh = false;
                                isp720 = false;
                                isqvga1 = true;
                                isvga1 = false;
                                addReslution(stqvga1, isqvga1);
                            }
                        }
                    }

                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                width, width * 3 / 4);
                        lp.gravity = Gravity.CENTER;
//                        lp.gravity = Gravity.TOP;
                        playSurface.setLayoutParams(lp);
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                width, height);
                        lp.gravity = Gravity.CENTER;
                        playSurface.setLayoutParams(lp);
                    }
                    framePool.pushBytes(videoData, videoDataLen, nVideoWidths, nVideoHeights);
                }
                break;
                case 2: // JPEG
                {
                    if (reslutionMap.size() == 0) {
                        if (nResolution == 1) {
                            isvga = true;
                            isqvga = false;
                            addReslution(stvga, isvga);
                        } else if (nResolution == 0) {
                            isqvga = true;
                            isvga = false;
                            addReslution(stqvga, isqvga);
                        }
                    } else {
                        if (reslutionMap.containsKey(strDID)) {
                            getReslution();
                        } else {
                            if (nResolution == 1) {
                                isvga = true;
                                isqvga = false;
                                addReslution(stvga, isvga);
                            } else if (nResolution == 0) {
                                isqvga = true;
                                isvga = false;
                                addReslution(stqvga, isqvga);
                            }
                        }
                    }
                    mBmp = BitmapFactory.decodeByteArray(videoData, 0,
                            videoDataLen);
                    if (mBmp == null) {
                        bDisplayFinished = true;
                        return;
                    }
                    if (isTakepic) {
                        takePicture(mBmp);
                        isTakepic = false;
                    }
                    nVideoWidths = mBmp.getWidth();
                    nVideoHeights = mBmp.getHeight();

                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // Bitmap
                        Bitmap bitmap = Bitmap.createScaledBitmap(mBmp, width,
                                width * 3 / 4, true);
                        //videoViewLandscape.setVisibility(View.GONE);
                        videoViewPortrait.setVisibility(View.VISIBLE);
                        videoViewPortrait.setImageBitmap(bitmap);

                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Bitmap bitmap = Bitmap.createScaledBitmap(mBmp, width, height, true);
                        videoViewPortrait.setVisibility(View.GONE);
                        //videoViewLandscape.setVisibility(View.VISIBLE);
                        //videoViewLandscape.setImageBitmap(bitmap);
                    }

                }
                break;
                default:
                    break;
            }
            if (msg.what == 1 || msg.what == 2) {
                bDisplayFinished = true;
            }
        }

    }

}
