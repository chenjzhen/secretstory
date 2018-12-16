package com.zeus.sdk.test;

import android.content.Context;
import android.content.res.Configuration;

import com.zeus.sdk.plugin.ifc.IApplicationListener;

public class GameProxyApplication implements IApplicationListener {

    @Override
    public void onProxyAttachBaseContext(Context base) {
        // TODO 需要在Application的attachBaseContext中的操作，放在这里
    }

    @Override
    public void onProxyCreate() {
        // TODO 需要在Application的onCreate中的操作，放在这里
        // 如果需要获取Application对象，通过 AresSDK.getInstance().getApplication()来获取
    }

    @Override
    public void onProxyConfigurationChanged(Configuration config) {
        // TODO 需要在Application的onConfigurationChanged中的操作，放在这里
    }

    @Override
    public void onProxyTerminate() {
        // TODO 需要在Application的onTerminate中的操作，放在这里
    }

    @Override
    public void onProxyLowMemory() {
        // TODO 需要在Application的onProxyLowMemory中的操作，放在这里
    }

    @Override
    public void onProxyTrimMemory(int i) {
        // TODO 需要在Application的onProxyTrimMemory中的操作，放在这里
    }
}
