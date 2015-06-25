package com.simplecloud.android.services;

import java.util.List;

import android.content.Context;
import android.os.Handler;

import com.simplecloud.android.database.AccountDAO;
import com.simplecloud.android.database.FileSystemObjectDAO;
import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.services.dropbox.DropboxServiceManager;
import com.simplecloud.android.services.googledrive.GoogleDriveServiceManager;

public abstract class AbstractServiceManager implements IAuthenticator,IAccountManager, IFileManager {
    
    protected Context mContext;
    protected Handler mHandler;
    protected Account mAccount;
    protected AccountDAO mAccountDAO;
    protected FileSystemObjectDAO mFileDAO;
    
    public AbstractServiceManager(Context context){
        this.mContext = context;
        mAccountDAO = new AccountDAO(context);
        mFileDAO = new FileSystemObjectDAO(context);
    }

    public AbstractServiceManager(Context context, Handler handler, int accId){
        this(context);
        this.mHandler = handler;
        mAccount = mAccountDAO.getAccountById(accId);
    }
    
    @Override
    public void loadDirectoryFromDatabase(DirectoryObject directory) {
        AccountType accType = null;
        if (this instanceof GoogleDriveServiceManager) {
            accType = AccountType.GOOGLEDRIVE;
        } else if (this instanceof DropboxServiceManager) {
            accType = AccountType.DROPBOX;
        }
        
        FileSystemObject temp = mFileDAO.getFile(
            mAccount.getId(), accType, directory.getUId());
        
        if (temp == null || !(temp instanceof DirectoryObject)) {
            return;
        }
                
        directory.setAll((DirectoryObject)temp);
                
        List<FileSystemObject> children = mFileDAO.getChildren(
            mAccount.getId(), accType, directory.getUId());
    
        directory.getChildren().clear();
        directory.getChildren().addAll(children);
        
    }
    
    public abstract void cancelAllBackgroundTasks();
    
}