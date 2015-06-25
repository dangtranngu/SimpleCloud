package com.simplecloud.android.models.accounts;

public class GoogleDriveAccount extends Account{

    public GoogleDriveAccount(com.google.api.services.drive.model.User acc){
        displayName = acc.getDisplayName();
        userId = acc.getEmailAddress();
    }

    public GoogleDriveAccount() {

    }

    @Override
    public AccountType getAccountType() {
        return AccountType.GOOGLEDRIVE;
    }
}
