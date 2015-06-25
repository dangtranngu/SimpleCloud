package com.simplecloud.android.models.files;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.RESTUtility;

public class DropboxDirectory extends DirectoryObject {

    public DropboxDirectory(int accId, Entry file) {
        super(accId, file.path.toLowerCase(), file.parentPath().toLowerCase(), file.fileName());
        
        if (parentUId.length() > 1) {
            parentUId = parentUId.substring(0, parentUId.length() - 1);
        }

        size = file.bytes;
        mimeType = file.mimeType == null ? "" : file.mimeType;
        lastModifiedTime = file.modified == null ? 0 :  RESTUtility.parseDate(file.modified).getTime();
        hash = file.hash == null ? "" : file.hash;
    }
    
    public DropboxDirectory(DropboxDirectory obj) {
        super(obj);
    }
    
    public DropboxDirectory() {
        
    }
}
