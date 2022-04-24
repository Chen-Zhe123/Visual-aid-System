package com.ipcamera.demotest.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

//import com.arcsoft.arcfacedemo.R;
import com.ipcamer.demotest.R;
import com.ipcamera.demotest.database.CompareResult;
import com.ipcamera.demotest.database.localdb.FaceServer;
import com.bumptech.glide.Glide;
import com.ipcamera.demotest.model.FaceCardInfo;

import java.io.File;
import java.util.List;

public class FaceSearchResultAdapter extends RecyclerView.Adapter<FaceSearchResultAdapter.CompareResultHolder> {
    private List<CompareResult> compareResultList;
    private List<FaceCardInfo> faceInfoList;
    private LayoutInflater inflater;

    public FaceSearchResultAdapter(List<CompareResult> compareResultList,List<FaceCardInfo> faceInfoList, Context context) {
        inflater = LayoutInflater.from(context);
        this.compareResultList = compareResultList;
        this.faceInfoList = faceInfoList;
    }

    @NonNull
    @Override
    public CompareResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.recycler_item_search_result, null, false);
        CompareResultHolder compareResultHolder = new CompareResultHolder(itemView);
        compareResultHolder.textView = itemView.findViewById(R.id.tv_item_name);
        compareResultHolder.imageView = itemView.findViewById(R.id.iv_item_head_img);
        return compareResultHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CompareResultHolder holder, int position) {
        if (compareResultList == null) {
            return;
        }
        File imgFile = new File(FaceServer.ROOT_PATH + File.separator + FaceServer.SAVE_IMG_DIR + File.separator + compareResultList.get(position).getUserName() + FaceServer.IMG_SUFFIX);
        Glide.with(holder.imageView)
                .load(imgFile)
                .into(holder.imageView);
        // 搜索姓名
        String name = null;
        for(FaceCardInfo info:faceInfoList){
            if(info.getId().equals(compareResultList.get(position).getUserName())){
                   name = info.getName();
                   break;
            }
        }
        holder.textView.setText(name == null ? compareResultList.get(position).getUserName() : name);
    }

    @Override
    public int getItemCount() {
        return compareResultList == null ? 0 : compareResultList.size();
    }

    class CompareResultHolder extends RecyclerView.ViewHolder {

        TextView textView;
        ImageView imageView;

        CompareResultHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
