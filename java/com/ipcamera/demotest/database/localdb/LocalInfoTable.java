package com.ipcamera.demotest.database.localdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.ipcamera.demotest.model.FaceCardInfo;

import java.util.ArrayList;
import java.util.List;

import static com.ipcamera.demotest.database.localdb.DBOpenHelper.INFO_TABLE_NAME;

public class LocalInfoTable {

    private final String TAG = "LocalInfoTable";
    private SQLiteDatabase mDatabase;

    public void setDatabase(SQLiteDatabase db) {
        mDatabase = db;
    }


    public boolean addFace(String id,String name,String relationship,String priority,String regTime) {
        Log.d(TAG, "addFace: mDatabase"+mDatabase);
        if (mDatabase == null) return false;
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", name);
        cv.put("relationship", relationship);
        cv.put("priority", priority);
        cv.put("regtime", regTime);
        return mDatabase.insert(INFO_TABLE_NAME, null, cv) >= 0;
    }

    public boolean addFace(FaceCardInfo faceCardInfo) {
        if (mDatabase == null) return false;
        ContentValues cv = new ContentValues();
        cv.put("id", faceCardInfo.getId());
        cv.put("name", faceCardInfo.getName());
        cv.put("relationship", faceCardInfo.getFaceRelationShip());
        cv.put("priority", faceCardInfo.getPriority());
        cv.put("regtime", faceCardInfo.getRegtime());
        return mDatabase.insert(INFO_TABLE_NAME, null, cv) >= 0;
    }

    public boolean modifyFace(FaceCardInfo faceCardInfo) {
        if (mDatabase == null) return false;
        ContentValues cv = new ContentValues();
        cv.put("id", faceCardInfo.getId());
        cv.put("name", faceCardInfo.getName());
        cv.put("relationship", faceCardInfo.getFaceRelationShip());
        cv.put("priority", faceCardInfo.getPriority());
        cv.put("regtime", faceCardInfo.getRegtime());
        String where = "id = ? ";
        String[] whereValue = {faceCardInfo.getId()};
        return mDatabase.update(INFO_TABLE_NAME, cv, where, whereValue) >= 0;
    }

    public List<FaceCardInfo> queryFace() {
        Log.d(TAG, String.valueOf(mDatabase == null) );
        if (mDatabase == null) return null;
        Cursor cursor;
        List<FaceCardInfo> faceCardInfoList = new ArrayList<>();
        cursor = mDatabase.query(INFO_TABLE_NAME, null, null,
                null, null, null, null);
        while (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex("id"));
            String name = cursor.getString(cursor.getColumnIndex("name"));
            String relationship = cursor.getString(cursor.getColumnIndex("relationship"));
            String priority = cursor.getString(cursor.getColumnIndex("priority"));
            String regTime = cursor.getString(cursor.getColumnIndex("regtime"));

            FaceCardInfo faceCardInfo = new FaceCardInfo();
            faceCardInfo.setId(id);
            faceCardInfo.setName(name);
            faceCardInfo.setFaceRelationShip(relationship);
            faceCardInfo.setPriority(priority);
            faceCardInfo.setRegtime(regTime);
            faceCardInfoList.add(faceCardInfo);
            Log.d(TAG, "queryFace:size: " + faceCardInfoList.size());
        }
        if (cursor!=null) cursor.close();
        return faceCardInfoList;
    }

    public boolean deleteFace(String id) {
        if (!TextUtils.isEmpty(id)) {
            String where = " id=?";
            String[] whereValue = { id };
            return mDatabase.delete(INFO_TABLE_NAME, where, whereValue) >= 0;
        }
        return false;
    }
}
