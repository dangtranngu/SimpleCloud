package com.simplecloud.android.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.simplecloud.android.common.ErrorCode;

/**
 * Known bug: only support ASCII
 * 
 * @author ngu
 * 
 */
public class ZipUtils {

    public static int zipFiles(List<File> files, String destZipFile) {
        
        int err = ErrorCode._NO_ERROR;
        ZipOutputStream zip = null;
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(destZipFile);
            zip = new ZipOutputStream(fos);

            for (File f : files) {
                if (f.isFile()) {
                    addFileToZip("", f, zip);

                } else if (f.isDirectory()) {
                    addFolderToZip("", f, zip);

                }
            }


        } catch (FileNotFoundException e) {
            err = ErrorCode._FILE_NOT_FOUND;

        } catch (IOException e) {
            err = ErrorCode._FAIL_TO_ZIP_FILE;

        } finally {
            try {
                zip.flush();
                zip.close();
            } catch (IOException e) {
                err = ErrorCode._FAIL_TO_ZIP_FILE;
            }

        }
        return err;

    }

    public static int unZipFiles(File zipFile, String dest) {

        int err = ErrorCode._NO_ERROR;
        byte[] buffer = new byte[1024];
        ZipInputStream zis = null;

        try {
            zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry = zis.getNextEntry();

            while (entry != null) {

                String fileName = entry.getName();
                File newFile = new File(dest + File.separator + fileName);

                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                entry = zis.getNextEntry();
            }

        } catch (IOException ex) {
            err = ErrorCode._FAIL_TO_UNZIP_FILE;
            
        } finally {
            try {
                zis.closeEntry();
                zis.close();
            } catch (IOException e) {
                err = ErrorCode._FAIL_TO_UNZIP_FILE;
            }

        }
        return err;
    }

    private static void addFileToZip(String zipPath, File srcFile, ZipOutputStream zip)
        throws IOException {

        FileInputStream in = null;
        ZipEntry entry = null;
        try {
            entry = new ZipEntry(zipPath + "/" + srcFile.getName());
            zip.putNextEntry(entry);

            in = new FileInputStream(srcFile);
            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }

        } finally {
            if (in != null)
                in.close();

        }
    }

    private static void addFolderToZip(String zipPath, File srcFolder, ZipOutputStream zip)
        throws IOException {

        for (File child : srcFolder.listFiles()) {
            if (child.isDirectory()) {
                addFolderToZip(zipPath + "/" + srcFolder.getName(), child, zip);
            } else {
                addFileToZip(zipPath + "/" + srcFolder.getName(), child, zip);
            }
        }


    }



}
