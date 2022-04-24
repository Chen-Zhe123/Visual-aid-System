package com.ipcamera.demotest.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.ipcamera.demotest.database.clouddb.DBUtils;
import com.ipcamera.demotest.database.clouddb.FaceInfoTable;
import com.ipcamera.demotest.database.clouddb.MyDataSource;
import com.ipcamera.demotest.database.localdb.DBOpenHelper;
import com.ipcamera.demotest.database.localdb.LocalFeatureTable;
import com.ipcamera.demotest.database.localdb.LocalInfoTable;

import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;

public class DBMaster {

    private final String TAG = "DBMaster";
    private Context mContext;
    private int initWhich = -1;
    /**
     * 同时管理本地数据库和远程数据库,本地数据库用作
     * 日常使用,远程数据库用作备份数据,当系统转移至
     * 另一台设备时可导入人脸数据
     */
    @SuppressLint("StaticFieldLeak")
    /**
     * 本地数据库
     */
    private static DBMaster localDB;
    private SQLiteDatabase liteDatabase;
    private DBOpenHelper mDBOpenHelper;
    private LocalInfoTable localInfoTable;
    private LocalFeatureTable localFeatureTable;
    /**
     * 维护远程数据库
     */
    private static DBMaster farDB;
    private QueryRunner queryRunner;
    private FaceInfoTable faceInfoTable;

    /**
     * 按需初始化
     * @return 错误码,见DBErrorCode
     */
    public int init(Context context) {
        mContext = context;
        if(initWhich == 0){
            localInfoTable = new LocalInfoTable();
            localFeatureTable = new LocalFeatureTable();
            openLocalDB();
            return 0;
        }else if(initWhich == 1) {
            DBUtils.initJDBC();
            try {
                DataSource dataSource = new MyDataSource();//
                queryRunner = new QueryRunner(dataSource);
                openTables();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
        return 1;
    }

    public LocalInfoTable getLocalFaceInfoTable() {
        return localInfoTable;
    }
    public LocalFeatureTable getLocalFeatureTable() {
        return localFeatureTable;
    }

    public FaceInfoTable getFarFaceInfoTable() {
         return faceInfoTable;
    }

    private void openLocalDB(){
        mDBOpenHelper = new DBOpenHelper(mContext);
        try {
            liteDatabase = mDBOpenHelper.getWritableDatabase();
        }catch(SQLException e){
            liteDatabase = mDBOpenHelper.getReadableDatabase();
        }
        localInfoTable.setDatabase(liteDatabase);
        localFeatureTable.setDatabase(liteDatabase);
    }

    void openTables(){
        faceInfoTable = new FaceInfoTable(queryRunner);
    }

    public static synchronized DBMaster getLocalDB(){
        if(localDB == null){
            localDB = new DBMaster();
            localDB.initWhich = 0;
        }
        return localDB;
    }

    public static synchronized DBMaster getFarDB(){
        if(farDB == null){
            farDB = new DBMaster();
            localDB.initWhich = 1;
        }
        return farDB;
    }
}