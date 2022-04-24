package com.ipcamera.demotest.database.localdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ipcamera.demotest.model.FaceRegisterInfo;

import java.util.ArrayList;
import java.util.List;

import static com.ipcamera.demotest.database.localdb.DBOpenHelper.FEATURE_TABLE_NAME;

public class LocalFeatureTable {
    private final String TAG = "LocalFeatureTable";
    private SQLiteDatabase mDatabase;

    public void setDatabase(SQLiteDatabase db) {
        mDatabase = db;
    }

    public boolean addFace(String id,byte[] feature,byte[] portrait) {
        Log.d(TAG, "addFace: mDatabase"+mDatabase);
        if (mDatabase == null) return false;
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("feature", feature);
        cv.put("portrait", portrait);
        return mDatabase.insert(FEATURE_TABLE_NAME, null, cv) >= 0;
    }

    public List<FaceRegisterInfo> queryAllFace(){
        Log.d(TAG, String.valueOf(mDatabase == null) );
        if (mDatabase == null) return null;
        Cursor cursor;
        List<FaceRegisterInfo> faceList = new ArrayList<>();
        cursor = mDatabase.query(FEATURE_TABLE_NAME, null, null,
                null, null, null, null);
        while (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex("id"));
            byte[] feature = cursor.getBlob(cursor.getColumnIndex("feature"));
            FaceRegisterInfo info = new FaceRegisterInfo(feature,id);
            faceList.add(info);
            Log.d(TAG, "queryFace:size: " + faceList.size());
        }
        if (cursor!=null) cursor.close();
        return faceList;
    }

    public int deleteAllFace(){
            return mDatabase.delete(FEATURE_TABLE_NAME, null, null);
    }
}
