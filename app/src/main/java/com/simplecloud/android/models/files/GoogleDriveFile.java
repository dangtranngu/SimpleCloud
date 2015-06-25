package com.simplecloud.android.models.files;


/**
 * By default, Google Drive file names can be the same in the same directory 
 * @author ngu
 *
 */
public class GoogleDriveFile extends FileObject {

    public GoogleDriveFile(int accId, com.google.api.services.drive.model.File file) {
        super(accId, file.getId(), null, file.getTitle());

        if (file.getParents() != null && file.getParents().size() > 0) {
            parentUId = file.getParents().get(0).getId();
        }
        
        size = file.getFileSize() == null ? 0L : file.getFileSize();
        mimeType = file.getMimeType();
        lastModifiedTime = file.getModifiedDate().getValue();
    }
    
    public GoogleDriveFile() {
        super();
    }
    
    public GoogleDriveFile(GoogleDriveFile obj){
        super(obj);
    }

}
