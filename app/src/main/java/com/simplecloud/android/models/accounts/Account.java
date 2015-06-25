package com.simplecloud.android.models.accounts;

public abstract class Account {

    /**
     * Caution: order is important as it specified 
     * values of column account_type in table Account
     */
    public enum AccountType {
        DROPBOX, GOOGLEDRIVE
    }
    
    protected int id = 0; // row id of the account stored in table Account
    protected String displayName;
    protected String userId; // unique identity of the account
    protected String accessSecret;


    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessSecret() {
        return accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }
    
    public abstract AccountType getAccountType();
    
}