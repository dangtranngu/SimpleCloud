package com.simplecloud.android.common.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

public class NetworkUtils {

    public static boolean isConnected(Context context){
        boolean isConnected = false;
        ConnectivityManager con = (ConnectivityManager) context.getSystemService(
            Context.CONNECTIVITY_SERVICE);
        
        State mobile = con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        if (mobile == NetworkInfo.State.CONNECTED || mobile == NetworkInfo.State.CONNECTING) {
            isConnected = true;
        } 
        
        State wifi = con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING) {
            isConnected = true;
        }
        return isConnected;
    }
}
