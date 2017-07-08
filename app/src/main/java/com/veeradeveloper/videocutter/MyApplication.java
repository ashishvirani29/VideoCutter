package com.veeradeveloper.videocutter;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

/**
 * Created by vaishaliarora on 16/06/16.
 */
public class MyApplication extends Application {

    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        applicationHandler = new Handler(applicationContext.getMainLooper());
    }
}
