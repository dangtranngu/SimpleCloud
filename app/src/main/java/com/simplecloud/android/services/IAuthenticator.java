package com.simplecloud.android.services;

public interface IAuthenticator {
    void login();
    void logout();
    boolean isLogin();
}

