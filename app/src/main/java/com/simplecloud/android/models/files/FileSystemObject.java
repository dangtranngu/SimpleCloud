package com.simplecloud.android.models.files;



public class FileSystemObject {

    protected String name = "";
    protected String parentUId = "";
    protected String uid = ""; // unique for each file 
    protected int iconId;
    protected long size;
    protected String mimeType = "";
    protected long lastModifiedTime;
    protected String hash = ""; // current file's version (only for Dropbox at the moment)
    protected int accId;
    
    public FileSystemObject() {
        
    }
    
    public FileSystemObject(int accId, String uid, String parentUId, String name, int iconId) {
        this.accId = accId;
        this.name = name;
        this.uid = uid;
        this.parentUId = parentUId;
        this.iconId = iconId;
    }

    public FileSystemObject(FileSystemObject obj) {
        name = obj.getName();
        parentUId = obj.getParentUId();
        uid = obj.getUId();
        size = obj.getSize();
        mimeType = obj.getMimeType();
        lastModifiedTime = obj.getLastModifiedTime();
        accId = obj.getAccId();
        iconId = obj.getIconId();
        hash = obj.getHash();
    }
    
    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public void setParentUId(String parentUId) {
        this.parentUId = parentUId;
    }

    public String getParentUId() {
        return parentUId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getUId() {
        return uid;
    }
    
    public void setUId(String uid) {
        this.uid = uid;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getAccId() {
        return accId;
    }

    public void setAccId(int accId) {
        this.accId = accId;
    }
    
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public void setAll(FileSystemObject obj) {
        name = obj.getName();
        parentUId = obj.getParentUId();
        uid = obj.getUId();
        size = obj.getSize();
        mimeType = obj.getMimeType();
        lastModifiedTime = obj.getLastModifiedTime();
        hash = obj.getHash();
    }

    
    @Override
    public String toString() {
        return uid;
    }

    
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof FileSystemObject)) 
            return false;
        
        FileSystemObject f = (FileSystemObject) obj;

        return accId == f.getAccId() && uid.equalsIgnoreCase(f.getUId());

    }
    
    public boolean isIdentical(FileSystemObject f) {
        return uid.equalsIgnoreCase(f.getUId())
                && accId == f.getAccId()
                && name.equalsIgnoreCase(f.getName())
                && parentUId.equalsIgnoreCase(f.getParentUId())
                && mimeType.equals(f.getMimeType())
                && size == f.getSize()
                && lastModifiedTime == f.getLastModifiedTime()
                && hash.equals(f.getHash());

    }

}
