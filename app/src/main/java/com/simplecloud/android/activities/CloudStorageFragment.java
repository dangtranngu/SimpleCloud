package com.simplecloud.android.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.TaskCode;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.NetworkUtils;
import com.simplecloud.android.models.ObjectFactory;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.services.AbstractServiceManager;

public abstract class CloudStorageFragment extends NavigationFragment {

    protected AbstractServiceManager cloudSMgr;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSwipeRefreshLayout.setEnabled(true);
        mSwipeRefreshLayout.removeAllViews();
        mSwipeRefreshLayout.addView(getListView());

        mSwipeRefreshLayout.setColorScheme(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_red_dark);

        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public boolean onBackPressed() {
        if (super.onBackPressed()) {
            refreshView();
            return true;
        }
        return false;
    }

    @Override
    public void onRefresh() {
        Log.d(LOGCAT, "onRefresh() " + mDirectory.getName());
        cloudSMgr.loadDirectoryFromCloud(mDirectory);
    }

    /* (non-Javadoc)
     * Get files and subdirectories from database in background and update the view
     */
    @Override
    public void updateView(DirectoryObject dir) {
        mPrevDirectories.push((DirectoryObject)ObjectFactory.clone(mDirectory));
        mDirectory.setUId(dir.getUId());
        generateActionBarBreadcrumbsNavigation();
        new RefreshViewAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        switch (msg.what) {
            case TaskCode._REFRESH_COPY_DESTINATION_VIEW:
                if (mCopyProcess.getDestination().equals(mDirectory)) {
                    new RefreshViewAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                break;
                
            case TaskCode._STOP_REFRESH:
                mSwipeRefreshLayout.setRefreshing(false);
                break;
                
            case ErrorCode._UPLOAD_LIMIT_EXCEEDED:
            case ErrorCode._DOWNLOAD_LIMIT_EXCEEDED:
            case ErrorCode._NO_INTERNET_CONNECTION:
                showMessage(ErrorCode.getErrorMessage(msg.what));
                break;
                
            default:
                super.handleMessage(msg);
                break;
        }
        
        return false;
    }

    @Override
    protected List<BreadcrumbsItem> getBreadcrumbsItems() {
        List<BreadcrumbsItem> items = new ArrayList<BreadcrumbsItem>();
        DirectoryObject parent = (DirectoryObject) mFileDAO
            .getFile(mAccId, getAccountType(), mDirectory.getUId());
        
        //TODO: why NullPointerException
        if (parent == null)
            return items;
        
        do {
            BreadcrumbsItem item = new BreadcrumbsItem(parent);
            items.add(item);
            parent = (DirectoryObject) mFileDAO
                .getFile(mAccId, getAccountType(), parent.getParentUId());
        } while (parent != null);
        
        Collections.reverse(items);
        return items;
    }
    
    @Override
    protected void onActionBarBreadcrumbsItemClick(BreadcrumbsItem item) {
        DirectoryObject dir = (DirectoryObject) mFileDAO.getFile(
            mAccId, getAccountType(), item.getUId());
        
        if (dir.equals(mDirectory))
            return; 
        
        updateView(dir);
    }
    
    @Override
    protected void onFileObjectClick(FileObject f) {
        f = (FileObject)mFileDAO.getFile(mAccId, getAccountType(), f.getUId());
        
        File cache = FileUtils.getCacheFile(mAccId, f);
        if (cache.exists()) {
            if (cache.lastModified() > f.getLastModifiedTime()) {
                openFile(cache);
                return;
            }
        }
        
        if (f.getSize() > FileUtils.MAX_DOWNLOAD_SIZE) {
            showMessage("Large file is currently not supported " +
            		"(" + FileUtils.readableFileSize(FileUtils.MAX_DOWNLOAD_SIZE) + ")");
        }
        
        if (!NetworkUtils.isConnected(getActivity())) {
            showMessage("No internet connection");
            return;
        }
        new OpenFileAsync(f).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    protected void loadDirectoryFromCloud() {
        if (!NetworkUtils.isConnected(getActivity())) {
            showMessage("No internet connection");
            return;
        }
        mSwipeRefreshLayout.setRefreshing(true);
        cloudSMgr.loadDirectoryFromCloud(mDirectory);
    }
    
    @Override
    protected void onDirectoryObjectClick(DirectoryObject dir) {
        updateView(dir);
    }
    
    @Override
    protected void pasteObjects() {
        cloudSMgr.copyFiles(mAdapter);
    }

    @Override
    protected int deleteEmptyDirectoryObject(DirectoryObject directory) {
        if (!NetworkUtils.isConnected(getActivity())) {
            return ErrorCode._NO_INTERNET_CONNECTION;
        }

        return cloudSMgr.deleteEmptyDirectory(directory);
    }

    @Override
    protected int deleteFileObject(FileObject file) {
        if (!NetworkUtils.isConnected(getActivity())) {
            return ErrorCode._NO_INTERNET_CONNECTION;
        }

        return cloudSMgr.deleteFile(file);
    }

    @Override
    protected int createNewFileObject(String newFileName) {
        if (!NetworkUtils.isConnected(getActivity())) {
            return ErrorCode._NO_INTERNET_CONNECTION;
        }

        return cloudSMgr.createEmptyFile(mDirectory, newFileName);
    }
    
    @Override
    protected int createNewDirectoryObject(final String name) {
        if (!NetworkUtils.isConnected(getActivity())) {
            return ErrorCode._NO_INTERNET_CONNECTION;
        }

        return cloudSMgr.createEmptyFolder(mDirectory, name);
    }

    @Override
    protected int renameDirectoryObject(DirectoryObject directory, String newName) {
        if (!NetworkUtils.isConnected(getActivity())) {
            return ErrorCode._NO_INTERNET_CONNECTION;
        }

        return cloudSMgr.renameDirectory(directory, newName);
    }

    @Override
    protected int renameFileObject(FileObject file, String newName) {
        if (!NetworkUtils.isConnected(getActivity())) {
            return ErrorCode._NO_INTERNET_CONNECTION;
        }

        return cloudSMgr.renameFile(file, newName);
    }

    @Override
    protected void shareFileOrDirectoryObject(FileSystemObject file) {
        if (!NetworkUtils.isConnected(getActivity())) {
            showMessage(ErrorCode.getErrorMessage(ErrorCode._NO_INTERNET_CONNECTION));
            return;
        }

        new ShareLinkAsync(file).execute();
    }

    @Override
    protected void bookmarkFileOrDirectoryObject(FileSystemObject file) {
        mFileDAO.setBookmark(mAccId, file.getUId(), true);
    }

    @Override
    protected void zipFileOrDirectoryObjects(List<FileSystemObject> files, String newName) {
        // TODO: not implemented
        
    }

    @Override
    public void cancelAllBackgroundTasks() {
        super.cancelAllBackgroundTasks();
        cloudSMgr.cancelAllBackgroundTasks();
    }

    
    @Override
    protected Properties getObjectProperties(FileSystemObject file) {
        final Properties properties;
        
        final FileSystemObject f = mFileDAO.getFile(mAccId, getAccountType(), file.getUId());
        String name = file.getName();
        
        if (file instanceof DirectoryObject) {
            
            properties = new Properties(name, -1, f.getLastModifiedTime());
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Size is calculated in the background
                    properties.setSize(getDirectorySize((DirectoryObject)f));
                }
            });
            t.start();
        } else {
            properties = new Properties(name, file.getSize(), file.getLastModifiedTime());
        }

        return properties;
    }

    protected long getDirectorySize(DirectoryObject directory) {
        long length = 0;
        cloudSMgr.loadDirectoryFromDatabase(directory);
        for (FileSystemObject f : directory.getChildren()) {
            if (f instanceof FileObject)
                length += f.getSize();
            else
                length += getDirectorySize((DirectoryObject) f);
        }
        return length;
    }
    
    protected class ShareLinkAsync extends AsyncTask<Void, Void, String> {

        private FileSystemObject file;
        
        public ShareLinkAsync(FileSystemObject file) {
            this.file = file;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Generate link ...");
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(true);
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            return cloudSMgr.shareLink(file);
        }

        @Override
        protected void onPostExecute(String link) {
            if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                mProgressDialog.dismiss();
            }

            if (link != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, link);
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, null));
            } else {
                showMessage("Unable to generate link");
            }
        }

    }
    
    /**
     * Refresh view requires query children list of the current directory from database.
     * @author ngu
     *
     */
    protected class RefreshViewAsync extends AsyncTask<Void, Void, Void> {
        
        @Override
        protected Void doInBackground(Void... params) {
            cloudSMgr.loadDirectoryFromDatabase(mDirectory);
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            refreshView();
        }
    }

    /**
     * Download to cache and open the file
     * @author ngu
     */
    class OpenFileAsync extends AsyncTask<Void, Void, Integer> {

        private FileObject file;
        
        public OpenFileAsync(FileObject file) {
            this.file = file;
        }
        
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(true);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax((int)file.getSize());
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                
                @Override
                public void onCancel(DialogInterface dialog) {
                    OpenFileAsync.this.cancel(true);
                }
            });
            mProgressDialog.setProgressNumberFormat(FileUtils.readableFileSize(file.getSize()));
            mProgressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... unused) {
            
            return cloudSMgr.downloadFileToCache(file, mProgressDialog);
        }
        
        @Override
        protected void onCancelled() {
            if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                mProgressDialog.dismiss();
            }
        }

        @Override
        protected void onPostExecute(Integer err) {
            if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                mProgressDialog.dismiss();
            }
            if (err == ErrorCode._NO_ERROR) {
                openFile(FileUtils.getCacheFile(mAccId, file));
            } else {
                FileUtils.getCacheFile(mAccId, file).delete();
                showMessage(ErrorCode.getErrorMessage(err));
            }
        }
    }
}


