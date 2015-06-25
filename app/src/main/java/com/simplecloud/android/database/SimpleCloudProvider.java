package com.simplecloud.android.database;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class SimpleCloudProvider extends ContentProvider {

    private static final String LOGCAT = SimpleCloudProvider.class.getSimpleName();

    SimpleCloudDBHelper mDatabaseHelper;

    public static final String AUTHORITY = "com.application.simplecloud.database.SimpleCloud";

    private static final int FILE = 1;
    private static final int FILE_ID = 2;
    private static final int ACCOUNT = 3;
    private static final int ACCOUNT_ID = 4;
    
    private static final int QUERY_URI = 1;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    
    static {
        URI_MATCHER.addURI(AUTHORITY, SimpleCloudDBHelper.TABLE_FILE, FILE);
        URI_MATCHER.addURI(AUTHORITY, SimpleCloudDBHelper.TABLE_FILE + "/#", FILE_ID);
        URI_MATCHER.addURI(AUTHORITY, SimpleCloudDBHelper.TABLE_ACCOUNT, ACCOUNT);
        URI_MATCHER.addURI(AUTHORITY, SimpleCloudDBHelper.TABLE_ACCOUNT + "/#", ACCOUNT_ID);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new SimpleCloudDBHelper(getContext());
        return mDatabaseHelper == null ? true : false;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        String groupBy = null;
        Long rowId = (long) 0;
        
        switch (URI_MATCHER.match(uri)) {
            
            case FILE:
                qb.setTables(SimpleCloudDBHelper.TABLE_FILE);
                break;
                
            case FILE_ID:
                try {
                    rowId = Long.decode(uri.getPathSegments().get(QUERY_URI));
                } catch (NumberFormatException ex) {
                    Log.e(LOGCAT, uri.getPathSegments().get(QUERY_URI), ex);
                }
                qb.appendWhere(File_Column._ID + "=" + rowId);
                break;
 
            case ACCOUNT:
                qb.setTables(SimpleCloudDBHelper.TABLE_ACCOUNT);
                break;
                
            case ACCOUNT_ID:
                try {
                    rowId = Long.decode(uri.getPathSegments().get(QUERY_URI));
                } catch (NumberFormatException ex) {
                    Log.e(LOGCAT, uri.getPathSegments().get(QUERY_URI), ex);
                }
                qb.appendWhere(Account_Column._ID + "=" + rowId);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
    

    @Override
    public synchronized int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        
        int rowsAdded = 0;
        long rowId = 0;
        
        switch (URI_MATCHER.match(uri)) {
            case FILE:
                try {
                    db.beginTransaction();

                    for (ContentValues v : values) {
                        rowId = db.insert(SimpleCloudDBHelper.TABLE_FILE, null, v);
                        if (rowId > 0)
                            rowsAdded++;
                    }

                    db.setTransactionSuccessful();
                } catch (SQLException ex) {
                    Log.e(LOGCAT, "bulkInsert() FILE", ex);
                } finally {
                    db.endTransaction();
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return rowsAdded;
    }
    

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        String table = null;
        Uri newUri = null;
        long rowId = 0;

        switch (URI_MATCHER.match(uri)) {
            case FILE:
                if (values == null)
                    break;
                
                table = SimpleCloudDBHelper.TABLE_FILE;
                boolean isLocalBookmark = (values.getAsInteger(File_Column.ACC_ID) == 0) ? true : false;
                try {
                    if (isLocalBookmark)
                        db.execSQL("PRAGMA foreign_keys=OFF;");
                    
                    rowId = db.insert(table, null, values);
                } catch (IllegalStateException ex) {
                    Log.e(LOGCAT, "insert() FILE", ex);
                } finally {
                    if (isLocalBookmark)
                        db.execSQL("PRAGMA foreign_keys=ON;");
                }
                if (rowId > 0) {
                    newUri = File_Column.getContentUri(rowId);
                }
                break;

            case ACCOUNT:
                if (values == null)
                    break;

                table = SimpleCloudDBHelper.TABLE_ACCOUNT;
                try {
                    rowId = db.insert(table, null, values);
                } catch (IllegalStateException ex) {
                    Log.e(LOGCAT, "insert() ACCOUNT", ex);
                }
                if (rowId > 0) {
                    newUri = Account_Column.getContentUri(rowId);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        return newUri;
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        String finalSelection;
        int count = 0;
        
        switch (URI_MATCHER.match(uri)) {
            case FILE:
                count = db.delete(SimpleCloudDBHelper.TABLE_FILE, selection, selectionArgs);
                break;

            case FILE_ID:
                finalSelection = File_Column._ID + "=" + uri.getPathSegments().get(QUERY_URI);
                
                if (!TextUtils.isEmpty(selection))
                    finalSelection = finalSelection + " AND " + selection;
                
                count = db.delete(SimpleCloudDBHelper.TABLE_FILE, finalSelection, selectionArgs);
                break;

            case ACCOUNT:
                count = db.delete(SimpleCloudDBHelper.TABLE_ACCOUNT, selection, selectionArgs);
                break;

            case ACCOUNT_ID:
                finalSelection = File_Column._ID + "=" + uri.getPathSegments().get(QUERY_URI);
                
                if (!TextUtils.isEmpty(selection))
                    finalSelection = finalSelection + " AND " + selection;
                
                count = db.delete(SimpleCloudDBHelper.TABLE_ACCOUNT, finalSelection, selectionArgs);
                break;

        }

        return count;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        String finalSelection;
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case FILE:
                count = db.update(SimpleCloudDBHelper.TABLE_FILE, values, selection, selectionArgs);
                break;

            case FILE_ID:
                finalSelection = File_Column._ID + "=" + uri.getPathSegments().get(QUERY_URI);
                
                if (!TextUtils.isEmpty(selection))
                    finalSelection = finalSelection + " AND " + selection;
                
                count = db.update(SimpleCloudDBHelper.TABLE_FILE, values, finalSelection, selectionArgs);
                break;

            case ACCOUNT:
                count = db.update(SimpleCloudDBHelper.TABLE_ACCOUNT, values, selection, selectionArgs);
                break;

            case ACCOUNT_ID:
                finalSelection = File_Column._ID + "=" + uri.getPathSegments().get(QUERY_URI);
                
                if (!TextUtils.isEmpty(selection))
                    finalSelection = finalSelection + " AND " + selection;
                
                count = db.update(SimpleCloudDBHelper.TABLE_ACCOUNT, values, finalSelection, selectionArgs);
                break;
        }

        return count;
    }
    
    
    public static final class Account_Column implements BaseColumns {
        
        public final static String DISPLAY_NAME = "display_name"; // displayed in the navigation drawer
        public final static String ACC_TYPE = "acc_type";
        public final static String USER_ID = "uid"; // unique identity (email ...)
        public final static String ACCESS_SECRET = "access_secret";


        public static Uri getContentUri() {
            return Uri.parse("content://" + SimpleCloudProvider.AUTHORITY + "/account");
        }

        public static Uri getContentUri(long rowId) {
            return Uri.parse("content://" + SimpleCloudProvider.AUTHORITY + "/account/" + rowId);
        }

    }
    
    public static final class Bookmark_Column implements BaseColumns {
        
        public final static String UID = "file_uid"; // Unique representation of file
        public final static String NAME = "file_name";
        public final static String IS_DIR = "is_directory";
        public final static String ACC_ID = "account_id"; // foreign key link to Account table

        public static Uri getContentUri() {
            return Uri.parse("content://"
                + SimpleCloudProvider.AUTHORITY + "/bookmark");
        }

        public static Uri getContentUri(long rowId) {
            return Uri.parse("content://"
                + SimpleCloudProvider.AUTHORITY + "/bookmark/" + rowId);
        }
    }
    
    public static final class File_Column implements BaseColumns {
        
        public final static String UID = "file_uid"; // Unique representation of file
        public final static String NAME = "file_name";
        public final static String PARENT_UID = "parent_uid";
        public final static String IS_DIR = "is_directory";
        public final static String MIMETYPE = "mime_type";
        public final static String SIZE = "file_size";
        public final static String MODIFIED_DATE = "modified_date";
        public final static String HASH = "hash";
        public final static String ACC_ID = "account_id"; // foreign key link to Account table
        public final static String BOOKMARK = "is_bookmark";

        public static Uri getContentUri() {
            return Uri.parse("content://" + SimpleCloudProvider.AUTHORITY + "/file");
        }

        public static Uri getContentUri(long rowId) {
            return Uri.parse("content://" + SimpleCloudProvider.AUTHORITY + "/file/" + rowId);
        }
    }

}
