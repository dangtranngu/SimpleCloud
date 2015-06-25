package com.simplecloud.android.common;

import com.dropbox.client2.DropboxAPI;
import com.simplecloud.android.common.utils.FileUtils;

/**
 * Collections of all error codes. Must be different from task codes
 * @author ngu
 *
 */
public class ErrorCode {

    // General errors
    public static final int _NO_ERROR = 100;
    
    // Network related errors
    public static final int _NO_INTERNET_CONNECTION = 400;
    public static final int _DOWNLOAD_LIMIT_EXCEEDED = 401;
    public static final int _UPLOAD_LIMIT_EXCEEDED = 402;
    
    // File related errors
    public static final int _FILE_NOT_FOUND = 500;
    public static final int _FILE_CAN_NOT_BE_CREATED = 501;
    public static final int _DIRECTORY_ALREADY_EXIST = 502;
    public static final int _FILE_CAN_NOT_BE_DELETED = 503;
    public static final int _FILE_CAN_NOT_BE_RENAMED = 504;
    public static final int _FILE_ALREADY_EXIST = 505;
    public static final int _CAN_NOT_COPY_FOLDER_INTO_ITSELF = 506;
    public static final int _FILE_CAN_NOT_BE_READ = 507;
    public static final int _FAIL_TO_ZIP_FILE = 508;
    public static final int _FAIL_TO_UNZIP_FILE = 509;
    
    // Dropbox errors
    public static final int _DROPBOX_EXCEPTION = 200;
    public static final int _DROPBOX_EXCEPTION_MAX_UPLOAD_SIZE = 201;
    public static final int _DROPBOX_EXCEPTION_BAD_GATEWAY = 202;
    public static final int _DROPBOX_EXCEPTION_SERVICE_UNAVAILABLE = 203;
    public static final int _DROPBOX_EXCEPTION_INSUFFICIENT_STORAGE = 204;
    public static final int _DROPBOX_EXCEPTION_INTERNAL_SERVER_ERR = 205;
    public static final int _DROPBOX_EXCEPTION_UNAUTHORIZED = 206;
    
    // Google drive errors
    public static final int _GOOGLE_DRIVE_IO_EXCEPTION = 300;
    public static final int _GOOGLE_DRIVE_SECURITY_EXCEPTION = 301;
    

    public static String getErrorMessage(int errorCode) {
        String message = null;
        switch (errorCode) {
            
            case _NO_ERROR:
                break;

                
            // Network related errors
            case _NO_INTERNET_CONNECTION:
                message = "No internet connection";
                break;
                
            case _DOWNLOAD_LIMIT_EXCEEDED:
                message = "Large file/folder size is currently not supported " +
                        "(" + FileUtils.readableFileSize(FileUtils.MAX_DOWNLOAD_SIZE) + ")";
                break;
                
            case _UPLOAD_LIMIT_EXCEEDED:
                message = "Large file/folder size is currently not supported " +
                    "(" + FileUtils.readableFileSize(FileUtils.MAX_UPLOAD_SIZE) + ")";
                break;

                
            // File related errors
            case _DIRECTORY_ALREADY_EXIST:
                message = "Directory already exists";
                break;
 
            case _FILE_ALREADY_EXIST:
                message = "File already exists";
                break;
                
            case _FILE_CAN_NOT_BE_DELETED:
                message = "File can not be deleted";
                break;
 
            case _FILE_CAN_NOT_BE_RENAMED:
                message = "File can not be renamed";
                break;
                
            case _FILE_NOT_FOUND:
                message = "Item doesn't exist. Please refresh your data content";
                break;
 
            case _FAIL_TO_ZIP_FILE:
                message = "Error when compressing data";
                break;
    
            case _FAIL_TO_UNZIP_FILE:
                message = "Error when decompressing data";
                break;
                
            // Google drive errors
            case _GOOGLE_DRIVE_IO_EXCEPTION:
                message = "Fail to get data from Google";
                break;
 
            case _GOOGLE_DRIVE_SECURITY_EXCEPTION:
                message = "Can not authenticate with Google. If your password has been changed, please remove and add this account again";
                break;
                
                
            // Dropbox errors                
            case _DROPBOX_EXCEPTION:
                message = "Fail to get data from Dropbox";
                break;

            case _DROPBOX_EXCEPTION_MAX_UPLOAD_SIZE:
                message = "Dropbox does not support uploading large file " +
                    "(" + FileUtils.readableFileSize(DropboxAPI.MAX_UPLOAD_SIZE) + ")";
                break;


            default:
                message = "TODO: Not finished";
                break;
        }
        return message;
    }
}
