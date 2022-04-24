package com.ipcamera.demotest.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamera.demotest.common.Constants;
import com.ipcamer.demotest.R;

public class RegDialog {

    private Dialog mDialog;
    @SuppressLint("StaticFieldLeak")
    private static RegDialog instance;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private RegisterListener registerListener;
    private Bitmap headPortrait;
    private ImageView headPorImage;
    private EditText nameEdit;
    private EditText relationshipEdit;
    private TextView sureReg;
    private TextView cancelReg;
    private RadioGroup radioGroup;
    private String priority = Constants.HIGH_DETECT_PRIORITY;

    public static synchronized RegDialog getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = new RegDialog();
        }
        return instance;
    }

    public void setRegListener(RegisterListener listener) {
        registerListener = listener;
    }

    public void showDialog(Bitmap bitmap) {
        Log.d("chenzhe", "showDialog: 4");
        mDialog = new Dialog(mContext);
        mDialog.setContentView(R.layout.dialog_reg);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        headPortrait = bitmap;
        Matrix matrix = new Matrix();
        // 将图片放大1.5倍
        matrix.postScale(3.5f, 3.5f);
        headPortrait = Bitmap.createBitmap(headPortrait, 0, 0, headPortrait.getWidth(), headPortrait.getHeight(), matrix, true);
        findView();
        mDialog.show();
    }

    public void findView() {
        headPorImage = mDialog.findViewById(R.id.reg_head_portrait);
        headPorImage.setImageBitmap(headPortrait);
        nameEdit = mDialog.findViewById(R.id.face_name_edit);
        relationshipEdit = mDialog.findViewById(R.id.face_relationship_edit);
        sureReg = mDialog.findViewById(R.id.sure_reg_button);
        cancelReg = mDialog.findViewById(R.id.cancel_reg_button);
        sureReg.setOnClickListener((View v) -> {
            try {
                sureReg();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        cancelReg.setOnClickListener((View v) -> cancelReg());
        radioGroup = ((RadioGroup) mDialog.findViewById(R.id.detect_priority_radioGroup));
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.high_priority:
                    priority = Constants.HIGH_DETECT_PRIORITY;
                    break;
                case R.id.medium_priority:
                    priority = Constants.MEDIUM_DETECT_PRIORITY;
                    break;
                case R.id.low_priority:
                    priority = Constants.LOW_DETECT_PRIORITY;
                    break;
                default:
                    break;
            }
        });
    }

    public void sureReg() throws Exception {
        // TODO 回调，传参
        String name = nameEdit.getText().toString();
        String relationship = relationshipEdit.getText().toString();
        if (name.length() == 0 || relationship.length() == 0) {
            showToast("输入不可为空！");
        } else {
            registerListener.sureVideoRegCallback(name,relationship,priority);
            if (mDialog != null) {
                mDialog.dismiss();
            }
        }
    }

    public void cancelReg() {
        if (mDialog != null) {
            registerListener.cancelRegCallback();
            mDialog.dismiss();
        }
    }

    private void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * 用户行为的回调
     */
    public interface RegisterListener {

        void sureVideoRegCallback(String name, String relationship, String priority) throws Exception;

        void cancelRegCallback();
    }


}
