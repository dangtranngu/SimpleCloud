package com.simplecloud.android.models.files;

import com.simplecloud.android.services.googledrive.GoogleDriveServiceManager;


public class GoogleDriveDirectory extends DirectoryObject {

    public GoogleDriveDirectory(int accId, com.google.api.services.drive.model.File folder) {
        super(accId, folder.getId(), "", folder.getTitle());

        if (folder.getParents() != null && folder.getParents().size() > 0) {
            parentUId = folder.getParents().get(0).getId();
        }
        
        size = folder.getFileSize() == null ? 0L : folder.getFileSize();
        mimeType = folder.getMimeType();
        lastModifiedTime = folder.getModifiedDate().getValue();
    }

    public GoogleDriveDirectory() {
        super();
    }

    /**
     * Constructor for root folder only
     * @param accId
     * @param uid
     */
    public GoogleDriveDirectory(int accId, String uid) {
        super(accId, uid, "", "");
        name = GoogleDriveServiceManager.ROOT_FOLDER;
    }
    
    public GoogleDriveDirectory(GoogleDriveDirectory obj){
    	super(obj);
    }

}
