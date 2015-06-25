package com.simplecloud.android.models.files;

import java.util.ArrayList;
import java.util.List;

import com.simplecloud.android.R;

public class DirectoryObject extends FileSystemObject {

    protected List<FileSystemObject> children = new ArrayList<FileSystemObject>();
    
    
    public DirectoryObject() {
        
    }
    
    public DirectoryObject(int accId, String uid, String parentUId, String name) {
        super(accId, uid, parentUId, name, R.drawable.directory_generic);
        String tmp = name.toLowerCase();
        
        if (tmp.equals("download") || tmp.equals("downloads") ) {
            iconId = R.drawable.directory_download;
            
        } else if (tmp.equals("picture") || tmp.equals("pictures") || tmp.equals("galleries") || tmp.equals("dcim")) {
            iconId = R.drawable.directory_pictures;
            
        } else if (tmp.equals("video") || tmp.equals("videos") || tmp.equals("movies") ) {
            iconId = R.drawable.directory_movies;
            
        } else if (tmp.equals("music")) {
            iconId = R.drawable.directory_music;
            
        }
    }
    
    public DirectoryObject(DirectoryObject dir) {
        super(dir);
        getChildren().clear();
        getChildren().addAll(dir.getChildren());
    }
    
    public void setAll(DirectoryObject dir) {
        super.setAll(dir);
        children.clear();
        children.addAll(dir.getChildren());
    }
    
    public boolean isReadable() {
        return true;
    }

    public List<FileSystemObject> getChildren() {
        return children;
    }

    public void setChildren(List<FileSystemObject> children) {
        this.children = children;
    }
    
    @Override
    public int getIconId() {
        if (iconId == 0)
            iconId = R.drawable.directory_generic;
        return iconId;
    }
}
