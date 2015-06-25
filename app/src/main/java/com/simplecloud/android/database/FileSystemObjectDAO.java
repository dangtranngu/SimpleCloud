package com.simplecloud.android.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.simplecloud.android.common.utils.StringUtils;
import com.simplecloud.android.database.SimpleCloudProvider.File_Column;
import com.simplecloud.android.models.ObjectFactory;
import com.simplecloud.android.models.accounts.Account.AccountType;
import com.simplecloud.android.models.files.DirectoryObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.GoogleDriveDirectory;
import com.simplecloud.android.services.googledrive.GoogleDriveServiceManager;

public class FileSystemObjectDAO {
    
    public static final String LOGCAT = FileSystemObjectDAO.class.getSimpleName();
    
    private ContentResolver mContentResolver;
    
    public FileSystemObjectDAO(Context context) {
        mContentResolver = context.getContentResolver();
    }
    
    
    /**
     * Insert a FileSystemObject to database
     * @param file FileSystemObject to insert
     */
    public void insertOrUpdateFile(FileSystemObject file) {

        ContentValues v = new ContentValues();
        
        v.put(File_Column.NAME, file.getName());
        v.put(File_Column.UID, file.getUId());
        v.put(File_Column.PARENT_UID, file.getParentUId());
        v.put(File_Column.MIMETYPE, file.getMimeType());
        v.put(File_Column.SIZE, file.getSize());
        v.put(File_Column.ACC_ID, file.getAccId());
        v.put(File_Column.MODIFIED_DATE, file.getLastModifiedTime());
        v.put(File_Column.HASH, file.getHash());
        v.put(File_Column.IS_DIR, (file instanceof DirectoryObject) ? 1 : 0);

        mContentResolver.insert(File_Column.getContentUri(), v);
        Log.d(LOGCAT, "insertFile() - " + file.getName());
    }
    
    /**
     * Insert multiple FileSystemObject to database
     * @param object FileSystemObject to insert
     */
    public void insertFiles(List<FileSystemObject> files) {

        ContentValues[] values = new ContentValues[files.size()]; 
        
        for (int i = 0; i < values.length; i++) {
            FileSystemObject f = files.get(i);
            ContentValues v = new ContentValues();
            v.put(File_Column.NAME, f.getName());
            v.put(File_Column.UID, f.getUId());
            v.put(File_Column.PARENT_UID, f.getParentUId());
            v.put(File_Column.MIMETYPE, f.getMimeType());
            v.put(File_Column.SIZE, f.getSize());
            v.put(File_Column.ACC_ID, f.getAccId());
            v.put(File_Column.MODIFIED_DATE, f.getLastModifiedTime());
            v.put(File_Column.HASH, f.getHash());
            v.put(File_Column.IS_DIR, (f instanceof DirectoryObject) ? 1 : 0);
            
            values[i] = v;
        }
        Log.d(LOGCAT, "insertFiles() - " + files.size());
        mContentResolver.bulkInsert(File_Column.getContentUri(), values);
    }
    
    public void insertLocalBookmark(File file) {

        ContentValues v = new ContentValues();
        
        v.put(File_Column.UID, file.getAbsolutePath());
        v.put(File_Column.ACC_ID, 0);
        v.put(File_Column.IS_DIR, (file.isDirectory()) ? 1 : 0);
        v.put(File_Column.NAME, file.getName());
        v.put(File_Column.BOOKMARK, 1);

        mContentResolver.insert(File_Column.getContentUri(), v);
        Log.d(LOGCAT, "insertLocalBookmark() - " + file);
    }
    
    public boolean deleteLocalBookmark(String uid) {
        
        String selection = File_Column.ACC_ID + "='" + 0 + "'"
            + " AND " + File_Column.UID + "='" + uid + "'";
        
        int numOfRowDeleted = mContentResolver.delete(File_Column.getContentUri(), selection, null);
        Log.d(LOGCAT, "deleteLocalBookmark() - " + uid);
        return (numOfRowDeleted > 0);
    }
    
    /**
     * Update a FileSystemObject in database
     * @param file FileSystemObject to update
     * @param mContext
     */
    public int updateFile(FileSystemObject file) {
        
        ContentValues v = new ContentValues();
        
        v.put(File_Column.NAME, file.getName());
        v.put(File_Column.PARENT_UID, file.getParentUId());
        v.put(File_Column.MIMETYPE, file.getMimeType());
        v.put(File_Column.SIZE, file.getSize());
        v.put(File_Column.MODIFIED_DATE, file.getLastModifiedTime());
        v.put(File_Column.HASH, file.getHash());
        
        String selection = File_Column.ACC_ID + "='" + file.getAccId() + "'"
            + " AND " + File_Column.UID + "='" + file.getUId() + "'";
        
        Log.d(LOGCAT, "updateFile() - " + file.getName());
        return mContentResolver.update(
            File_Column.getContentUri(), v, selection, null);
    }
    
    public int setBookmark(int accId, String uid, boolean isBookmark) {
        
        ContentValues v = new ContentValues();

        v.put(File_Column.BOOKMARK, isBookmark ? 1 : 0);
        
        String selection = File_Column.ACC_ID + "='" + accId + "'"
            + " AND " + File_Column.UID + "='" + uid + "'";
        
        Log.d(LOGCAT, "setBookmark() - " + uid + " to " + isBookmark);
        return mContentResolver.update(
            File_Column.getContentUri(), v, selection, null);
    }

    /**
     * Delete a FileSystemObject in database
     * @param object FileSystemObject to delete
     * @param mContext
     */
    public boolean deleteFile(int accId, String uid) {
        
        String selection = File_Column.ACC_ID + "='" + accId + "'"
            + " AND " + File_Column.UID + "='" + uid + "'";
        
        int numOfRowDeleted = mContentResolver.delete(File_Column.getContentUri(), selection, null);
        Log.d(LOGCAT, "deleteFile() - " + uid);
        return (numOfRowDeleted > 0);
    }
    
    public FileSystemObject getFile(int accId, AccountType accType, String uid) {

        FileSystemObject f = null;

        Cursor c = null;
        try {
            String[] projection = new String[] {File_Column.NAME, 
                File_Column.IS_DIR, File_Column.PARENT_UID, 
                File_Column.HASH, File_Column.MODIFIED_DATE,
                File_Column.SIZE, File_Column.MIMETYPE};
            
            String selection = File_Column.ACC_ID + "='" + accId + "'"
                + " AND " + File_Column.UID + "='" + StringUtils.escapeSpecialChars(uid) + "'";
            
            c = mContentResolver.query(
                File_Column.getContentUri(), projection, selection, null, null);
            
            if (c != null && c.moveToFirst() && c.isLast()) {
                f = ObjectFactory.generateNewFileSystemObject(accType,
                    (c.getInt(c.getColumnIndex(File_Column.IS_DIR)) > 0) ? true : false);
                
                f.setName(c.getString(c.getColumnIndex(File_Column.NAME)));
                f.setParentUId(c.getString(c.getColumnIndex(File_Column.PARENT_UID)));
                f.setHash(c.getString(c.getColumnIndex(File_Column.HASH)));
                f.setSize(c.getLong(c.getColumnIndex(File_Column.SIZE)));
                f.setMimeType(c.getString(c.getColumnIndex(File_Column.MIMETYPE)));
                f.setLastModifiedTime(c.getLong(c.getColumnIndex(File_Column.MODIFIED_DATE)));
                f.setUId(uid);
                f.setAccId(accId);
            }
            
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        //Log.d(LOGCAT, "getFile() - " + ((f == null) ? "" : f.getName()));
        return f;
    }
    
    public List<FileSystemObject> getBookmarks(int accId) {
        List<FileSystemObject> files = new ArrayList<FileSystemObject>();
        
        Cursor c = null;
        try {
            String[] projection = new String[] {File_Column.NAME, 
                File_Column.UID, File_Column.IS_DIR, File_Column.MODIFIED_DATE};
            String selection = File_Column.ACC_ID + "='" + accId + "'"
                + " AND " + File_Column.BOOKMARK + "=1";
            
            c = mContentResolver.query(
                File_Column.getContentUri(), projection, selection, null, null);
            
            if (c != null && c.moveToFirst()) {
                do {
                    FileSystemObject f = ObjectFactory.generateNewFileSystemObject(
                        null, (c.getInt(c.getColumnIndex(File_Column.IS_DIR)) > 0) ? true : false);
                    
                    f.setName(c.getString(c.getColumnIndex(File_Column.NAME)));
                    f.setUId(c.getString(c.getColumnIndex(File_Column.UID)));
                    f.setLastModifiedTime(c.getLong(c.getColumnIndex(File_Column.MODIFIED_DATE)));
                    f.setAccId(accId);
                    files.add(f);
                    
                } while (c.moveToNext());
            }
            
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        return files;
    }
    
    public List<FileSystemObject> getChildren(int accId, AccountType accType, String parentUId) {
        List<FileSystemObject> objs = new ArrayList<FileSystemObject>();
        
        Cursor c = null;
        try {
            String[] projection = new String[] {File_Column.NAME, 
                File_Column.UID, File_Column.IS_DIR, 
                File_Column.SIZE, File_Column.HASH};
            String selection = File_Column.ACC_ID + "='" + accId + "'"
                + " AND " + File_Column.PARENT_UID + "='" + StringUtils.escapeSpecialChars(parentUId) + "'";
            
            c = mContentResolver.query(
                File_Column.getContentUri(), projection, selection, null, null);
            
            if (c != null && c.moveToFirst()) {
                do {
                    FileSystemObject obj = ObjectFactory.generateNewFileSystemObject(
                        accType, (c.getInt(c.getColumnIndex(File_Column.IS_DIR)) > 0) ? true : false);
                    
                    obj.setName(c.getString(c.getColumnIndex(File_Column.NAME)));
                    obj.setParentUId(parentUId);
                    obj.setUId(c.getString(c.getColumnIndex(File_Column.UID)));
                    obj.setSize(c.getLong(c.getColumnIndex(File_Column.SIZE)));
                    obj.setHash(c.getString(c.getColumnIndex(File_Column.HASH)));
                    obj.setAccId(accId);
                    objs.add(obj);
                    
                } while (c.moveToNext());
            }
            
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        return objs;
    }
    
    public GoogleDriveDirectory getGoogleDriveRootDirectory(int accId) {
        Cursor c = null;
        GoogleDriveDirectory directory = new GoogleDriveDirectory();
        
        try {
            String[] projection = new String[] {File_Column.UID};
            String selection = File_Column.ACC_ID + "='" + accId + "'"
                + " AND " + File_Column.NAME + "='" + GoogleDriveServiceManager.ROOT_FOLDER + "'"
                + " AND " + File_Column.PARENT_UID + "=''";

            c = mContentResolver.query(
                File_Column.getContentUri(), projection, selection, null, null);

            if (c != null && c.moveToFirst() && c.isLast()) {
                String uid = c.getString(c.getColumnIndex(File_Column.UID));
                directory = new GoogleDriveDirectory(accId, uid);
            }
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        return directory;
    }
}
