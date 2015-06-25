package com.simplecloud.android.models.files;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.RESTUtility;

public class DropboxFile extends FileObject {

    public DropboxFile(int accId, Entry file) {
        super(accId, file.path.toLowerCase(), file.parentPath().toLowerCase(), file.fileName());
        if (parentUId.length() > 1) {
            parentUId = parentUId.substring(0, parentUId.length() - 1);
        }
        size = file.bytes;
        mimeType = file.mimeType;
        hash = file.hash == null ? "" : file.hash;
        lastModifiedTime =  RESTUtility.parseDate(file.modified).getTime();
    }
    
    public DropboxFile() {
        
    }

    public DropboxFile(DropboxFile obj) {
        super(obj);
    }
}
