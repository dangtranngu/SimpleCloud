package com.simplecloud.android.background;

import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.simplecloud.android.background.CopyManager.CopyType;
import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.TaskCode;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;

public abstract class CopyFilesAsync extends AsyncTask<Void, Void, Integer> {

    protected static final String LOGCAT = CopyFilesAsync.class.getSimpleName();
    public static final int UPLOAD_MAX_SIZE = 1024*1024*1024; // 1GB
    public static final int DOWNLOAD_MAX_SIZE = 1024*1024*1024; // 1GB
    
    protected Activity mActivity;
    protected OnListViewProgressBarUpdateListener mAdapterListener;
    protected OnDrawerProgressBarUpdateListener mDrawerListener;
    protected Handler mHandler; // Communicate with the main thread
    
    protected CopyManager mCopyProcess = CopyManager.getCopyManager();
    protected List<FileSystemObject> mSrcFiles;
    protected DirectoryObject mDestination;
    
    protected long mSize; // size of a file/directory to be copied/moved
    protected long mTotalSize; // total bytes to be copied/moved
    protected int refreshViewTaskCode = TaskCode._REFRESH_COPY_DESTINATION_VIEW;
    

    public CopyFilesAsync(Activity mActivity, 
            OnListViewProgressBarUpdateListener mAdapterListener, Handler mHandler) {
        this.mActivity = mActivity;
        this.mSrcFiles = mCopyProcess.getSourceFiles();
        this.mDestination = mCopyProcess.getDestination();
        this.mAdapterListener = mAdapterListener;
        this.mDrawerListener = (OnDrawerProgressBarUpdateListener) mActivity;
        this.mHandler = mHandler;
        
        if (mCopyProcess.getCopyType() == CopyType.GOOGLE_DRIVE_DOWNLOAD
                || mCopyProcess.getCopyType() == CopyType.DROPBOX_DOWNLOAD) {
            
            refreshViewTaskCode = TaskCode._REFRESH_DOWNLOAD_DIRECTORY_VIEW;
        }
    }
    
    
    @Override
    protected void onPreExecute() {
        mDrawerListener.onPrepareProgressBar();
    }

    @Override
    protected Integer doInBackground(Void... unused) {
        
        // Get total size of copied files/directories
        if (mCopyProcess.getCopyType() == CopyType.LOCAL_COPY
                || mCopyProcess.getCopyType() == CopyType.GOOGLE_DRIVE_UPLOAD
                || mCopyProcess.getCopyType() == CopyType.DROPBOX_UPLOAD) {
            
        
            for (FileSystemObject f : mSrcFiles) {
                long s = 0;
                if (f instanceof LocalFile) {
                    s = ((LocalFile) f).getFile().length();
                } else if (f instanceof LocalDirectory) {
                    s = FileUtils.getDirectorySize(((LocalDirectory) f).getFile());
                }
                f.setSize(s);
                mTotalSize += s;
            }
        }
        
        if (mCopyProcess.getCopyType() == CopyType.GOOGLE_DRIVE_DOWNLOAD
                || mCopyProcess.getCopyType() == CopyType.DROPBOX_DOWNLOAD) {

            if (mTotalSize > FileUtils.MAX_DOWNLOAD_SIZE) {
                cancel(true);
                mHandler.sendEmptyMessage(ErrorCode._UPLOAD_LIMIT_EXCEEDED);
                return null;
            }
        }

        if (mCopyProcess.getCopyType() == CopyType.GOOGLE_DRIVE_UPLOAD
                || mCopyProcess.getCopyType() == CopyType.DROPBOX_UPLOAD) {

            if (mTotalSize > FileUtils.MAX_UPLOAD_SIZE) {
                cancel(true);
                mHandler.sendEmptyMessage(ErrorCode._DOWNLOAD_LIMIT_EXCEEDED);
                return null;
            }
        }

        
        mCopyProcess.setSize(mTotalSize);

        publishProgress();
        
        for (final FileSystemObject f : mSrcFiles) {
            mSize = f.getSize();
            mCopyProcess.setFileInProgress(f);
            mAdapterListener.onPrepareProgressBar(mSize);
            
            // Refresh copy destination view -> progress bar will be available
            mHandler.sendEmptyMessage(refreshViewTaskCode);
            
            Log.d(LOGCAT, "Copy '" + f.getName() + "' -> '" + mDestination.getName() + "'");
            int err = copy(f);
            
            if (err != ErrorCode._NO_ERROR) {
                return err;
            }
            
            if (isCancelled()) 
                break;
        }
        
        return ErrorCode._NO_ERROR;
    }
    
    @Override
    protected void onProgressUpdate(Void... values) {
        mDrawerListener.onStartProgressBar();
    }

    /* 
     * Trigger on pressing Cancel button in navigation drawer
     * @see android.os.AsyncTask#onCancelled()
     */
    @Override
    protected void onCancelled() {
        mCopyProcess.finish();
        mHandler.sendEmptyMessage(refreshViewTaskCode);
    }
    
    @Override
    protected void onPostExecute(Integer err) {
        if (err == ErrorCode._NO_ERROR) {
            mCopyProcess.finish();
            mHandler.sendEmptyMessage(TaskCode._SHOW_MESSAGE_COPY_FINISHED);
        } else {
            Toast.makeText(mActivity, ErrorCode.getErrorMessage(err), Toast.LENGTH_SHORT).show();
            mCopyProcess.finish(err);
        }
        
        mDrawerListener.onFinishedProgress(err);
        mHandler.sendEmptyMessage(refreshViewTaskCode);
        
    }

    /**
     * Copy a single file or a directory
     * @param srcFile
     * @return the error code
     */
    protected abstract int copy(FileSystemObject srcFile);

    
    /**
     * Communicate with Progress bar of the list view item, 
     * only visible when copying a file/directory
     */
    public interface OnListViewProgressBarUpdateListener {
        /**
         * Increase the progress by rate (%)
         */
        void onIncrementProgress(float rate);
        void onPrepareProgressBar(long max);
        void onFinishedProgress();
    }

    
    public interface OnDrawerProgressBarUpdateListener {
        /**
         * Increase the progress by rate (%)
         */
        void onIncrementProgress(float rate);
        
        /** 
         * Initilize the progress bar on the navigation drawer. Must be implemented on UI Thread
         */
        void onPrepareProgressBar();
        void onStartProgressBar();
        void onFinishedProgress(int err);
    }
}
