package com.ipcamera.demotest.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamer.demotest.R;
import com.ipcamera.demotest.activity.ImageRegActivity;
import com.ipcamera.demotest.activity.VideoRegActivity;
import com.ipcamera.demotest.common.Constants;
import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.model.FaceCardInfo;
import com.ipcamera.demotest.utils.TTSPlayer;

public class ModifyFaceDialog {

    private static Context mContext;
    private static ModifyFaceDialog instance;
    private ModifyFaceListener mListener;
    private Dialog mDialog;
    String priority;
    public ModifyFaceDialog(Context context) {
        this.mContext = context;
    }

    public static synchronized ModifyFaceDialog getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = new ModifyFaceDialog(context);
        }
        return instance;
    }

    public void show(int index,FaceCardInfo info) {
        mDialog = new Dialog(mContext);
        mDialog.setContentView(R.layout.dialog_modify_face);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.show();

        priority = info.getPriority();
        EditText nameEdit = mDialog.findViewById(R.id.modify_name_edit);
        nameEdit.setText(info.getName());
        EditText relationshipEdit = mDialog.findViewById(R.id.modify_relationship_edit);
        relationshipEdit.setText(info.getFaceRelationShip());

        RadioGroup radioGroup = mDialog.findViewById(R.id.modify_detect_priority_radioGroup);
        RadioButton radio1 = mDialog.findViewById(R.id.high_priority1);
        RadioButton radio2 = mDialog.findViewById(R.id.medium_priority1);
        RadioButton radio3 = mDialog.findViewById(R.id.low_priority1);
        switch (info.getPriority()){
            case Constants.HIGH_DETECT_PRIORITY:
                radio1.setChecked(true);
                break;
            case Constants.MEDIUM_DETECT_PRIORITY:
                radio2.setChecked(true);
                break;
            case Constants.LOW_DETECT_PRIORITY:
                radio3.setChecked(true);
                break;
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.high_priority1:
                    priority = Constants.HIGH_DETECT_PRIORITY;
                    break;
                case R.id.medium_priority1:
                    priority = Constants.MEDIUM_DETECT_PRIORITY;
                    break;
                case R.id.low_priority1:
                    priority = Constants.LOW_DETECT_PRIORITY;
                    break;
                default:
                    break;
            }
        });
        TextView sureModify = mDialog.findViewById(R.id.sure_modify);
        sureModify.setOnClickListener(v ->{
            String name = nameEdit.getText().toString();
            String relationship = relationshipEdit.getText().toString();
            if (name.length() == 0 || relationship.length() == 0) {
                showToast("输入不可为空！");
            }else{
                FaceCardInfo faceCardInfo = new FaceCardInfo();
                faceCardInfo.setId(info.getId());
                faceCardInfo.setName(name);
                faceCardInfo.setFaceRelationShip(relationship);
                faceCardInfo.setPriority(priority);
                faceCardInfo.setRegtime(info.getRegtime());
                boolean success = DBMaster.getLocalDB().getLocalFaceInfoTable().modifyFace(faceCardInfo);
                if(success){
                    mListener.modifyCallback(index,faceCardInfo);
                    TTSPlayer.getInstance().startSpeaking("人脸信息修改成功");
                    mDialog.dismiss();
                }else{
                    TTSPlayer.getInstance().startSpeaking("人脸信息修改失败");
                }
            }
        });
        TextView cancelModify = mDialog.findViewById(R.id.cancel_modify);
        cancelModify.setOnClickListener(v ->{
            mDialog.dismiss();
        });
        LinearLayout updateLayout = mDialog.findViewById(R.id.update_face_layout);
        TextView updateFace = mDialog.findViewById(R.id.update_face);
        TextView videoReg = mDialog.findViewById(R.id.to_video_reg);
        TextView imageReg = mDialog.findViewById(R.id.to_image_reg);
        TextView cancelUpdate = mDialog.findViewById(R.id.cancel_update_face);
//        videoReg.setOnClickListener(v ->{
//            mContext.startActivity(new Intent(mContext, VideoRegActivity.class));
//        });
//        imageReg.setOnClickListener(v ->{
//            mContext.startActivity(new Intent(mContext, ImageRegActivity.class));
//        });
        cancelUpdate.setOnClickListener(v ->{
            updateLayout.setVisibility(View.GONE);
            updateFace.setVisibility(View.VISIBLE);
        });
//        updateFace.setOnClickListener(v ->{
//            updateFace.setVisibility(View.GONE);
//            updateLayout.setVisibility(View.VISIBLE);
//        });

    }

    public void setListener(ModifyFaceListener listener) {
        mListener = listener;
    }

    public interface ModifyFaceListener {

        void modifyCallback(int index, FaceCardInfo faceCardInfo);
    }
    private void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }
}
