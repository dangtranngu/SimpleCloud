package com.simplecloud.android.services;

import android.app.ProgressDialog;

import com.simplecloud.android.background.CopyFilesAsync.OnListViewProgressBarUpdateListener;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;

/** All files related actions
 * @author ngu
 *
 */
public interface IFileManager {

    /** Update the directory and its children from database. Requires running on background
     * @param directory
     */
    void loadDirectoryFromDatabase(DirectoryObject directory);
    
    /**
     * Update the directory and its children from the cloud service
     * @param directory
     */
    void loadDirectoryFromCloud(DirectoryObject directory);
    
    void copyFiles(OnListViewProgressBarUpdateListener mAdapterListener);
    
    int deleteFile(FileObject file);

    int deleteEmptyDirectory(DirectoryObject directory);

    int createEmptyFile(DirectoryObject parentDir, String name);

    int createEmptyFolder(DirectoryObject parentDir, String name);

    int renameFile(FileObject f, String newName);

    int renameDirectory(DirectoryObject d, String newName);
    
    String shareLink(FileSystemObject f);
    
    /**
     * Download the file into a preconfigured local directory. Requires running on background
     * @param f  the file to be downloaded
     * @param dialog  progress dialog of the process
     * @return  the error code 
     */
    int downloadFileToCache(FileObject f, ProgressDialog dialog);
    
}
