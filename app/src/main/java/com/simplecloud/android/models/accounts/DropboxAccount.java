package com.simplecloud.android.models.accounts;

public class DropboxAccount extends Account {

    public DropboxAccount(com.dropbox.client2.DropboxAPI.Account acc, String accessSecret) {
        this.accessSecret = accessSecret;
        displayName = acc.displayName;
        userId = Long.toString(acc.uid);
    }
    
    public DropboxAccount() {
        
    }
    
    @Override
    public AccountType getAccountType() {
        return AccountType.DROPBOX;
    }

}
