package com.simplecloud.android.models.files;

import java.io.File;
import java.util.List;

import com.simplecloud.android.common.utils.FileUtils;


public class LocalDirectory extends DirectoryObject {

    public LocalDirectory() {
        
    }
    
    public LocalDirectory(File file) {
        super(0, file.getAbsolutePath(), file.getParent(), 
            file.getName().equals("") ? "/" : file.getName());
    }
    
    public LocalDirectory(File file, int iconId) {
        this(file);
        setIconId(iconId);
    }

    @Override
    public List<FileSystemObject> getChildren() {
        return FileUtils.getChildren(new File(uid));
    }

    @Override
    public boolean isReadable() {
        return !FileUtils.isProtected(new File(uid));
    }

    public File getFile() {
        return new File(uid);
    }

}
