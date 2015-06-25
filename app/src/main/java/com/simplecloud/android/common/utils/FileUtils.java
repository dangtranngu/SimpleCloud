package com.simplecloud.android.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.simplecloud.android.background.CopyFilesAsync;
import com.simplecloud.android.common.ErrorCode;
import com.simplecloud.android.models.files.FileObject;
import com.simplecloud.android.models.files.FileSystemObject;
import com.simplecloud.android.models.files.LocalDirectory;
import com.simplecloud.android.models.files.LocalFile;

public class FileUtils {

    public static final String LOGCAT = FileUtils.class.getSimpleName();
    public static final String DEFAULT_APP_DIRECTORY = Environment
        .getExternalStorageDirectory().getAbsolutePath() + "/.SimpleCloud/";
    public static File cacheFile = new File(DEFAULT_APP_DIRECTORY);
    
    public static final long MAX_UPLOAD_SIZE = 200*1024*1024;
    public static final long MAX_DOWNLOAD_SIZE = 200*1024*1024;
    
    public static final String ROOT_DIRECTORY = "/";
    public static final String PARENT_DIRECTORY = "..";
    public static final String CURRENT_DIRECTORY = ".";
    public static final String USER_ROOT = "root";

    public static File getSDCardDirectory() {
        return Environment.getExternalStorageDirectory();
    }
    
    public static File getSDCardDownloadDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static void setDefaultApplicationDirectory() {
        File d = new File(DEFAULT_APP_DIRECTORY);
        if (!d.exists()) {
            d.mkdirs();
        }
    }
    
    public static void setCacheDirectory(File file) {
        if (file != null)
            cacheFile = file;
    }

    public static File getCacheFile(int accId, FileObject f) {
        File d = new File(cacheFile, Integer.toString(accId));
        if (!d.exists()) {
            d.mkdirs();
        }
        
        return new File(d, Integer.toString(f.getUId().hashCode()) + "_" + f.getName());
    }
    
    public static void deleteCacheDirByAccount(int accId) {
        File d = new File(cacheFile, Integer.toString(accId));
        deleteDirectory(d);
    }
    
    public static boolean isProtected(File path) {
        return (!path.canRead() && !path.canWrite());
    }

    public static boolean isDirectoryAccessible(String directory) {
        File d = new File(directory);
        return d.isDirectory() && !isProtected(d);
    }

    public static  void copyFile(File source, File destination) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(new File(destination, source.getName()));
        } catch (FileNotFoundException e) {

        }

        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Log.e(LOGCAT, "Fail to copy file: " + source + " => " + destination, e);
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            } catch (IOException e) {
                Log.e(LOGCAT, "", e);
            }

        }
    }

    public static void copyDirectory(File source, File destination) {
        File targetDir = new File(destination, source.getName());
        if (!targetDir.exists())
            targetDir.mkdir();

        File[] files = source.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    copyFile(f, targetDir);
                } else if (f.isDirectory()) {
                    copyDirectory(f, targetDir);
                }
            }
        }
    }

    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    f.delete();
                } else if (f.isDirectory()) {
                    deleteDirectory(f);
                }
            }
            directory.delete();
        }
    }
    
    public static void createDirectory(String path) {
        File d = new File(path);
        if (!d.exists()) {
            d.mkdir();
        }
    }

    public static List<FileSystemObject> getChildren(File parentDir) {

        if (!parentDir.exists())
            return null;
        
        File[] dirs = parentDir.listFiles();
        List<FileSystemObject> files = new ArrayList<FileSystemObject>();
        
        if (dirs != null) {
            for (File item : dirs) {
                if (item.isDirectory()) {
                    files.add(new LocalDirectory(item));
                } else {
                    files.add(new LocalFile(item));
                }
            }
        }
        
        return files;
    }

    public static File writeLog() {

        FileOutputStream fos = null;
        OutputStreamWriter osw = null;

        try {
            Process process = Runtime.getRuntime().exec("logcat -d *:E");
            BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }

            File file = new File(DEFAULT_APP_DIRECTORY, "log.txt");
            if (file.exists()) {
                file.delete();
            }

            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos);
            osw.write(sb.toString());
            
            return file;
            
        } catch (IOException e) {

        } finally {
            if (osw != null) {
                try {
                    osw.flush();
                    osw.close();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }

    public static String readableFileSize(long size) {
        if (size < 0) return null;
        
        if (size == 0) return "0 B";
        
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#")
                .format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static long getDirectorySize(File directory) {
        long length = 0;
        File[] children = directory.listFiles();
        if (children != null) {
            for (File file : children) {
                if (file.isFile())
                    length += file.length();
                else
                    length += getDirectorySize(file);
            }
        }
        return length;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }
    
    public static class LocalCopyFilesAsync extends CopyFilesAsync {

        private File targetDir;
        
        public LocalCopyFilesAsync(Activity mActivity, 
                OnListViewProgressBarUpdateListener mAdapterListener, Handler mHandler) {
            super(mActivity, mAdapterListener, mHandler);
            targetDir = ((LocalDirectory)mDestination).getFile();
        }

        @Override
        protected int copy(FileSystemObject f) {
            int err = ErrorCode._NO_ERROR;
            try {
                if (f instanceof FileObject) {
                    copyFile(((LocalFile) f).getFile(), targetDir);
                } else {
                    copyDirectory(((LocalDirectory) f).getFile(), targetDir);
                }
            } catch (IOException e) {
                Log.e(LOGCAT, "Fail to copy " + f.getName(), e);
                err = ErrorCode._FILE_CAN_NOT_BE_READ;
            }
            
            return err;
        }

        private void copyFile(File srcFile, File dstDir) throws IOException {
            InputStream is = null;
            OutputStream os = null;
            File dstFile = new File(dstDir, srcFile.getName());
            try {
                is = new FileInputStream(srcFile);
                os = new FileOutputStream(dstFile);
            } catch (FileNotFoundException e) {
                Log.e(LOGCAT, "", e);
            }
            
            try {
                
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
                
                // Move item: delete after copied successfully
                if (mCopyProcess.isMove()) { 
                    srcFile.delete();
                }
                
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.flush();
                    os.close();
                }
            }
        }
        
        private void copyDirectory(File srcDir, File dstDir) throws IOException {
            
            if (srcDir.getAbsolutePath().equals(mDestination.getUId())) {
                mDrawerListener.onIncrementProgress(mTotalSize == 0 ? 1 : ((float)srcDir.length())/mTotalSize);
                mHandler.sendEmptyMessage(ErrorCode._CAN_NOT_COPY_FOLDER_INTO_ITSELF);
                return;
            }
            
            File newDir = new File(dstDir, srcDir.getName());
            newDir.mkdir();

            File[] files = srcDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        copyFile(f, newDir);
                    } else if (f.isDirectory()) {
                        copyDirectory(f, newDir);
                    }
                    if (isCancelled())
                        return;
                }
                
                if (mCopyProcess.isMove()) { 
                    srcDir.delete();
                }
            }
        }
        
    }
}
