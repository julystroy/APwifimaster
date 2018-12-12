package com.julystroy.ap_wifi_master;

import android.app.Application;

import com.julystroy.ap_wifi_master.manager.RetrofitUrlManager;

public class WifiAplication extends Application {
    private static final String AP_WIFI = "192.168.0.100";

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化要动态变化的wifi
        RetrofitUrlManager.getInstance().putDomain("wifi_name", AP_WIFI);
    }
}
