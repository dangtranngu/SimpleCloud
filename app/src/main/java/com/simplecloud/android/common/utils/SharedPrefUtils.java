package com.simplecloud.android.common.utils;


import java.io.File;

import android.content.Context;
import android.preference.PreferenceManager;


public class SharedPrefUtils {

    public static final String DEFAULT_DOWNLOAD_DIR = "DEFAULT_DOWNLOAD_DIR";
    public static final String LAST_UPDATE = "LAST_UPDATE_OF_ACCOUNT_";
    public static final String LARGEST_GD_CHANGE_ID = "LARGEST_GOOGLE_DRIVE_CHANGE_ID_OF_ACCOUNT_";
    
    public static void setDownloadDirectory(Context context, File defaultDir) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
            DEFAULT_DOWNLOAD_DIR, defaultDir.getAbsolutePath()).commit();
    }
    
    public static String getDownloadDirectory(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(DEFAULT_DOWNLOAD_DIR, FileUtils.getSDCardDownloadDirectory().getAbsolutePath());
    }
    
    public static void setLastUpdate(Context context, int accId, long lastUpdate) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(
            LAST_UPDATE + accId, lastUpdate).commit();
    }
    
    public static long getLastUpdate(Context context, int accId) {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(LAST_UPDATE + accId, 0L);
    }
    
    public static void setLatestGoogleDriveChangeId(Context context, int accId, long largestChangeId) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(
            LARGEST_GD_CHANGE_ID + accId, largestChangeId).commit();
    }
    
    public static long getLatestGoogleDriveChangeId(Context context, int accId) {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(LARGEST_GD_CHANGE_ID + accId, 0L);
    }
    
}
