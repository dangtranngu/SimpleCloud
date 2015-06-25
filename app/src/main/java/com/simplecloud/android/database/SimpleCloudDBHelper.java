package com.simplecloud.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.simplecloud.android.database.SimpleCloudProvider.Account_Column;
import com.simplecloud.android.database.SimpleCloudProvider.File_Column;

public class SimpleCloudDBHelper extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "SimpleCloud.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_FILE = "file";
    public static final String TABLE_ACCOUNT = "account";
    
    public static final String LOGCAT = SimpleCloudDBHelper.class.getSimpleName();
    
    public SimpleCloudDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ACCOUNT + " ( "
            + Account_Column._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Account_Column.DISPLAY_NAME + " TEXT, "
            + Account_Column.ACC_TYPE + " INTEGER, "
            + Account_Column.USER_ID + " TEXT, "
            + Account_Column.ACCESS_SECRET + " TEXT, "
            + "CONSTRAINT ACCOUNT_UNIQUE UNIQUE(" + Account_Column.ACC_TYPE + "," + Account_Column.USER_ID + ") "
            + "ON CONFLICT IGNORE );");
        

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FILE + " ( "
            + File_Column._ID  + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + File_Column.UID + " TEXT, "
            + File_Column.ACC_ID + " INTEGER NOT NULL ,"
            + File_Column.PARENT_UID + " TEXT, "
            + File_Column.NAME + " TEXT, "
            + File_Column.IS_DIR + " INTEGER, "
            + File_Column.MIMETYPE + " TEXT, "
            + File_Column.SIZE + " INTEGER, "
            + File_Column.MODIFIED_DATE + " INTEGER, "
            + File_Column.HASH + " TEXT, "
            + File_Column.BOOKMARK + " INTEGER, "

            + "FOREIGN KEY  (" + File_Column.ACC_ID + ") REFERENCES " + TABLE_ACCOUNT + " (" + Account_Column._ID + ") "
            + "ON DELETE CASCADE, "
            
            + "CONSTRAINT FILE_UNIQUE UNIQUE (" + File_Column.ACC_ID + "," + File_Column.UID + ") "
            + "ON CONFLICT REPLACE );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SimpleCloudDBHelper.class.getName(),
            "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
    
}


