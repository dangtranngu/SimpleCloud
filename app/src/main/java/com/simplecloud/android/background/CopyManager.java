package com.simplecloud.android.background;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;

import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.models.ObjectFactory;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.DropboxDirectory;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.GoogleDriveDirectory;
import com.simplecloud.android.models.files.LocalDirectory;

public class CopyManager {
    
    public enum CopyType {
        LOCAL_COPY, 
        DROPBOX_COPY, DROPBOX_DOWNLOAD, DROPBOX_UPLOAD,
        GOOGLE_DRIVE_COPY, GOOGLE_DRIVE_UPLOAD, GOOGLE_DRIVE_DOWNLOAD,
        NOT_SUPPORTED
    }
    
    public enum CopyState {
        PREPARE,
        IN_PROGRESS,
        IDLE
    }

    private List<FileSystemObject> srcFiles = new ArrayList<FileSystemObject>(); // list of files/directories to copy

    private static CopyManager mCopyManager; // Only maintain 1 instance in the app
    
    private DirectoryObject srcDirectory;
    private DirectoryObject dstDirectory;
    private boolean isMove; // copy or move operation
    
    private CopyType copyType;
    private CopyState state;
    private CopyFilesAsync mAsync;
    
    // unique uid to identify the object currently being copied
    public static final String FILE_IN_PROGRESS_UID = "file_in_progress_uid";
    
    private FileSystemObject fileInProgress; // current file being copied
    private boolean indeterminate; // percentage 
    private long size; // total size of source files
    private int error;
    
    
    public CopyManager() {
        state = CopyState.IDLE;
    }
    
    public static CopyManager getCopyManager() {
        if (mCopyManager == null)
            mCopyManager = new CopyManager();
        return mCopyManager;
    }

    public void prepare(DirectoryObject srcDirectory,
            List<FileSystemObject> srcFiles,  DirectoryObject dstDirectory, boolean isMove) {
        
        size = -1; // size is not set
        indeterminate = false;
        
        this.srcDirectory = (DirectoryObject)ObjectFactory.clone(srcDirectory);
        this.srcFiles = srcFiles;
        this.isMove = isMove;
        
        state = CopyState.PREPARE;
        
        if (dstDirectory == null) // destination is not set
            return;
        
        this.dstDirectory = (DirectoryObject)ObjectFactory.clone(dstDirectory);
        
        copyType = CopyType.NOT_SUPPORTED;
        
        if (srcDirectory instanceof LocalDirectory) {
            
            if (dstDirectory instanceof LocalDirectory) {
                copyType = CopyType.LOCAL_COPY;
                
            } else if (dstDirectory instanceof GoogleDriveDirectory) {
                copyType = CopyType.GOOGLE_DRIVE_UPLOAD;
                indeterminate = true;
                
            } else if (dstDirectory instanceof DropboxDirectory) {
                copyType = CopyType.DROPBOX_UPLOAD;
            }
            
        } else if (srcDirectory instanceof GoogleDriveDirectory) {
            
            if (dstDirectory instanceof LocalDirectory) {
                copyType = CopyType.GOOGLE_DRIVE_DOWNLOAD;
                
            } else if (dstDirectory instanceof GoogleDriveDirectory) {
                copyType = CopyType.GOOGLE_DRIVE_COPY;
                indeterminate = true;
            }
            
        } else if (srcDirectory instanceof DropboxDirectory) {
            
            if (dstDirectory instanceof LocalDirectory) {
                copyType = CopyType.DROPBOX_DOWNLOAD;

            } else if (dstDirectory instanceof DropboxDirectory) {
                copyType = CopyType.DROPBOX_COPY;
            }
        }
    }

    public void start(CopyFilesAsync async) {
        mAsync = async;
        state = CopyState.IN_PROGRESS;
        error = ErrorCode._NO_ERROR;
        mAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public void finish() {
        state = CopyState.IDLE;
    }
    
    public void finish(int error) {
        state = CopyState.IDLE;
        this.error = error;
    }
    
    public void cancel() {
        if (state == CopyState.IN_PROGRESS) {
            mAsync.cancel(true);
        }
        finish();
    }

    public List<FileSystemObject> getSourceFiles() {
        return srcFiles;
    }

    public DirectoryObject getSource() {
        return srcDirectory;
    }
    
    public DirectoryObject getDestination() {
        return dstDirectory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public CopyType getCopyType() {
        return copyType;
    }

    public CopyState getState() {
        return state;
    }

    public FileSystemObject getFileInProgress() {
        return fileInProgress;
    }
    
    public boolean isMove() {
        return isMove;
    }

    /**
     * Identify the in-progress file/directory. 
     * The listview item associated with this file will show the current progress and size.
     * @param srcFile  source file that are being copied
     */
    public void setFileInProgress(FileSystemObject srcFile) {
        if (srcFile instanceof DirectoryObject)
            fileInProgress = new DirectoryObject();
        else 
            fileInProgress = new FileObject();
        
        fileInProgress.setName(srcFile.getName());
        fileInProgress.setSize(srcFile.getSize());
        fileInProgress.setIconId(srcFile.getIconId());
        fileInProgress.setUId(FILE_IN_PROGRESS_UID);
    }

    public int getError() {
        return error;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }


}
