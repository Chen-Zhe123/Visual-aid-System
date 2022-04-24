package com.ipcamera.demotest.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.ipcamer.demotest.R;
import com.ipcamera.demotest.common.Constants;
import com.ipcamera.demotest.common.SharedPreferenceUtil;

public class SetFilterDialog {

    private Dialog mDialog;
    @SuppressLint("StaticFieldLeak")
    private static SetFilterDialog instance;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private ModifyFilterListener mListener;
    private Boolean isNoFilter;

    private RadioGroup radioGroup;
    private String priority = Constants.HIGH_DETECT_PRIORITY;

    public static synchronized SetFilterDialog getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = new SetFilterDialog();
        }
        return instance;
    }

    public void setListener(ModifyFilterListener listener) {
        mListener = listener;
    }

    public void show() {
        mDialog = new Dialog(mContext);
        mDialog.setContentView(R.layout.dialog_set_fliter);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.show();
        int filter = SharedPreferenceUtil.getFilter(mContext);
        RadioGroup radioGroup = mDialog.findViewById(R.id.set_filter_radio_group);
        RadioButton radioButton1 = mDialog.findViewById(R.id.no_filter);
        RadioButton radioButton2 = mDialog.findViewById(R.id.only_high_medium);
        RadioButton radioButton3 = mDialog.findViewById(R.id.only_high);
        switch (filter) {
            case Constants.NO_FILTER:
                radioButton1.setChecked(true);
                isNoFilter = true;
                break;
            case Constants.ONLY_HIGH_MEDIUM:
                radioButton2.setChecked(true);
                isNoFilter = false;
                break;
            case Constants.ONLY_HIGH:
                radioButton3.setChecked(true);
                isNoFilter = false;
                break;
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.no_filter:
                    SharedPreferenceUtil.saveFilter(mContext, Constants.NO_FILTER);
                    mListener.modifyFilterCallback(true);
                    break;
                case R.id.only_high_medium:
                    SharedPreferenceUtil.saveFilter(mContext, Constants.ONLY_HIGH_MEDIUM);
                    mListener.modifyFilterCallback(false);
                    break;
                case R.id.only_high:
                    SharedPreferenceUtil.saveFilter(mContext, Constants.ONLY_HIGH);
                    mListener.modifyFilterCallback(false);
                    break;
            }
        });
    }

    public interface ModifyFilterListener {
        void modifyFilterCallback(Boolean isNoFilter);
    }
}
