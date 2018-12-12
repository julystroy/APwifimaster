package com.julystroy.ap_wifi_master.WifiUtils.wifiConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;


import com.julystroy.ap_wifi_master.WifiUtils.WeakHandler;

import java.util.Objects;


import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.isAlreadyConnected;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.reEnableNetworkIfPossible;
import static com.julystroy.ap_wifi_master.WifiUtils.WifiUtils.wifiLog;
import static java.util.Optional.ofNullable;


public final class WifiConnectionReceiver extends BroadcastReceiver {
    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback;
    @Nullable
    private ScanResult mScanResult;
    @NonNull
    private final WifiManager mWifiManager;
    private long mDelay;
    @NonNull
    private final WeakHandler handler;
    @NonNull
    private final Runnable handlerCallback = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            wifiLog("Connection Timed out...");
            reEnableNetworkIfPossible(mWifiManager, mScanResult);
            if (isAlreadyConnected(mWifiManager, ofNullable(mScanResult).map(scanResult -> scanResult.BSSID).get()))
                mWifiConnectionCallback.successfulConnect();
            else
                mWifiConnectionCallback.errorConnect();
            handler.removeCallbacks(this);
        }
    };

    public WifiConnectionReceiver(@NonNull WifiConnectionCallback callback, @NonNull WifiManager wifiManager, long delayMillis) {
        this.mWifiConnectionCallback = callback;
        this.mWifiManager = wifiManager;
        this.mDelay = delayMillis;
        this.handler = new WeakHandler();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        final String action = intent.getAction();
        wifiLog("Connection Broadcast action: " + action);
        if (Objects.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION, action)) {
            /*
                Note here we dont check if has internet connectivity, because we only validate
                if the connection to the hotspot is active, and not if the hotspot has internet.
             */
            if (isAlreadyConnected(mWifiManager, ofNullable(mScanResult).map(scanResult -> scanResult.BSSID).get())) {
                handler.removeCallbacks(handlerCallback);
                mWifiConnectionCallback.successfulConnect();
            }
        } else if (Objects.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, action)) {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

            if (state == null) {
                handler.removeCallbacks(handlerCallback);
                mWifiConnectionCallback.errorConnect();
                return;
            }

            wifiLog("Connection Broadcast action: " + state);

            switch (state) {
                case COMPLETED:
                case FOUR_WAY_HANDSHAKE:
                    if (isAlreadyConnected(mWifiManager, ofNullable(mScanResult).map(scanResult -> scanResult.BSSID).get())) {
                        handler.removeCallbacks(handlerCallback);
                        mWifiConnectionCallback.successfulConnect();
                    }
                    break;
                case DISCONNECTED:
                    if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                        wifiLog("Authentication error...");
                        handler.removeCallbacks(handlerCallback);
                        mWifiConnectionCallback.errorConnect();
                    } else {
                        wifiLog("Disconnected. Re-attempting to connect...");
                        reEnableNetworkIfPossible(mWifiManager, mScanResult);
                    }
            }
        }
    }

    public void setTimeout(long millis) {
        this.mDelay = millis;
    }

    @NonNull
    public WifiConnectionReceiver activateTimeoutHandler(@NonNull ScanResult result) {
        mScanResult = result;
        handler.postDelayed(handlerCallback, mDelay);
        return this;
    }
}
