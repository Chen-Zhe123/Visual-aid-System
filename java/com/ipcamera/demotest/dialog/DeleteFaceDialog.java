package com.ipcamera.demotest.dialog;

import android.content.Context;

public class DeleteFaceDialog {

    private Context mContext;

    public DeleteFaceDialog(android.content.Context context){
        this.mContext = context;
    }

    public void show(){

    }

    public interface DeleteFaceListener{

        void deleteCallback(int index);

    }
}
