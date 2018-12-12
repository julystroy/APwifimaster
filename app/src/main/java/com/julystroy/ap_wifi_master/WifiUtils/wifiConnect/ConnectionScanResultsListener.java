package com.julystroy.ap_wifi_master.WifiUtils.wifiConnect;


import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;



public interface ConnectionScanResultsListener
{
    @Nullable
    ScanResult onConnectWithScanResult(@NonNull List<ScanResult> scanResults);
}
