package com.simplecloud.android.services.dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.session.AppKeyPair;
import com.simplecloud.android.background.CopyFilesAsync;
import com.simplecloud.android.background.CopyFilesAsync.OnListViewProgressBarUpdateListener;
import com.simplecloud.android.background.CopyManager;
import com.simplecloud.android.background.CopyManager.CopyType;
import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.common.TaskCode;
import com.simplecloud.android.common.ui.BlockingOnUIRunnable;
import com.simplecloud.android.common.ui.BlockingOnUIRunnable.BlockingOnUIRunnableListener;
import com.simplecloud.android.common.utils.FileUtils;
import com.simplecloud.android.common.utils.NetworkUtils;
import com.simplecloud.android.models.ObjectFactory;
import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.accounts.DropboxAccount;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.DropboxDirectory;
import com.simplecloud.android.models.files.DropboxFile;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;
import com.simplecloud.android.services.AbstractServiceManager;

public class DropboxServiceManager extends AbstractServiceManager {
    
	private static final String LOGCAT = DropboxServiceManager.class.getSimpleName();
    private static final String APP_KEY = "u0hi3vp6wmewwcn";
    private static final String APP_SECRET = "fqmuo0g01t0671n";

    private static final int DROPBOX_FILE_LIMIT = 1000;
    public static final String ROOT_FOLDER = "/";
    
    private DropboxAPI<AndroidAuthSession> mApi;
    private RetrieveFilesAsync mRetrieveFilesAsync;

    
    public DropboxServiceManager(Context context) {
        super(context);
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        mApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    public DropboxServiceManager(Context context, int accId, Handler mHandler) {
        super(context, mHandler, accId);
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        session.setOAuth2AccessToken(mAccount.getAccessSecret());
        mApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    @Override
    public void login() {
        mApi.getSession().startOAuth2Authentication(mContext);
    }

    @Override
    public void logout() {
        mApi.getSession().unlink();
    }

    /**
     * Check status of dropbox connection.
     */
    @Override
    public boolean isLogin() {
        return true;
    }

    /**
     * Get information from dropbox acc and save in database
     */
    @Override
    public void addAccount(String accName) {
        final ProgressDialog mProgressDialog = ProgressDialog.show(
            mContext, "Please wait ...", "Updating Dropbox data", true, true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                AndroidAuthSession session = mApi.getSession();
                if (session.authenticationSuccessful()) {
                    try {
                        session.finishAuthentication();

                        Account acc = new DropboxAccount(mApi.accountInfo(), session.getOAuth2AccessToken());

                        Entry root = mApi.metadata(ROOT_FOLDER, DROPBOX_FILE_LIMIT, null, true, null);
                        
                        // Check if user already cancel the dialog
                        if (!mProgressDialog.isShowing())
                            return;
                        
                        int accId = mAccountDAO.insertAccount(acc);
                        
                        if (accId == 0) // fail to insert. Account already exists
                            return;
                        
                        DropboxDirectory rootDirectory = new DropboxDirectory(accId, root);
                        
                        mFileDAO.insertOrUpdateFile(rootDirectory);
                        for (Entry entry : root.contents) {
                            FileSystemObject child = null;
                            if (entry.isDir) {
                                child = new DropboxDirectory(accId, entry);
                            } else
                                child = new DropboxFile(accId, entry);

                            mFileDAO.insertOrUpdateFile(child);
                        }
                        
                        BlockingOnUIRunnable r = new BlockingOnUIRunnable((Activity) mContext, 
                            new BlockingOnUIRunnableListener() {
                            
                                @Override
                                public void onRunOnUIThread() {
                                    Toast.makeText(mContext, 
                                        "Account added successfully", Toast.LENGTH_SHORT).show();
                                }
                            }
                        );
                        r.startOnUiAndWait();
                        
                    } catch (IllegalStateException e) {
                        Log.e(LOGCAT, "Couldn't authenticate with Dropbox", e);
                    } catch (DropboxException e) {
                        Log.e(LOGCAT, "Fail to get account information", e);
                        BlockingOnUIRunnable r = new BlockingOnUIRunnable((Activity) mContext, 
                            new BlockingOnUIRunnableListener() {
                            
                                @Override
                                public void onRunOnUIThread() {
                                    Toast.makeText(mContext, 
                                        "Fail to get account information. Please try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                        );
                        r.startOnUiAndWait();
                    } finally {
                        mProgressDialog.dismiss();
                    }
                }
            }
        });

        t.start();
        
    }

    @Override
    public void loadDirectoryFromCloud(DirectoryObject directory) {
        mRetrieveFilesAsync = new RetrieveFilesAsync((DropboxDirectory)directory);
        mRetrieveFilesAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void copyFiles(OnListViewProgressBarUpdateListener mAdapterListener) {
        CopyManager.getCopyManager().start(new DropboxCopyFilesAsync(mAdapterListener));
    }
    
    @Override
    public int deleteFile(FileObject file) {
        try {
            mApi.delete(file.getUId());
            mFileDAO.deleteFile(mAccount.getId(), file.getUId());
            
            return ErrorCode._NO_ERROR;
            
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            if (e.error == DropboxServerException._404_NOT_FOUND) {
                mFileDAO.deleteFile(mAccount.getId(), file.getUId());
            }
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Delete file " + file.getUId(), e);
            return ErrorCode._DROPBOX_EXCEPTION;
        }
    }

    @Override
    public int deleteEmptyDirectory(DirectoryObject directory) {
        try {
            mApi.delete(directory.getUId());
            mFileDAO.deleteFile(mAccount.getId(), directory.getUId());
            
            return ErrorCode._NO_ERROR;
            
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            if (e.error == DropboxServerException._404_NOT_FOUND) {
                mFileDAO.deleteFile(mAccount.getId(), directory.getUId());
            }
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Delete empty folder " + directory.getUId(), e);
            return ErrorCode._DROPBOX_EXCEPTION;
        }
    }

    @Override
    public int createEmptyFile(DirectoryObject parentDir, String name) {
        FileInputStream fis = null;
        
        try {             
            File emptyFile = File.createTempFile("tmp123", null, null);
            fis = new FileInputStream(emptyFile);
            Entry fileEntry = mApi.putFile(
                parentDir.getUId() + "/" + name, fis, 0, null, null);

            DropboxFile uploadedFile = new DropboxFile(mAccount.getId(), fileEntry);
            mFileDAO.insertOrUpdateFile(uploadedFile);
            parentDir.getChildren().add(uploadedFile);
            
            return ErrorCode._NO_ERROR;
            
        } catch (FileNotFoundException e) {
            Log.e(LOGCAT,"", e);
            return ErrorCode._FILE_NOT_FOUND;
            
        } catch (IOException e) {
            Log.e(LOGCAT,"", e);
            return ErrorCode._FILE_CAN_NOT_BE_CREATED;

        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Create empty file " + parentDir.getUId() + "/" + name, e);
            return ErrorCode._DROPBOX_EXCEPTION;
            
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                Log.e(LOGCAT,"", e);
            }
        }
    }

    @Override
    public int createEmptyFolder(DirectoryObject parentDir, String name) {
        try {
            Entry dirEntry = mApi.createFolder(parentDir.getUId() + "/" + name);
            DropboxDirectory newDir = new DropboxDirectory(mAccount.getId(), dirEntry);
            mFileDAO.insertOrUpdateFile(newDir);
            parentDir.getChildren().add(newDir);
            
            return ErrorCode._NO_ERROR;
            
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Create empty folder " + parentDir.getUId() + "/" + name, e);
            return ErrorCode._DROPBOX_EXCEPTION;
        }
    }

    @Override
    public int renameFile(FileObject file, String newName) {
        try {
            Entry fileEntry = mApi.move(file.getUId(), file.getParentUId() + "/" + newName);
            DropboxFile newFile = new DropboxFile(mAccount.getId(), fileEntry);
            mFileDAO.updateFile(newFile);
            
            return ErrorCode._NO_ERROR;
            
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Rename file " + file.getUId() + " to " + newName, e);
            return ErrorCode._DROPBOX_EXCEPTION;
        }
    }

    @Override
    public int renameDirectory(DirectoryObject directory, String newName) {
        try {
            Entry dirEntry = mApi.move(directory.getUId(), directory.getParentUId() + "/" + newName);
            DropboxDirectory newDir = new DropboxDirectory(mAccount.getId(), dirEntry);
            mFileDAO.updateFile(newDir);
            
            return ErrorCode._NO_ERROR;
            
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            if (e.error == DropboxServerException._404_NOT_FOUND) {
                mFileDAO.deleteFile(mAccount.getId(), directory.getUId());
            }
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Rename folder " + directory.getUId() + " to " + newName, e);
            return ErrorCode._DROPBOX_EXCEPTION;
        }
    }

    @Override
    public String shareLink(FileSystemObject f) {
        try {
            return mApi.share(f.getUId()).url;
            
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            if (e.error == DropboxServerException._404_NOT_FOUND) {
                mFileDAO.deleteFile(mAccount.getId(), f.getUId());
            }
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Fail to get link", e);
        }
        return null;
    }

    @Override
    public int downloadFileToCache(FileObject file, ProgressDialog dialog) {
        FileOutputStream fos = null; 
        DropboxInputStream dis = null;
        File dstFile = FileUtils.getCacheFile(mAccount.getId(), file);
        
        try {             
            fos = new FileOutputStream(dstFile);
            dis = mApi.getFileStream(file.getUId(), null);
            
            byte data[] = new byte[1024];
            
            long size = file.getSize();
            int len = 0;
            int c = 0; // increase counter/size
            final float fr = 0.01f; // increase (%) to the listview progress bar (1 file/directory)
            int segment = (int) (size * fr); // increase (bytes) of 1 file/directory
    
            while ((len = dis.read(data)) != -1) {
                fos.write(data, 0, len);
                c += len;
                if (c >= segment) {
                    // when onCancelled() is trigger, the dialog will be dimissed -> stop the download
                    if (!dialog.isShowing()) {
                        FileUtils.getCacheFile(mAccount.getId(), file).delete();
                        break;
                    }
                    dialog.incrementProgressBy(segment);
                    c -= segment;
                }
    
            }
            dialog.incrementProgressBy(c);
            
        } catch (FileNotFoundException e) {
            
        } catch (IOException e) {
    
        } catch (DropboxServerException e) {
            Log.e(LOGCAT,"", e);
            return getErrorCode(e);
            
        } catch (DropboxException e) {
            Log.e(LOGCAT, "Fail to download file " + file.getUId() + " to cache ", e);
            return ErrorCode._DROPBOX_EXCEPTION;
            
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (dis != null)
                    dis.close();
            } catch (IOException e) {
                Log.e(LOGCAT, "", e);
            }
        }
        
        return ErrorCode._NO_ERROR;
    }

    @Override
    public void cancelAllBackgroundTasks() {
        if (mRetrieveFilesAsync != null && !mRetrieveFilesAsync.isCancelled()) {
            mRetrieveFilesAsync.cancel(true);
        }
    }
    
    private int getErrorCode(DropboxServerException e) {
        int err = 0;
        switch (e.error) {
            case DropboxServerException._404_NOT_FOUND:
                err = ErrorCode._FILE_NOT_FOUND;
                break;
    
            case DropboxServerException._500_INTERNAL_SERVER_ERROR:
                err = ErrorCode._DROPBOX_EXCEPTION_INTERNAL_SERVER_ERR;
                break;
    
            case DropboxServerException._502_BAD_GATEWAY:
                err = ErrorCode._DROPBOX_EXCEPTION_BAD_GATEWAY;
                break;
    
            case DropboxServerException._507_INSUFFICIENT_STORAGE:
                err = ErrorCode._DROPBOX_EXCEPTION_INSUFFICIENT_STORAGE;
                break;
    
            case DropboxServerException._401_UNAUTHORIZED:
                err = ErrorCode._DROPBOX_EXCEPTION_UNAUTHORIZED;
                break;
    
            default:
                err = ErrorCode._DROPBOX_EXCEPTION;
                break;
        }
    
        return err;
    }

    private class RetrieveFilesAsync extends AsyncTask<Void, Void, Integer> {
        
        private DropboxDirectory mLoadDirectory;
        private DropboxDirectory mCurrentDirectory;
        
        
        public RetrieveFilesAsync(DropboxDirectory dir) {
            this.mCurrentDirectory = dir;
            this.mLoadDirectory = (DropboxDirectory)ObjectFactory.clone(dir);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int err = ErrorCode._NO_ERROR;
            
            try {
                Entry dirEntry = mApi.metadata(mLoadDirectory.getUId(), DROPBOX_FILE_LIMIT, 
                    (mLoadDirectory.getHash().equals("") ? null : mLoadDirectory.getHash()), true, null);
                
                DropboxDirectory temp = new DropboxDirectory(mAccount.getId(), dirEntry);
                
                if (!temp.isIdentical(mLoadDirectory)) {
                    mFileDAO.updateFile(temp);
                    mLoadDirectory.setAll(temp);
                }
                
                List<FileSystemObject> children = mFileDAO.getChildren(
                    mAccount.getId(), AccountType.DROPBOX, mLoadDirectory.getUId());
                
                List<String> childUIds = new ArrayList<String>();
                for (int i = 0; i < children.size(); i++) {
                    childUIds.add(children.get(i).getUId());
                }
                
                mLoadDirectory.getChildren().clear();
                for (Entry entry : dirEntry.contents) {
                    FileSystemObject child = null;
                    if (entry.isDir) {
                        child = new DropboxDirectory(mAccount.getId(), entry);
                    } else
                        child = new DropboxFile(mAccount.getId(), entry);

                    childUIds.remove(entry.path.toLowerCase());
                    
                    mLoadDirectory.getChildren().add(child);
                    mFileDAO.insertOrUpdateFile(child);
                }
                
                for (String childUId : childUIds) {
                    mFileDAO.deleteFile(mAccount.getId(), childUId);
                }
                
                if (mCurrentDirectory.equals(mLoadDirectory)) {
                    mCurrentDirectory.setAll(mLoadDirectory);
                    mHandler.sendEmptyMessage(TaskCode._REFRESH_VIEW);
                }
                
            } catch (DropboxServerException e) {
                if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                    Log.d(LOGCAT, mLoadDirectory + " is not modified");
                    
                } else {
                    if (e.error == DropboxServerException._404_NOT_FOUND) {
                        mFileDAO.deleteFile(mAccount.getId(), mLoadDirectory.getUId());
                    }
                    err = getErrorCode(e);
                }
                
            } catch (DropboxException e) {
                Log.e(LOGCAT, "Fail to retrieve file", e);
                err = ErrorCode._DROPBOX_EXCEPTION;
                
            } finally {
                //TODO: stop refresh only when all tasks finish
                mHandler.sendEmptyMessage(TaskCode._STOP_REFRESH);
            }
            
            return err;
        }
        
        @Override
        protected void onPostExecute(Integer err) {
            if (err != ErrorCode._NO_ERROR) {
                Toast.makeText(mContext, ErrorCode.getErrorMessage(err), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DropboxCopyFilesAsync extends CopyFilesAsync {

        private UploadRequest mUploadRequest;

        public DropboxCopyFilesAsync(OnListViewProgressBarUpdateListener mAdapterListener) {
            super((Activity)mContext, mAdapterListener, DropboxServiceManager.this.mHandler);
        }

        @Override
        protected Integer doInBackground(Void... unused) {
        
            if (!NetworkUtils.isConnected(mContext)) {
                return ErrorCode._NO_INTERNET_CONNECTION;
            }
            
            if (mCopyProcess.getCopyType() == CopyType.DROPBOX_COPY
                    || mCopyProcess.getCopyType() == CopyType.DROPBOX_DOWNLOAD) {
        
                for (FileSystemObject f : mSrcFiles) {
                    try {
                        if (f instanceof DropboxFile) {
                            Entry fileEntry = mApi.metadata(f.getUId(), DROPBOX_FILE_LIMIT, null, true, null);
        
                            DropboxFile newFile = new DropboxFile(mAccount.getId(), fileEntry);
                            mFileDAO.updateFile(newFile);
        
                            f.setSize(fileEntry.bytes);
                            mTotalSize += fileEntry.bytes;
        
                        } else if (f instanceof DropboxDirectory) {
                            long s = mTotalSize;
                            loadDirectoryRecursive(f.getUId());
                            f.setSize(mTotalSize - s);
                        }
                        
                        if (isCancelled())
                            return null;
                        
                    } catch (DropboxException e) {
                        // Only skip the file
                    }
                }
            }
        
            return super.doInBackground(unused);
        }

        @Override
        protected int copy(FileSystemObject f) {
            
            int err = ErrorCode._NO_ERROR;
            
            try {
                switch (mCopyProcess.getCopyType()) {
                    case DROPBOX_DOWNLOAD:
                        java.io.File dstDir = ((LocalDirectory) mDestination).getFile();
                        if (f instanceof DropboxDirectory) {
                            downloadDirectory((DropboxDirectory) f, dstDir);
                        } else {
                            downloadFile((DropboxFile) f, dstDir);
                        }
                        break;

                    case DROPBOX_UPLOAD:
                        if (f instanceof LocalFile) {
                            uploadFile(((LocalFile) f).getFile(), (DropboxDirectory) mDestination);
                        } else {
                             uploadDirectory(
                                 ((LocalDirectory) f).getFile(),(DropboxDirectory) mDestination);
                        }
                        break;

                    case DROPBOX_COPY:
                        if (f instanceof DropboxDirectory) {
                            copyDirectory((DropboxDirectory) f, (DropboxDirectory) mDestination);
                        } else {
                            copyFile((DropboxFile) f, (DropboxDirectory) mDestination);
                        }
                        break;

                    default:
                        break;
                }
                
            } catch (DropboxServerException e) {
                Log.e(LOGCAT,"", e);
                err = getErrorCode(e);
                
            } catch (DropboxException e) {
                Log.e(LOGCAT, "Copy '" + f.getName() + "' -> '" + mDestination.getName() + "'", e);
                err = ErrorCode._DROPBOX_EXCEPTION;
            }
            
            return err;
        }
        
        private void copyFile(DropboxFile srcFile, DropboxDirectory dstDir) throws DropboxException {
            Entry fileEntry = mApi.copy(srcFile.getUId(), dstDir.getUId() + "/" + srcFile.getName());
            DropboxFile copiedFile = new DropboxFile(mAccount.getId(), fileEntry);
            mFileDAO.insertOrUpdateFile(copiedFile);
            
            if (mCopyProcess.isMove()) { 
                deleteFileOrEmptyDirectory(srcFile.getUId());
            }
        }
        
        private void copyDirectory(DropboxDirectory srcDir, DropboxDirectory dstDir) throws DropboxException {
            
            if (srcDir.equals(mDestination)) {
                mHandler.sendEmptyMessage(ErrorCode._CAN_NOT_COPY_FOLDER_INTO_ITSELF);
                return;
            }
            
            createEmptyFolder(dstDir, srcDir.getName());
            
            loadDirectoryFromDatabase(srcDir);
            for (FileSystemObject child : srcDir.getChildren()) {
                if (child instanceof DropboxDirectory) {
                    copyDirectory((DropboxDirectory) child, dstDir);
                } else {
                    copyFile((DropboxFile) child, dstDir);
                }
                if (isCancelled())
                    return;
                
                if (mCopyProcess.isMove()) { 
                    deleteFileOrEmptyDirectory(srcDir.getUId());
                }
            }
        }
        
        private void uploadFile(File srcFile, DropboxDirectory dstDir) throws DropboxException {
            FileInputStream fis = null;
            final String dstFilePath = dstDir.getUId() + "/" + srcFile.getName();
            try {             
                fis = new FileInputStream(srcFile);
                mUploadRequest = mApi.putFileOverwriteRequest(
                    dstFilePath, fis, srcFile.length(), new ProgressListener() {
                    
                        long bytesUploaded;
                        
                        @Override
                        public void onProgress(long bytes, long total) {
                            mAdapterListener.onIncrementProgress(((float)(bytes - bytesUploaded))/mSize);
                            mDrawerListener.onIncrementProgress(((float)(bytes - bytesUploaded))/mTotalSize);
                            bytesUploaded = bytes;
                        }
                });
                
                //TODO: Thread inside AsyncTask ?
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!isCancelled()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                
                            }
                        }
                        mUploadRequest.abort();
                        try {
                            deleteFileOrEmptyDirectory(dstFilePath);
                        } catch (DropboxException e) {
                            Log.e(LOGCAT, "Abort uploading to dropbox: fail to delete " + dstFilePath, e);
                        }
                    }
                });
                t.start();
                
                Entry fileEntry = mUploadRequest.upload();
                DropboxFile uploadedFile = new DropboxFile(mAccount.getId(), fileEntry);
                mFileDAO.insertOrUpdateFile(uploadedFile);
                
            } catch (FileNotFoundException e) {
                
            } catch (DropboxPartialFileException e) {
                Log.i(LOGCAT, "Abort uploading to Dropbox");
                
            } catch (DropboxFileSizeException e) {
                Log.e(LOGCAT, "File size is too large: " + srcFile.length(), e);
                
            } finally {
                try {
                    if (fis != null)
                        fis.close();

                } catch (IOException e) {
                    Log.e(LOGCAT, "", e);
                }
            }
        }
        
        private void uploadDirectory(File srcDir, DropboxDirectory dstDir) throws DropboxException {
            DropboxDirectory newDir = createEmptyFolder(dstDir, srcDir.getName());

            java.io.File[] files = srcDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isFile()) {
                        uploadFile(f, newDir);
                    } else if (f.isDirectory()) {
                        uploadDirectory(f, newDir);
                    }
                    if (isCancelled())
                        return;
                }

            }
        }
        
        private void downloadFile(DropboxFile srcFile, File dstDir) throws DropboxException {
            FileOutputStream fos = null; 
            DropboxInputStream dis = null;
            File dstFile = new File(dstDir, srcFile.getName());
            
            try {             
                fos = new FileOutputStream(dstFile);
                dis = mApi.getFileStream(srcFile.getUId(), null);
                
                byte data[] = new byte[1024];
                
                int len = 0;
                int c = 0; // increase counter/size
                final float fr = 0.01f; // increase (%) to the listview progress bar (1 file/directory)
                int segment = (int) (mSize * fr); // increase (bytes) of 1 file/directory
                float r = ((float)mSize) * fr / mTotalSize; // increase (%) of the whole process

                while ((len = dis.read(data)) != -1) {
                    fos.write(data, 0, len);
                    c += len;
                    if (c >= segment) {
                        mAdapterListener.onIncrementProgress(fr);
                        mDrawerListener.onIncrementProgress(r);
                        c -= segment;
                    }
                    
                    if (isCancelled()) {
                        dstFile.delete();
                        break;
                    }
                }
                
                mAdapterListener.onIncrementProgress(mSize == 0 ? 1 : ((float)c)/mSize);
                mDrawerListener.onIncrementProgress(mTotalSize == 0 ? 1 : ((float)c)/mTotalSize);
                
                if (mCopyProcess.isMove()) {
                    deleteFile(srcFile);
                }
                
            } catch (FileNotFoundException e) {
                Log.e(LOGCAT, "", e);
                
            } catch (IOException e) {
                Log.e(LOGCAT, "", e);
                
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                    if (dis != null)
                        dis.close();
                } catch (IOException e) {

                }
            }
        }
        
        private void downloadDirectory(DropboxDirectory srcDir, java.io.File dstDir) throws DropboxException {
            
            java.io.File newDir = new java.io.File(dstDir, srcDir.getName());
            newDir.mkdir();
            
            loadDirectoryFromDatabase(srcDir); // get list of children from database
            for (FileSystemObject child : srcDir.getChildren()) {
                if (child instanceof DropboxDirectory) {
                    downloadDirectory((DropboxDirectory) child, newDir);
                } else {
                    downloadFile((DropboxFile) child, newDir);
                }
                if (isCancelled())
                    return;
            }
            
            if (mCopyProcess.isMove()) { 
                deleteEmptyDirectory(srcDir);
            }
        }

        private DropboxDirectory createEmptyFolder(DropboxDirectory parentDir, String name) throws DropboxException {
            Entry dirEntry = mApi.createFolder(parentDir.getUId() + "/" + name);
            DropboxDirectory newDir = new DropboxDirectory(mAccount.getId(), dirEntry);
            mFileDAO.insertOrUpdateFile(newDir);
            
            return newDir;
        }
        
        private void deleteFileOrEmptyDirectory(String path) throws DropboxException {
                mApi.delete(path);
                mFileDAO.deleteFile(mAccount.getId(), path);
        }


        private void loadDirectoryRecursive(String path) throws DropboxException {
            
            if (isCancelled())
                return;
            
            Entry dirEntry = mApi.metadata(path, DROPBOX_FILE_LIMIT, null, true, null);
            
            DropboxDirectory parent = new DropboxDirectory(mAccount.getId(), dirEntry);
            mFileDAO.insertOrUpdateFile(parent);
            
            List<FileSystemObject> children = mFileDAO.getChildren(
                mAccount.getId(), AccountType.DROPBOX, path);
            
            List<String> childUIds = new ArrayList<String>();
            for (int i = 0; i < children.size(); i++) {
                childUIds.add(children.get(i).getUId());
            }
            
            for (Entry entry : dirEntry.contents) {
                FileSystemObject child = null;
                if (entry.isDir) {
                    child = new DropboxDirectory(mAccount.getId(), entry);
                    loadDirectoryRecursive(entry.path);
                } else {
                    child = new DropboxFile(mAccount.getId(), entry);
                    mFileDAO.insertOrUpdateFile(child);
                    mTotalSize += entry.bytes;
                }

                childUIds.remove(entry.path.toLowerCase());
                
                
            }
            
            for (String childUId : childUIds) {
                mFileDAO.deleteFile(mAccount.getId(), childUId);
            }


        }
        
    }

}