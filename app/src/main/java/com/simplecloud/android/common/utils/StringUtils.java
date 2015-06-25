package com.simplecloud.android.common.utils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;



public class StringUtils {
    private static final String LOGCAT = StringUtils.class.getSimpleName();
 
    public static String parseName(String path) {
        return new File(path).getName();
    }

    public static String parseParent(String path) {
        return new File(path).getParent();
    }
    
    /** Escape special characters before querying database
     * @param str
     * @return
     */
    public static String escapeSpecialChars(String str) {
        return str.replaceAll("'", "''");
    }
    
    public static String parseDate(String date) {
        // Thu, 27 Sep 2012 13:44:09 +0000
        SimpleDateFormat dfDb = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        Date dateDb = null;
        try {
            dateDb = dfDb.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (dateDb == null) {
            // 27 Sep 2012 13:44:09 +0000
            dfDb = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");
            try {
                dateDb = dfDb.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        
        // 27/09/2012 13:44:09
        SimpleDateFormat toYours = new SimpleDateFormat("dd/mm/yyyy HH:mm:ss");
        String yourString = toYours.format(dateDb);
        return yourString;
    }
}
