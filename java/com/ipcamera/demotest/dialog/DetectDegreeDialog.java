package com.ipcamera.demotest.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

//import com.arcsoft.arcfacedemo.R;
import com.ipcamera.demotest.common.SharedPreferenceUtil;
import com.ipcamer.demotest.R;

import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_0_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_180_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_270_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_90_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_ALL_OUT;

public class DetectDegreeDialog extends DialogFragment implements View.OnClickListener {

    private ModifyDetectDegreeListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_choose_detect_degree, container);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        initView(dialogView);
        return dialogView;
    }

    private void initView(View dialogView) {
        ImageView ivClose = dialogView.findViewById(R.id.iv_close);
        ivClose.setOnClickListener(this);
        //设置视频模式下的人脸优先检测方向
        RadioGroup radioGroupFtOrient = dialogView.findViewById(R.id.radio_group_ft_orient);
        RadioButton rbOrient0 = dialogView.findViewById(R.id.rb_orient_0);
        RadioButton rbOrient90 = dialogView.findViewById(R.id.rb_orient_90);
        RadioButton rbOrient180 = dialogView.findViewById(R.id.rb_orient_180);
        RadioButton rbOrient270 = dialogView.findViewById(R.id.rb_orient_270);
        RadioButton rbOrientAll = dialogView.findViewById(R.id.rb_orient_all);
        switch (SharedPreferenceUtil.getFtOrient(getActivity())) {
            case ASF_OP_90_ONLY:
                rbOrient90.setChecked(true);
                break;
            case ASF_OP_180_ONLY:
                rbOrient180.setChecked(true);
                break;
            case ASF_OP_270_ONLY:
                rbOrient270.setChecked(true);
                break;
            case ASF_OP_ALL_OUT:
                rbOrientAll.setChecked(true);
                break;
            case ASF_OP_0_ONLY:
            default:
                rbOrient0.setChecked(true);
                break;
        }
        radioGroupFtOrient.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_orient_90:
                        SharedPreferenceUtil.setFtOrient(getActivity(), ASF_OP_90_ONLY);
                        listener.modifyCallback(2);
                        break;
                    case R.id.rb_orient_180:
                        SharedPreferenceUtil.setFtOrient(getActivity(), ASF_OP_180_ONLY);
                        listener.modifyCallback(4);
                        break;
                    case R.id.rb_orient_270:
                        SharedPreferenceUtil.setFtOrient(getActivity(), ASF_OP_270_ONLY);
                        listener.modifyCallback(3);
                        break;
                    case R.id.rb_orient_all:
                        SharedPreferenceUtil.setFtOrient(getActivity(), ASF_OP_ALL_OUT);
                        listener.modifyCallback(5);
                        break;
                    case R.id.rb_orient_0:
                    default:
                        SharedPreferenceUtil.setFtOrient(getActivity(), ASF_OP_0_ONLY);
                        listener.modifyCallback(1);
                        break;
                }
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null){
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public void setListener(ModifyDetectDegreeListener listener){
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }

    public interface ModifyDetectDegreeListener{

        void modifyCallback(int index);
    }

}
