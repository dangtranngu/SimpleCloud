package com.simplecloud.android.activities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.EditText;

import com.simplecloud.android.adapters.FileSystemObjectAdapter;
import com.simplecloud.android.background.CopyManager;
import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.ZipUtils;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;
import com.simplecloud.android.services.dropbox.DropboxServiceManager;
import com.simplecloud.android.services.googledrive.GoogleDriveServiceManager;

public class HomeFragment extends NavigationFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDirectory = new LocalDirectory(FileUtils.getSDCardDirectory());
        mAdapter = new FileSystemObjectAdapter(getActivity(), mChildren);
        refreshView();
        generateActionBarBreadcrumbsNavigation();
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
    protected List<BreadcrumbsItem> getBreadcrumbsItems() {
        List<BreadcrumbsItem> items = new ArrayList<BreadcrumbsItem>();
    
        File parent = ((LocalDirectory) mDirectory).getFile();
        BreadcrumbsItem item = new BreadcrumbsItem(0, parent.getPath(), parent.getName());
        while(parent != null) {
            item = new BreadcrumbsItem(0, parent.getPath(), parent.getName());
            items.add(item);
            parent = parent.getParentFile();
        }
        
        Collections.reverse(items);
        return items;
    }

    @Override
    protected void onActionBarBreadcrumbsItemClick(BreadcrumbsItem item) {
        if (!item.getUId().equals(mDirectory.getUId())) {
            updateView(new LocalDirectory(new File(item.getUId())));
        }  
    }

    @Override
    protected void onDirectoryObjectClick(DirectoryObject dir) {
        LocalDirectory d = new LocalDirectory(new File(dir.getUId()));
        if (d.isReadable()) {
            updateView(d);
        } else {
            showMessage("Accessed denied");
        }
    }

    @Override
    protected void onFileObjectClick(FileObject file) {
        File f = new File(file.getUId());
        if (f.getName().toLowerCase().endsWith(".zip")) {
            new UnzipFileAsync(f).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            openFile(f);
        }
    }

    @Override
    protected void pasteObjects() {
        switch (CopyManager.getCopyManager().getCopyType()) {
            case LOCAL_COPY:
                mCopyProcess.start(new FileUtils.LocalCopyFilesAsync(getActivity(), mAdapter, mHandler));
                break;

            case GOOGLE_DRIVE_DOWNLOAD:
                GoogleDriveServiceManager gDriveSmgr = new GoogleDriveServiceManager(
                    getActivity(), mCopyProcess.getSource().getAccId(), mHandler);
                gDriveSmgr.copyFiles(mAdapter);
                break;

            case DROPBOX_DOWNLOAD:
                DropboxServiceManager dbSmgr = new DropboxServiceManager(
                    getActivity(), mCopyProcess.getSource().getAccId(), mHandler);
                dbSmgr.copyFiles(mAdapter);
                break;
                
            default:
                break;
        }
    }

    @Override
    protected int createNewDirectoryObject(String name) {
        File f = new File(((LocalDirectory) mDirectory).getFile(), name);
        if (f.mkdir()) {
            mDirectory.getChildren().add(new LocalDirectory(f));
            return ErrorCode._NO_ERROR;
        }
        return ErrorCode._DIRECTORY_ALREADY_EXIST;
    }

    @Override
    protected int createNewFileObject(String name) {
        File f = new File(((LocalDirectory) mDirectory).getFile(), name);
        try {
            if (f.createNewFile()) {
                mDirectory.getChildren().add(new LocalFile(f));
                return ErrorCode._NO_ERROR;
            }
        } catch (IOException e) {
            Log.e(LOGCAT, "Fail to create file", e);
        }
        return ErrorCode._FILE_ALREADY_EXIST;
    }

    @Override
    protected int deleteEmptyDirectoryObject(DirectoryObject object) {
        boolean isSuccess = ((LocalDirectory) object).getFile().delete() ;
        return isSuccess ? ErrorCode._NO_ERROR : ErrorCode._FILE_CAN_NOT_BE_DELETED;
    }

    @Override
    protected int deleteFileObject(FileObject object) {
        boolean isSuccess =  ((LocalFile) object).getFile().delete();  
        return isSuccess ? ErrorCode._NO_ERROR : ErrorCode._FILE_CAN_NOT_BE_DELETED;
    }

    @Override
    protected int renameDirectoryObject(DirectoryObject directory, String newName) {
        File f = ((LocalDirectory) directory).getFile();
        if (f.renameTo(new File(f.getParentFile(), newName))) {
            return ErrorCode._NO_ERROR;
        }
        return ErrorCode._FILE_CAN_NOT_BE_RENAMED;
    }

    @Override
    protected int renameFileObject(FileObject file, String newName) {
        File f = ((LocalFile) file).getFile();
        if (f.renameTo(new File(f.getParentFile(), newName))) {
            return ErrorCode._NO_ERROR;
        }
        return ErrorCode._FILE_CAN_NOT_BE_RENAMED;
    }

    @Override
    protected void bookmarkFileOrDirectoryObject(FileSystemObject file) {
        if (file instanceof LocalDirectory) {
            mFileDAO.insertLocalBookmark(((LocalDirectory) file).getFile());
        } else
            mFileDAO.insertLocalBookmark(((LocalFile) file).getFile());
    }

    @Override
    protected Properties getObjectProperties(FileSystemObject object) {
        final Properties properties;
        String name = object.getName();
        
        if (object instanceof LocalDirectory) {
            final File d = ((LocalDirectory) object).getFile();
            properties = new Properties(name, -1, d.lastModified());
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Size is calculated in the background
                    properties.setSize(FileUtils.getDirectorySize(d));
                }
            });
            t.start();
        } else {
            File f = ((LocalFile) object).getFile();
            properties = new Properties(name, f.length(), f.lastModified());
        }

        return properties;
    }

    @Override
    protected void shareFileOrDirectoryObject(FileSystemObject file) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        
        File f = null;
        if (file instanceof LocalFile) {
            f = ((LocalFile) file).getFile();
            
        } else if (file instanceof LocalDirectory) {
            Log.d(LOGCAT, "sharing folder is not supported");
            showMessage("Sharing folder is currently not supported");
            return;
            
        } else {
            return;
        }
        
        Uri uri = Uri.fromFile(f);
        
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(uri.toString().toLowerCase()));
        shareIntent.setType(type);
        startActivity(Intent.createChooser(shareIntent, null));
    }

    @Override
    protected void zipFileOrDirectoryObjects(List<FileSystemObject> files, String newName) {
        List<File> localFiles = new ArrayList<File>();
        
        for (FileSystemObject f : files) {
            if (f instanceof LocalFile) {
                localFiles.add(((LocalFile)f).getFile());
            } else if (f instanceof LocalDirectory) {
                localFiles.add(((LocalDirectory)f).getFile());
            }
        }
        ZipUtils.zipFiles(localFiles, mDirectory.getUId() + File.separator + newName);
        
    }

    class UnzipFileAsync extends AsyncTask<Void, Void, Integer> {

        private File file;
        
        public UnzipFileAsync(File file) {
            this.file = file;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Unzipping ...");
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return ZipUtils.unZipFiles(file, mDirectory.getUId());
        }

        @Override
        protected void onPostExecute(Integer err) {
            if ((mProgressDialog != null) && mProgressDialog.isShowing()) { 
                mProgressDialog.dismiss();
            }
            
            if (err == ErrorCode._NO_ERROR) 
                refreshView();
            else
                showMessage(ErrorCode.getErrorMessage(err));
        }

    }

 

}
