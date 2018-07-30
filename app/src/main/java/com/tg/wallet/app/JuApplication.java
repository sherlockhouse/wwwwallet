package com.tg.wallet.app;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.zhouyou.http.EasyHttp;

import com.tg.wallet.AppFilePath;



public class JuApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppFilePath.init(this);
        EasyHttp.init(this);//默认初始化
    }
    @Override
    public void attachBaseContext(Context base) {
        MultiDex.install(base);
        super.attachBaseContext(base);
    }
}
