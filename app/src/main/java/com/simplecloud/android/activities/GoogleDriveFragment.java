package com.simplecloud.android.activities;

import android.os.Bundle;
import android.os.Message;
import android.text.format.Time;

import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.utils.SharedPrefUtils;
import com.simplecloud.android.services.googledrive.GoogleDriveServiceManager;

public class GoogleDriveFragment extends CloudStorageFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        cloudSMgr = new GoogleDriveServiceManager(getActivity(), mAccId, mHandler);
        
        mDirectory = mFileDAO.getGoogleDriveRootDirectory(mAccId);
        generateActionBarBreadcrumbsNavigation();
        
        new RefreshViewAsync().execute();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Only update if the last update was at least 1 day ago 
        long lastUpdate = SharedPrefUtils.getLastUpdate(getActivity(), mAccId);
        Time t = new Time();
        t.setToNow();
        if (t.toMillis(true) - lastUpdate > 24*60*60*1000) {
            loadDirectoryFromCloud();
        }
    }
    
    @Override
    public boolean handleMessage(Message msg) {
        
        switch (msg.what) {

            case ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION:
                showMessage(ErrorCode.getErrorMessage(msg.what));
                break;

            default:
                super.handleMessage(msg);
                break;
        }
        
        return false;
    }

}
