package com.simplecloud.android.activities;

import android.os.Bundle;
import android.os.Message;

import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.DropboxDirectory;
import com.simplecloud.android.services.dropbox.DropboxServiceManager;

public class DropboxFragment extends CloudStorageFragment {

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        cloudSMgr = new DropboxServiceManager(getActivity(), mAccId, mHandler);
        
        mDirectory = new DropboxDirectory();
        mDirectory.setAccId(mAccId);
        mDirectory.setUId(DropboxServiceManager.ROOT_FOLDER);
        generateActionBarBreadcrumbsNavigation();

        new RefreshViewAsync().execute();
    }
    
    @Override
    protected void onDirectoryObjectClick(DirectoryObject dir) {
        updateView(dir);
        
        // first time open the dir
        dir = (DirectoryObject)mFileDAO.getFile(mAccId, AccountType.DROPBOX, dir.getUId());
        if (dir.getHash().equals("")) {
            loadDirectoryFromCloud();
        }
        
    }

    @Override
    public boolean handleMessage(Message msg) {
        
        switch (msg.what) {
            
            case ErrorCode._DROPBOX_EXCEPTION_BAD_GATEWAY:
            case ErrorCode._DROPBOX_EXCEPTION_INSUFFICIENT_STORAGE:
            case ErrorCode._DROPBOX_EXCEPTION_INTERNAL_SERVER_ERR:
            case ErrorCode._DROPBOX_EXCEPTION_SERVICE_UNAVAILABLE:
            case ErrorCode._DROPBOX_EXCEPTION_UNAUTHORIZED:
            case ErrorCode._DROPBOX_EXCEPTION_MAX_UPLOAD_SIZE:
            case ErrorCode._DROPBOX_EXCEPTION:
                showMessage(ErrorCode.getErrorMessage(msg.what));
                break;
                
            default:
                super.handleMessage(msg);
                break;
        }
        
        return false;
    }
    
}
