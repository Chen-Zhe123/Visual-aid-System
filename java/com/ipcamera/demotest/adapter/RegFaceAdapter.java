package com.ipcamera.demotest.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamer.demotest.R;
import com.ipcamera.demotest.database.CompareResult;
import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.database.localdb.AddFaceListener;
import com.ipcamera.demotest.database.localdb.FaceServer;
import com.ipcamera.demotest.dialog.RegDialog;
import com.ipcamera.demotest.model.RegFaceBean;
import com.ipcamera.demotest.utils.TTSPlayer;

import java.util.List;

public class RegFaceAdapter extends RecyclerView.Adapter<RegFaceAdapter.ViewHolder> {

    // 浅拷贝
    private List<RegFaceBean> mFaceList;

    private Context mContext;
    private FaceDataCardAdapter.DeleteFaceListener mListener;
    private AddFaceListener addFaceListener;

    public RegFaceAdapter(Context context) {
        mContext = context;
    }

    public void setFaceList(List<RegFaceBean> faceList) {
        mFaceList = faceList;
    }

    public void setListener(AddFaceListener addFaceListener) {
        this.addFaceListener = addFaceListener;
    }

    @NonNull
    @Override
    public RegFaceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reg_face_item, parent,
                false);
        return new RegFaceAdapter.ViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(final RegFaceAdapter.ViewHolder holder, int position) {
        final RegFaceBean face = mFaceList.get(position);
        holder.faceImage.setImageBitmap(face.getFaceImage());
        holder.regButton.setOnClickListener(v -> {
            RegDialog.getInstance(mContext).setRegListener(new RegDialog.RegisterListener() {
                @Override
                public void sureVideoRegCallback(String name, String relationship, String priority) {
                    // 判断重复注册
                    CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(face.getFeature());
                    if (compareResult != null) {
                        if (compareResult.getSimilar() > 0.8F) {
                            showToast("人脸已经注册过了,请勿重复注册");
                            return;
                        }
                    }
                    boolean success1 = FaceServer.getInstance().registerByImage(mContext,
                            face.getFeature(), face.getFaceImage(), face.getId());
                    boolean success2 = false;
                    if (success1) {
                        success2 = DBMaster.getLocalDB().getLocalFaceInfoTable().addFace(
                                face.getId(),
                                name, relationship, priority, String.valueOf(System.currentTimeMillis()));
                    }
                    // TODO 任何一方失败都需要回滚对数据库的操作
                    Log.d("asdf", "sureRegCallback: " + success1 + success2);
                    if (success1 && success2) {
                        showToast("注册成功");
                        TTSPlayer.getInstance().startSpeaking("注册成功");

                        // 更新人脸库
                        // 可在LibraryActivity resume时重新加载数据
//                        addFaceListener.regSuccessCallback();

                        // 移除数据
//                        mFaceList.remove(face);
//                        notifyItemRangeRemoved(pos,1);
//                        notifyDataSetChanged();
                    } else {
                        showToast("注册失败");
                        TTSPlayer.getInstance().startSpeaking("注册失败");
                    }
                }

                @Override
                public void cancelRegCallback() {

                }
            });
            RegDialog.getInstance(mContext).showDialog(face.getFaceImage());

        });
    }

    @Override
    public int getItemCount() {
        return mFaceList.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView faceImage;
        TextView regButton;

        public ViewHolder(View view) {
            super(view);
            faceImage = view.findViewById(R.id.show_face_to_reg);
            regButton = view.findViewById(R.id.begin_reg);
        }
    }

    private void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

//    public interface ImageFaceRegListener{
//        void sure
//    }

}
