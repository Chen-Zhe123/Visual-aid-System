package com.ipcamera.demotest.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.dialog.ModifyFaceDialog;
import com.ipcamera.demotest.model.FaceCardInfo;
import com.ipcamera.demotest.database.localdb.FaceServer;
import com.ipcamer.demotest.R;
import com.ipcamera.demotest.utils.TTSPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FaceDataCardAdapter extends RecyclerView.Adapter<FaceDataCardAdapter.ViewHolder> {

    // 浅拷贝
    private List<FaceCardInfo> userList;
    private Context mContext;
    private final String format = "%02d";
    private DeleteFaceListener mListener;
    private ModifyFaceDialog.ModifyFaceListener mModifyListener;
    private SlideListener mSlideListener;

    public FaceDataCardAdapter(Context context, DeleteFaceListener listener, ModifyFaceDialog.ModifyFaceListener modifyListener, SlideListener slideListener) {
        mContext = context;
        mListener = listener;
        mModifyListener = modifyListener;
        mSlideListener = slideListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView faceImage;
        TextView faceName;
        TextView faceRelationShip;
        TextView frPriorityLevel;
        TextView regTime;
        TextView number;
        ImageView deleteButton;
        ImageView modifyButton;

        public ViewHolder(View view) {
            super(view);
            faceImage = view.findViewById(R.id.face_image);
            faceName = view.findViewById(R.id.face_name_content);
            faceRelationShip = view.findViewById(R.id.face_relationship_content);
            frPriorityLevel = view.findViewById(R.id.fr_priority_level_content);
            regTime = view.findViewById(R.id.face_reg_time_content);
            number = view.findViewById(R.id.face_number_content);
            deleteButton = view.findViewById(R.id.delete_face_button);
            modifyButton = view.findViewById(R.id.modify_face_info);
        }
    }

    public void setFaceList(List<FaceCardInfo> userList) {
        this.userList = userList;
        this.notifyDataSetChanged();
    }

    public List<FaceCardInfo> getFaceList() {
        return userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.face_library_item, parent,
                false);
        return new ViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale", "ClickableViewAccessibility"})
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final FaceCardInfo faceCardInfo = userList.get(position);
        holder.faceName.setText(faceCardInfo.getName());
        holder.faceRelationShip.setText(faceCardInfo.getFaceRelationShip());
        Date date = new Date(Long.parseLong(faceCardInfo.getRegtime()));
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        String time = dateFormat.format(date);
        holder.frPriorityLevel.setText(faceCardInfo.getPriority());
        holder.regTime.setText(time);
        holder.number.setText(String.format(format, position + 1));
        // TODO 获取并设置头像
        File imgFile = new File(FaceServer.ROOT_PATH + File.separator + FaceServer.SAVE_IMG_DIR + File.separator + faceCardInfo.getId() + FaceServer.IMG_SUFFIX);
//        Glide.with(holder.faceImage).load(imgFile).into(holder.faceImage);
        Bitmap bitmap = null;
        try {
            FileInputStream fis = new FileInputStream(imgFile);
            bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) holder.faceImage.setImageBitmap(bitmap);
        holder.faceImage.setOnClickListener(v -> {
            if (holder.deleteButton.isShown()) holder.deleteButton.setVisibility(View.GONE);
            else holder.deleteButton.setVisibility(View.VISIBLE);
        });
        holder.deleteButton.setOnClickListener(v -> {
            Boolean success1 = DBMaster.getLocalDB().getLocalFaceInfoTable().deleteFace(faceCardInfo.getId());
            Boolean success2 = FaceServer.getInstance().deleteFaceById(mContext, faceCardInfo.getId());
            Log.d("asdf", "onBindViewHolder: " + success1 + success2);
            if (success1 && success2) {
                mListener.deleteCallback(faceCardInfo);
                Toast.makeText(mContext, "人脸" + faceCardInfo.getName() + "删除成功", Toast.LENGTH_SHORT).show();
                TTSPlayer.getInstance().startSpeaking("人脸" + faceCardInfo.getName() + "删除成功");
            } else {
                Toast.makeText(mContext, "人脸" + faceCardInfo.getName() + "删除失败", Toast.LENGTH_SHORT).show();
                TTSPlayer.getInstance().startSpeaking("人脸" + faceCardInfo.getName() + "删除失败");
            }
        });
        holder.modifyButton.setOnClickListener(v -> {
            ModifyFaceDialog.getInstance(mContext).setListener(mModifyListener);
            ModifyFaceDialog.getInstance(mContext).show(position, faceCardInfo);
        });
        holder.itemView.setOnTouchListener((v, event) -> {
            mSlideListener.onSlidingCallback();
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public interface SlideListener {
        void onSlidingCallback();
    }

    public interface DeleteFaceListener {

        void deleteCallback(FaceCardInfo faceCardInfo);
    }
}