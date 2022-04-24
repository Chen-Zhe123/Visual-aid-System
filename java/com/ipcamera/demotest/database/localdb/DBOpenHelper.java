package com.ipcamera.demotest.database.localdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "liteDataBase";
    private static final int VERSION = 1;
    private static final String CREATE_TABLE_START_SQL = "CREATE TABLE IF NOT EXISTS ";
    public static final String INFO_TABLE_NAME = "info";
    public static final String FEATURE_TABLE_NAME = "feature";

    private final String createFaceInfoTableStr =
            CREATE_TABLE_START_SQL+INFO_TABLE_NAME + " ( " +
                    " id" + " varchar(18) default \"\" ," +
                    " name"+" varchar(18) default \"\" ,"+
                    " relationship" + " varchar(18) default \"\" ,"+
                    " priority" + " varchar(18) default \"\" ,"+
                    " regtime" + " varchar(24) default \"\" )";

    private final String createFaceFeatureTableStr =
            CREATE_TABLE_START_SQL+INFO_TABLE_NAME + " ( " +
                    " id" + " varchar(18) default \"\" ," +
                    " feature" + " blob  ,"+
                    " portrait" + " blob  )";

    public DBOpenHelper(Context context){
        super(context,DB_NAME,null,VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createFaceInfoTableStr);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
