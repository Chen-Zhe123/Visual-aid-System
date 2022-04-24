package com.ipcamera.demotest.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.TextView;

import com.ipcamera.demotest.activity.ImageRegActivity;
import com.ipcamera.demotest.activity.VideoRegActivity;
import com.ipcamer.demotest.R;

public class MoreSelectDialog {


    private static Context mContext;
    private static MoreSelectDialog instance;
    private Dialog mDialog;
    private TextView videoReg;
    private TextView imageReg;
    private ChooseImageListener mListener;

    public static synchronized MoreSelectDialog getInstance(Context context) {
        if (instance == null) {
            mContext = context;
            instance = new MoreSelectDialog();
        }
        return instance;
    }

    public void setListener(ChooseImageListener listener){
        mListener = listener;
    }

    public void showDialog() {
        mDialog = new Dialog(mContext);
        mDialog.setContentView(R.layout.dialog_more_select);
        findView();
        WindowManager.LayoutParams params = mDialog.getWindow().getAttributes();
//        mDialog.getWindow().setGravity(Gravity.END);
//        mDialog.getWindow().setGravity(Gravity.RIGHT);
//        mDialog.getWindow().setGravity(Gravity.TOP);
//        params.x = 60;
//        params.y = 90;
        mDialog.getWindow().setAttributes(params);
        mDialog.show();
    }

    public void dismissDialog(){
        if(mDialog != null){
            mDialog.dismiss();
        }
        instance = null;
    }

    public void findView() {
        mDialog.findViewById(R.id.video_reg).setOnClickListener(v ->
                mContext.startActivity(new Intent(mContext, VideoRegActivity.class)));
        mDialog.findViewById(R.id.image_reg).setOnClickListener(v ->
                mContext.startActivity(new Intent(mContext, ImageRegActivity.class)));
//        mDialog.findViewById(R.id.image_reg).setOnClickListener(v ->{
//                   mListener.chooseImageCallback();
//                });

    }

    public interface ChooseImageListener {
        void chooseImageCallback();
    }

}