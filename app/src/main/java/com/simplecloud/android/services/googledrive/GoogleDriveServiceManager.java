package com.simplecloud.android.services.googledrive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.simplecloud.android.activities.SettingsActivity;
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
import com.simplecloud.android.common.utils.SharedPrefUtils;
import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.GoogleDriveAccount;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.GoogleDriveDirectory;
import com.simplecloud.android.models.files.GoogleDriveFile;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;
import com.simplecloud.android.services.AbstractServiceManager;


public class GoogleDriveServiceManager extends AbstractServiceManager {

    private static final String LOGCAT = GoogleDriveServiceManager.class.getSimpleName();

    public static final int REQUEST_ACCOUNT_PICKER = 1;
    public static final int REQUEST_AUTHORIZATION = 2;
    
    public static final String ROOT_FOLDER = "My Drive";
    
    private Drive mService;
    private GoogleAccountCredential mCredential;
    private RetrieveFilesAsync mRetrieveFilesAsync;
    

    public GoogleDriveServiceManager(Context context) {
        super(context);
        
        mCredential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE));
    }
    
    public GoogleDriveServiceManager(Context context, int accountId, Handler mHandler) {
        super(context, mHandler, accountId);
        
        mCredential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE));
        mCredential.setSelectedAccountName(mAccount.getUserId());
        
        mService = new Drive.Builder(
            AndroidHttp.newCompatibleTransport(), new GsonFactory(), mCredential)
            .setApplicationName("com.simplecloud.android").build();
    }
    
    @Override
    public void login() {
        ((Activity) mContext).getFragmentManager().findFragmentByTag(SettingsActivity.PREF_FRAGMENT_TAG)
                .startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    public void logout() {
        // No implementation
    }

    @Override
    public boolean isLogin() {
        return true;
    }
    
    public void addAccount(final String accountName) {
        
        final ProgressDialog mProgressDialog = ProgressDialog.show(
            mContext, "Please wait ...", "Retrieving " + accountName, true, true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int accId = 0;
                try {
                    mCredential.setSelectedAccountName(accountName);
                    mService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(), new GsonFactory(), mCredential)
                        .setApplicationName("com.simplecloud.android").build();
    
                    com.google.api.services.drive.model.About about =
                        mService.about().get().execute();

                    // Check if user already cancels the dialog
                    if (!mProgressDialog.isShowing()) 
                        return;

                    Account acc = new GoogleDriveAccount(about.getUser());
                    
                    accId = mAccountDAO.insertAccount(acc);
                    
                    if (accId == 0) 
                        return;
                    
                    // Save ROOT directory to database
                    DirectoryObject root = new GoogleDriveDirectory(accId, about.getRootFolderId());
                    root.setName(ROOT_FOLDER);
                    mFileDAO.insertOrUpdateFile(root);
                    
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
                    
                } catch (UserRecoverableAuthIOException e) {
                    ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    
                } catch (IOException e) {
                    Log.e(LOGCAT, "Add Account error", e);
                    
                } finally {
                    mProgressDialog.dismiss();
                }
            }
        });

        t.start();
    }

    @Override
    public void loadDirectoryFromCloud(DirectoryObject directory) {
        mRetrieveFilesAsync = new RetrieveFilesAsync((GoogleDriveDirectory)directory);
        mRetrieveFilesAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void copyFiles(OnListViewProgressBarUpdateListener mAdapterListener) {
        CopyManager.getCopyManager().start(new GoogleDriveCopyFilesAsync(mAdapterListener));
    }
    
    @Override
    public int renameFile(FileObject f, String newName) {
        return renameFileOrDirectory(f, newName);
    }

    @Override
    public int renameDirectory(DirectoryObject d, String newName) {
        return renameFileOrDirectory(d, newName);
    }
    
    private int renameFileOrDirectory(FileSystemObject f, String newName) {
        try {
            File file = new File();
            file.setTitle(newName);

            Files.Patch patchRequest = mService.files().patch(f.getUId(), file);
            patchRequest.setFields("title");

            patchRequest.execute();

            f.setName(newName);
            mFileDAO.updateFile(f);
            return ErrorCode._NO_ERROR;
            
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "renameFile[" + f.getName() + "]", e);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
        }
    }

    @Override
    public int deleteFile(FileObject f) {
        try {
            mService.files().delete(f.getUId()).execute();
            mFileDAO.deleteFile(mAccount.getId(), f.getUId());
            return ErrorCode._NO_ERROR;
            
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "Fail to delete file " + f.getName(), e);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
        }
    }
    
    @Override
    public int deleteEmptyDirectory(DirectoryObject f) {
        try {
            mService.files().delete(f.getUId()).execute();
            mFileDAO.deleteFile(mAccount.getId(), f.getUId());
            return ErrorCode._NO_ERROR;
            
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "Fail to delete empty directory " + f.getName(), e);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
            
        }
    }

    /**
     * @param parentDir create new folder in this directory
     * @param name name of the new folder
     * @return the error code
     */
    public int createEmptyFolder(DirectoryObject parentDir, String name) {
        try {
            File body = new File();
    
            String parentUId = parentDir.getUId();
            if (parentUId != null && parentUId.length() > 0) {
                body.setParents(Arrays.asList(new ParentReference().setId(parentUId)));
            }
    
            body.setTitle(name);
            body.setMimeType("application/vnd.google-apps.folder");
        
            File file = mService.files().insert(body).execute();

            GoogleDriveDirectory newDir = new GoogleDriveDirectory(mAccount.getId(), file);
            mFileDAO.insertOrUpdateFile(newDir);
            parentDir.getChildren().add(newDir);
            
            return ErrorCode._NO_ERROR;
            
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "Fail to create folder " + name, e);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
            
        }
    }
    
    /**
     * @param parentDir create new file in this directory
     * @param name name of the new file
     * @return error code
     */
    public int createEmptyFile(DirectoryObject parentDir, String name) {
        try {
            File body = new File();
            body.setTitle(name);
            body.setDescription("");
            body.setMimeType("text/plain");

            String parentUId = parentDir.getUId();
            if (parentUId != null && parentUId.length() > 0) {
                body.setParents(Arrays.asList(new ParentReference().setId(parentUId)));
            }

            File file = mService.files().insert(body).execute();
            
            GoogleDriveFile newFile = new GoogleDriveFile(mAccount.getId(), file);
            mFileDAO.insertOrUpdateFile(newFile);
            parentDir.getChildren().add(newFile);
            
            return ErrorCode._NO_ERROR;
            
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "Fail to create file " + name, e);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
        }
    }
    
    @Override
    public String shareLink(FileSystemObject file) {
        try {
            File f = mService.files().get(file.getUId()).execute();
            return f.getWebContentLink();
            
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "Fail to share file " + file.getName(), e);
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
        }
        //TODO: should return error code
        return null;
    }

    @Override
    public int downloadFileToCache(FileObject file, ProgressDialog dialog) {
        InputStream is = null;
        OutputStream os = null;
        try {
            dialog.setIndeterminate(true);
            File f = mService.files().get(file.getUId()).execute();
            
            String downloadUrl = f.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.length() == 0) {
                return 1;
            }    
            HttpResponse resp = mService.getRequestFactory()
                .buildGetRequest(new GenericUrl(downloadUrl)).execute();
            
            is = resp.getContent();
            if (is == null) {
                return 2;
            }
            
            dialog.setIndeterminate(false);
            os = new FileOutputStream(FileUtils.getCacheFile(mAccount.getId(), file));
            
            byte data[] = new byte[1024];
            
            long size = file.getSize();
            int len = 0;
            int c = 0; // increase counter/size
            final float fr = 0.01f; // increase (%) to the listview progress bar
            int segment = (int) (size * fr); // increase (bytes) of the file
    
            while ((len = is.read(data)) != -1) {
                os.write(data, 0, len);
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
            
            return ErrorCode._NO_ERROR;
    
        } catch (UserRecoverableAuthIOException e) {
            Log.e(LOGCAT, "", e);
            ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        }  catch (IOException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
            
        } catch (SecurityException e) {
            Log.e(LOGCAT, "", e);
            return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
            
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                Log.e(LOGCAT, "", e);
            }
        }
    }

    @Override
    public void cancelAllBackgroundTasks() {
        if (mRetrieveFilesAsync != null && !mRetrieveFilesAsync.isCancelled()) {
            mRetrieveFilesAsync.cancel(true);
        }
    }

    /** Retrieve all files from google drive
     * @author ngu
     *
     */
    public class RetrieveFilesAsync extends AsyncTask<Void, Void, Integer> {
    
        private GoogleDriveDirectory mDirectory;

        public RetrieveFilesAsync(GoogleDriveDirectory mDirectory) {
            this.mDirectory = mDirectory;
        }


        @Override
        protected Integer doInBackground(Void... unused) {
            int err = ErrorCode._NO_ERROR;
            try {
                if (SharedPrefUtils.getLatestGoogleDriveChangeId(mContext, mAccount.getId()) == 0) {
                    retrieveAllFiles(); // first complete update
                } else {
                    retrieveChanges();
                }
                
                Time t = new Time();
                t.setToNow();
                SharedPrefUtils.setLastUpdate(mContext, mAccount.getId(), t.toMillis(true));
                
            } catch (UserRecoverableAuthIOException e) {
                Log.e(LOGCAT, "", e);
                ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                
            } catch (IOException e) {
                Log.e(LOGCAT, "", e);
                err = ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
                
            } catch (SecurityException e) {
                Log.e(LOGCAT, "", e);
                err = ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
                
            } finally {
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
        
        private void retrieveAllFiles() throws IOException {
            Files.List request = mService.files().list();
            request.setMaxResults(1000);

            do {
                List<FileSystemObject> newFiles = new ArrayList<FileSystemObject>();
                boolean isAdded = false;
                FileList files = request.execute();

                for (File file : files.getItems()) {
                    if (file.getExplicitlyTrashed() == null) {
                        FileSystemObject newFile = null;
                        if (isDirectory(file)) {
                            newFile = new GoogleDriveDirectory(mAccount.getId(), file);
                        } else {
                            newFile = new GoogleDriveFile(mAccount.getId(), file);
                        }
                        
                        newFiles.add(newFile);
                        
                        if (mDirectory.getUId().equals(newFile.getParentUId())) {
                            if (mDirectory.getChildren().contains(newFile)) {
                                mDirectory.getChildren().remove(newFile);
                            }
                            mDirectory.getChildren().add(newFile);
                            isAdded = true;
                        }
                    }
                }
                mFileDAO.insertFiles(newFiles);
                
                if (isAdded) {
                    mHandler.sendEmptyMessage(TaskCode._REFRESH_VIEW);
                }
                
                if (isCancelled())
                    return;
                
                request.setPageToken(files.getNextPageToken());
                
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

            ChangeList changes = mService.changes().list().execute();
            SharedPrefUtils.setLatestGoogleDriveChangeId(
                mContext, mAccount.getId(), changes.getLargestChangeId());
        }
        
        private void retrieveChanges() throws IOException {
            Changes.List request = mService.changes().list();
            long startChangeId = SharedPrefUtils.getLatestGoogleDriveChangeId(mContext, mAccount.getId());
            request.setStartChangeId(startChangeId);
            
            do {
                boolean isChanged = false;
                ChangeList changes = request.execute();
                
                // Workaround: Google Drive's bug: always have at least 1 change
                if (changes.getItems().size() <= 1) 
                    return;

                for (Change c : changes.getItems()) {

                    if (c.getDeleted() && c.getFileId() != null) {
                        mFileDAO.deleteFile(mAccount.getId(), c.getFileId());
                        for (FileSystemObject f : mDirectory.getChildren()) {
                            if (f.getUId().equals(c.getFileId())) {
                                mDirectory.getChildren().remove(f);
                                isChanged = true;
                                break;
                            }
                        }
                    }

                    if (c.getFile() != null && c.getFile().getExplicitlyTrashed() == null) {
                        FileSystemObject newFile = null;
                        if (isDirectory(c.getFile())) {
                            newFile = new GoogleDriveDirectory(mAccount.getId(), c.getFile());
                        } else {
                            newFile = new GoogleDriveFile(mAccount.getId(), c.getFile());
                        }

                        mFileDAO.insertOrUpdateFile(newFile);
                        
                        if (mDirectory.getUId().equals(newFile.getParentUId())) {
                            if (mDirectory.getChildren().contains(newFile)) {
                                mDirectory.getChildren().remove(newFile);
                            }
                            mDirectory.getChildren().add(newFile);
                            isChanged = true;
                        }
                    }
                }

                if (isChanged) {
                    mHandler.sendEmptyMessage(TaskCode._REFRESH_VIEW);
                }
                
                if (isCancelled())
                    return;

                request.setPageToken(changes.getNextPageToken());

                SharedPrefUtils.setLatestGoogleDriveChangeId(
                    mContext, mAccount.getId(), changes.getLargestChangeId());

            } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        }
        
        private boolean isDirectory(File f) {
            return f.getMimeType().contains("apps.folder");
        }
    }


    private class GoogleDriveCopyFilesAsync extends CopyFilesAsync {


        public GoogleDriveCopyFilesAsync(OnListViewProgressBarUpdateListener mAdapterListener) {
            super((Activity)mContext, mAdapterListener, GoogleDriveServiceManager.this.mHandler);
        }
        
        @Override
        protected Integer doInBackground(Void... unused) {
            
            if (!NetworkUtils.isConnected(mContext)) {
                return ErrorCode._NO_INTERNET_CONNECTION;
            }
            
            if (mCopyProcess.getCopyType() == CopyType.GOOGLE_DRIVE_COPY
                    || mCopyProcess.getCopyType() == CopyType.GOOGLE_DRIVE_DOWNLOAD) {
                
                try {
                    loadChangesFromCloud();
                    
                } catch (UserRecoverableAuthIOException e) {
                    Log.e(LOGCAT, "", e);
                    ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    
                } catch (IOException e) {
                    Log.e(LOGCAT, "", e);
                    return ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
                    
                } catch (SecurityException e) {
                    Log.e(LOGCAT, "", e);
                    return ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
                }
        
                for (FileSystemObject f : mSrcFiles) {
        
                    if (f instanceof GoogleDriveDirectory) {
                        f.setSize(getDirectorySize((GoogleDriveDirectory) f));
                    }
                    mTotalSize += f.getSize();
                    
                    if (isCancelled())
                        return null;
                }
            }
        
            return super.doInBackground(unused);
        }

        @Override
        protected int copy(FileSystemObject f) {
            
            int err = ErrorCode._NO_ERROR;
            
            try {
                switch (mCopyProcess.getCopyType()) {
                    
                    case GOOGLE_DRIVE_DOWNLOAD:
                        java.io.File dstDir = ((LocalDirectory) mDestination).getFile();
                        if (f instanceof GoogleDriveDirectory) {
                            downloadDirectory((GoogleDriveDirectory) f, dstDir);
                        } else {
                            downloadFile((GoogleDriveFile) f, dstDir);
                        }
                        break;
            
                    case GOOGLE_DRIVE_UPLOAD:
                        if (f instanceof LocalFile) {
                            uploadFile(((LocalFile) f).getFile(), (GoogleDriveDirectory) mDestination);
                        } else {
                            uploadDirectory(((LocalDirectory) f).getFile(), (GoogleDriveDirectory) mDestination);
                        }
                        break;
            
                    case GOOGLE_DRIVE_COPY:
                        if (!mCopyProcess.isMove()) {
                            if (f instanceof GoogleDriveDirectory) {
                                copyDirectory((GoogleDriveDirectory) f, (GoogleDriveDirectory) mDestination);
                            } else {
                                copyFile((GoogleDriveFile) f, (GoogleDriveDirectory) mDestination);
                            }
                        } else {
                            moveFileOrDirectory(f, (GoogleDriveDirectory) mDestination);
                        }
                        break;
            
                    default:
                        break;
                }
            } catch (UserRecoverableAuthIOException e) {
                ((Activity) mContext).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                
            } catch (IOException e) {
                Log.e(LOGCAT, "Copy '" + f.getName() + "' -> '" + mDestination.getName() + "'", e);
                err = ErrorCode._GOOGLE_DRIVE_IO_EXCEPTION;
                
            } catch (SecurityException e) {
                Log.e(LOGCAT, "", e);
                err = ErrorCode._GOOGLE_DRIVE_SECURITY_EXCEPTION;
            }
            
            return err;
        }


        /**
         * Download a file from google drive to local directory
         * @param srcFile  google drive file
         * @param dstDir local destination
         * @throws IOException
         */
        private void downloadFile(GoogleDriveFile srcFile, java.io.File dstDir) throws IOException {
            InputStream is = null;
            OutputStream os = null;
            java.io.File dstFile = new java.io.File(dstDir, srcFile.getName());
            
            try {
                File f = mService.files().get(srcFile.getUId()).execute();
                
                String downloadUrl = f.getDownloadUrl();
                if (downloadUrl == null || downloadUrl.length() == 0) {
                    return;
                }    
                
                HttpResponse resp = mService.getRequestFactory()
                    .buildGetRequest(new GenericUrl(downloadUrl)).execute();
                
                is = resp.getContent();
                if (is == null) {
                    return;
                }
                
                os = new FileOutputStream(dstFile);
                
                byte data[] = new byte[1024];
                
                int len = 0;
                int c = 0; // increase counter/size
                final float fr = 0.01f; // increase (%) to the listview progress bar (1 file/directory)
                int segment = (int) (mSize * fr); // increase (bytes) of 1 file/directory
                float r = ((float)mSize) * fr / mTotalSize; // increase (%) of the whole process

                while ((len = is.read(data)) != -1) {
                    os.write(data, 0, len);
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
                
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (os != null) {
                        os.flush();
                        os.close();
                    }
                } catch (IOException e) {
                    Log.e(LOGCAT, "", e);
                }
            }
        }

        private void downloadDirectory(GoogleDriveDirectory srcDir, java.io.File dstDir) throws IOException {
            
            java.io.File newDir = new java.io.File(dstDir, srcDir.getName());
            newDir.mkdir();
            
            loadDirectoryFromDatabase(srcDir); // get list of children from database
            for (FileSystemObject child : srcDir.getChildren()) {
                if (child instanceof GoogleDriveDirectory) {
                    downloadDirectory((GoogleDriveDirectory) child, newDir);
                } else {
                    downloadFile((GoogleDriveFile) child, newDir);
                }
                if (isCancelled())
                    return;
            }
            
            if (mCopyProcess.isMove()) { 
                deleteEmptyDirectory(srcDir);
            }
        }

        private void copyFile(GoogleDriveFile srcFile, GoogleDriveDirectory dstDir) throws IOException {
            File content = new File();
            content.setTitle(srcFile.getName());

            String parentUId = dstDir.getUId();
            if (parentUId != null && parentUId.length() > 0) {
                content.setParents(Arrays.asList(new ParentReference().setId(parentUId)));
            }

            File f = mService.files().copy(srcFile.getUId(), content).execute();

            GoogleDriveFile copiedFile = new GoogleDriveFile(mAccount.getId(), f);
            mFileDAO.insertOrUpdateFile(copiedFile);
        }
        
        private void copyDirectory(GoogleDriveDirectory srcDir, GoogleDriveDirectory dstDir) throws IOException {
            
            if (srcDir.equals(mDestination)) {
                //TODO: increment drawer progress bar
                mHandler.sendEmptyMessage(ErrorCode._CAN_NOT_COPY_FOLDER_INTO_ITSELF);
                return;
            }
            
            createEmptyFolder(dstDir, srcDir.getName());
            
            loadDirectoryFromDatabase(srcDir);
            for (FileSystemObject child : srcDir.getChildren()) {
                if (child instanceof GoogleDriveDirectory) {
                    copyDirectory((GoogleDriveDirectory) child, dstDir);
                } else {
                    copyFile((GoogleDriveFile) child, dstDir);
                }
                if (isCancelled())
                    return;
            }
        }

        private void moveFileOrDirectory(FileSystemObject srcFile, GoogleDriveDirectory dstDir) throws IOException {
            File movedFile = new File();
            String parentUId = dstDir.getUId();
            if (parentUId != null && parentUId.length() > 0) {
                movedFile.setParents(Arrays.asList(new ParentReference().setId(parentUId)));
            }

            Files.Patch patchRequest = mService.files().patch(srcFile.getUId(), movedFile);
            patchRequest.setFields("parents");
            patchRequest.execute();

            srcFile.setParentUId(parentUId);
            mFileDAO.updateFile(srcFile);
        }
        
        private void uploadFile(java.io.File srcFile, GoogleDriveDirectory dstDir) throws IOException {
            
            String mimeType = FileUtils.getMimeType(srcFile.getAbsolutePath());
            File body = new File();
            body.setTitle(srcFile.getName());
            body.setDescription("");
            body.setMimeType(mimeType);

            String parentUId = dstDir.getUId();
            if (parentUId != null && parentUId.length() > 0) {
                body.setParents(Arrays.asList(new ParentReference().setId(parentUId)));
            }

            FileContent mediaContent = new FileContent(mimeType, srcFile);

            File file = mService.files().insert(body, mediaContent).execute();

            GoogleDriveFile newFile = new GoogleDriveFile(mAccount.getId(), file);
            mFileDAO.insertOrUpdateFile(newFile);
        }
        
        private void uploadDirectory(java.io.File srcDir, GoogleDriveDirectory targetDir) throws IOException {
            
            GoogleDriveDirectory newDir = createEmptyFolder(targetDir, srcDir.getName());

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
        
        private GoogleDriveDirectory createEmptyFolder(GoogleDriveDirectory parentDir, String name) 
                throws IOException {

            File body = new File();

            String parentUId = parentDir.getUId();
            if (parentUId != null && parentUId.length() > 0) {
                body.setParents(Arrays.asList(new ParentReference().setId(parentUId)));
            }

            body.setTitle(name);
            body.setMimeType("application/vnd.google-apps.folder");

            File file = mService.files().insert(body).execute();

            GoogleDriveDirectory newDir = new GoogleDriveDirectory(mAccount.getId(), file);
            mFileDAO.insertOrUpdateFile(newDir);

            return newDir;

        }

        private long getDirectorySize(GoogleDriveDirectory directory) {
            
            if (isCancelled())
                return 0;
            
            long length = 0;
            loadDirectoryFromDatabase(directory);
            for (FileSystemObject f : directory.getChildren()) {
                if (f instanceof FileObject)
                    length += f.getSize();
                else
                    length += getDirectorySize((GoogleDriveDirectory) f);
            }
            return length;
        }
        
        
        private void loadChangesFromCloud() throws IOException {
            Changes.List request = mService.changes().list();
            long startChangeId = SharedPrefUtils.getLatestGoogleDriveChangeId(mContext, mAccount.getId());
            request.setStartChangeId(startChangeId);
            //TODO: make sure startChangeId > 0
            
            do {
                ChangeList changes = request.execute();
                
                // Workaround: Google Drive's bug: always have at least 1 change
                if (changes.getItems().size() <= 1) 
                    return;

                for (Change c : changes.getItems()) {

                    if (c.getDeleted() && c.getFileId() != null) {
                        mFileDAO.deleteFile(mAccount.getId(), c.getFileId());
                    }

                    if (c.getFile() != null && c.getFile().getExplicitlyTrashed() == null) {
                        FileSystemObject newFile = null;
                        if (c.getFile().getMimeType().contains("apps.folder")) {
                            newFile = new GoogleDriveDirectory(mAccount.getId(), c.getFile());
                        } else {
                            newFile = new GoogleDriveFile(mAccount.getId(), c.getFile());
                        }

                        mFileDAO.insertOrUpdateFile(newFile);
                    }
                }
                
                if (isCancelled())
                    return;

                request.setPageToken(changes.getNextPageToken());

                SharedPrefUtils.setLatestGoogleDriveChangeId(
                    mContext, mAccount.getId(), changes.getLargestChangeId());

            } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        }
    }


}
