package com.ipcamera.demotest.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ipcamera.demotest.adapter.FaceDataCardAdapter;
import com.ipcamera.demotest.database.DBMaster;
import com.ipcamera.demotest.database.localdb.AddFaceListener;
import com.ipcamera.demotest.dialog.ModifyFaceDialog;
import com.ipcamera.demotest.model.FaceCardInfo;
import com.ipcamera.demotest.dialog.MoreSelectDialog;
import com.ipcamer.demotest.R;

import java.util.List;

public class FaceLibraryActivity extends AppCompatActivity implements FaceDataCardAdapter.SlideListener,FaceDataCardAdapter.DeleteFaceListener, ModifyFaceDialog.ModifyFaceListener, AddFaceListener {

    private final Context mContext = FaceLibraryActivity.this;
    private FaceDataCardAdapter mAdapter;
    private List<FaceCardInfo> faceCardInfoList;
    private final static String TAG = "FaceLibraryActivity";
    private RecyclerView recyclerView;
    private ImageView backButton;
    private ImageView moreSelectButton;
    private RelativeLayout moreSelectDialog;
    private Boolean isShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_library);
        init();
        findView();
    }

    private void init() {
        mAdapter = new FaceDataCardAdapter(mContext,this,this,this);
        faceCardInfoList = DBMaster.getLocalDB().getLocalFaceInfoTable().queryFace();
    }

    private void findView(){
        moreSelectDialog = findViewById(R.id.more_select_dialog);
        findViewById(R.id.back_from_face_library).setOnClickListener(v -> finish());


        findViewById(R.id.more_select).setOnClickListener(v ->{
//            if(!isFinishing()) {
//                MoreSelectDialog.getInstance(mContext).showDialog();
//            }
            if(!isShow){
                moreSelectDialog.setVisibility(View.VISIBLE);
                moreSelectDialog.bringToFront();
                isShow = true;
            }else{
                moreSelectDialog.setVisibility(View.GONE);
                isShow = false;
            }
        });
        findViewById(R.id.video_reg).setOnClickListener(v ->
                mContext.startActivity(new Intent(mContext, VideoRegActivity.class)));
        findViewById(R.id.image_reg).setOnClickListener(v ->
                mContext.startActivity(new Intent(mContext, ImageRegActivity.class)));


        recyclerView = findViewById(R.id.face_recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter.setFaceList(faceCardInfoList);
        recyclerView.setAdapter(mAdapter);

    }

    private void showToast(String toast){
        Toast.makeText(mContext,toast,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void deleteCallback(FaceCardInfo faceCardInfo) {
        faceCardInfoList.remove(faceCardInfo);
        mAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onResume(){
        super.onResume();
        faceCardInfoList = DBMaster.getLocalDB().getLocalFaceInfoTable().queryFace();
        mAdapter.setFaceList(faceCardInfoList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MoreSelectDialog.getInstance(mContext).dismissDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MoreSelectDialog.getInstance(mContext).dismissDialog();
    }



    @Override
    public void modifyCallback(int index, FaceCardInfo faceCardInfo) {
        faceCardInfoList.get(index).setName(faceCardInfo.getName());
        faceCardInfoList.get(index).setFaceRelationShip(faceCardInfo.getFaceRelationShip());
        faceCardInfoList.get(index).setPriority(faceCardInfo.getPriority());
        faceCardInfoList.get(index).setRegtime(faceCardInfo.getRegtime());
        mAdapter.notifyItemChanged(index);
    }

    @Override
    public void regSuccessCallback() {
        // 重新加载数据
        faceCardInfoList = DBMaster.getLocalDB().getLocalFaceInfoTable().queryFace();
        mAdapter.setFaceList(faceCardInfoList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSlidingCallback() {
        moreSelectDialog.setVisibility(View.GONE);
        isShow = false;
    }
}
