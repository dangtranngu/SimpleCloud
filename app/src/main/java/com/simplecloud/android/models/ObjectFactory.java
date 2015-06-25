package com.simplecloud.android.models;

import java.io.File;

import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.accounts.DropboxAccount;
import com.simplecloud.android.models.accounts.GoogleDriveAccount;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.DropboxDirectory;
import com.simplecloud.android.models.files.DropboxFile;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.GoogleDriveDirectory;
import com.simplecloud.android.models.files.GoogleDriveFile;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;


public class ObjectFactory {

    public static FileSystemObject clone(FileSystemObject obj) {

        if (obj instanceof LocalDirectory) {
            File f = new File(obj.getUId());
            return new LocalDirectory(f);
            
        } else if (obj instanceof DropboxDirectory) {
            return new DropboxDirectory((DropboxDirectory) obj);
            
        } else if (obj instanceof GoogleDriveDirectory) {
            return new GoogleDriveDirectory((GoogleDriveDirectory) obj);
            
        } else if (obj instanceof LocalFile) {
            File f = new File(obj.getUId());
            return new LocalFile(f);
            
        } else if (obj instanceof DropboxFile) {
            return new DropboxFile((DropboxFile) obj);
            
        } else if (obj instanceof GoogleDriveFile) {
            return new GoogleDriveFile((GoogleDriveFile) obj);
        }

        return null;
    }
    
    public static Account generateNewAccount(AccountType accType) {
        switch (accType) {
            case GOOGLEDRIVE:
                return new GoogleDriveAccount();
                
            case DROPBOX:
                return new DropboxAccount();

            default:
                break;
        }

        return null;
    }
    
    public static FileSystemObject generateNewFileSystemObject(AccountType accType, boolean isDir) {
        if (accType == null) {
            if (isDir)
                return new DirectoryObject();
            else
                return new FileObject();
        }
        
        switch (accType) {
            case GOOGLEDRIVE:
                return isDir == true ? new GoogleDriveDirectory() : new GoogleDriveFile();
                
            case DROPBOX:
                return isDir == true ? new DropboxDirectory() : new DropboxFile();
                
            default:
                break;
        }

        return null;
    }
}
