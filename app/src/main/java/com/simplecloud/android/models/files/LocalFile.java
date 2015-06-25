package com.simplecloud.android.models.files;

import java.io.File;

public class LocalFile extends FileObject {

    public LocalFile() {
        
    }
    
    public LocalFile(File file) {
        super(0, file.getAbsolutePath(), file.getParent(), file.getName());
        size = getFile().length();
    }
    
    public File getFile() {
        return new File(uid);
    }

}
