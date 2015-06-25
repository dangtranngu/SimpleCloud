package com.simplecloud.android.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.simplecloud.android.database.SimpleCloudProvider.Account_Column;
import com.simplecloud.android.models.ObjectFactory;
import com.simplecloud.android.models.accounts.Account;
import com.simplecloud.android.models.accounts.Account.AccountType;

public class AccountDAO {
    
    public static final String LOGCAT = AccountDAO.class.getSimpleName();
    private ContentResolver mContentResolver;
    
    public AccountDAO(Context context) {
        mContentResolver = context.getContentResolver();
    }

    public Account getAccountById(int accId) {
        Cursor c = null;
        
        try {
            c = mContentResolver.query(Account_Column.getContentUri(), null,
                Account_Column._ID + "='" + accId + "'", null, null);
            
            if (c.moveToFirst() && c.isLast()) {
                int accType = c.getInt(c.getColumnIndex(Account_Column.ACC_TYPE));
                Account acc = ObjectFactory.generateNewAccount(AccountType.values()[accType]);

                acc.setId(accId);
                acc.setDisplayName(c.getString(c.getColumnIndex(Account_Column.DISPLAY_NAME)));
                acc.setUserId(c.getString(c.getColumnIndex(Account_Column.USER_ID)));
                acc.setAccessSecret(c.getString(c.getColumnIndex(Account_Column.ACCESS_SECRET)));

                return acc;
            }
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        
        return null;
    }
    
    public List<Account> getAllAccounts() {
        List<Account> accounts = new ArrayList<Account>();
        Cursor c = null;
        
        try {
            c = mContentResolver.query(Account_Column.getContentUri(), null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    int accType = c.getInt(c.getColumnIndex(Account_Column.ACC_TYPE));
                    
                    Account acc = ObjectFactory.generateNewAccount(AccountType.values()[accType]);
                    acc.setId(c.getInt(c.getColumnIndex(Account_Column._ID)));
                    acc.setDisplayName(c.getString(c.getColumnIndex(Account_Column.DISPLAY_NAME)));
                    acc.setUserId(c.getString(c.getColumnIndex(Account_Column.USER_ID)));
                    
                    accounts.add(acc);
                } while (c.moveToNext());
                
            }
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        
        return accounts;
    }

    public boolean removeAccount(int accId) {
        int count = 0;
        String[] selectionArgs = new String[] {accId + ""};
        
        count = mContentResolver.delete(Account_Column.getContentUri(),
                Account_Column._ID + " = ? ", selectionArgs);
       
        return (count > 0);
    }

    /**
     * Insert an Account to database
     * @param object Account to insert
     * @return account id inserted or 0 if failed
     */
    public int insertAccount(Account acc) {

        ContentValues values = new ContentValues();
        
        values.put(Account_Column.DISPLAY_NAME, acc.getDisplayName());
        values.put(Account_Column.USER_ID, acc.getUserId());
        values.put(Account_Column.ACC_TYPE, acc.getAccountType().ordinal());
        values.put(Account_Column.ACCESS_SECRET, acc.getAccessSecret());

        Uri uri = mContentResolver.insert(Account_Column.getContentUri(), values);
        Log.i(LOGCAT, "insertAccount() " + uri);
        if (uri == null)
            return 0;
        
        int rowID = Integer.parseInt(uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));
        return rowID;
    }
    
/*    public int insertLocalAccount() {

        ContentValues values = new ContentValues();
        
        values.put(Account_Column._ID, 0);
        values.put(Account_Column.USER_ID, "Local");
        values.put(Account_Column.ACC_TYPE, AccountType.LOCAL.ordinal());

        Uri uri = mContentResolver.insert(Account_Column.getContentUri(), values);
        Log.i(LOGCAT, "insertLocalAccount() " + uri);
        if (uri == null)
            return 0;
        
        int rowID = Integer.parseInt(uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));
        return rowID;
    }*/
    
    public AccountType getAccountType(int accId) {
        String[] projection = new String[] {Account_Column.ACC_TYPE};
        
        Cursor c = mContentResolver.query(Account_Column.getContentUri(), projection, 
                Account_Column._ID + "='" + accId + "'", null, null);
        
        try {
            if (c != null && c.moveToFirst() && c.isLast()) {
                int accType = c.getInt(c.getColumnIndexOrThrow(Account_Column.ACC_TYPE));
                return AccountType.values()[accType];
            }
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        
        return null;
    }
}
