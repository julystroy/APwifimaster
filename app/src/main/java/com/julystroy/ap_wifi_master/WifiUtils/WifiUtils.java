package com.julystroy.ap_wifi_master.WifiUtils;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;


import com.julystroy.ap_wifi_master.WifiUtils.wifiConnect.ConnectionScanResultsListener;
import com.julystroy.ap_wifi_master.WifiUtils.wifiConnect.ConnectionSuccessListener;
import com.julystroy.ap_wifi_master.WifiUtils.wifiConnect.WifiConnectionCallback;
import com.julystroy.ap_wifi_master.WifiUtils.wifiConnect.WifiConnectionReceiver;
import com.julystroy.ap_wifi_master.WifiUtils.wifiScan.ScanResultsListener;
import com.julystroy.ap_wifi_master.WifiUtils.wifiScan.WifiScanCallback;
import com.julystroy.ap_wifi_master.WifiUtils.wifiScan.WifiScanReceiver;
import com.julystroy.ap_wifi_master.WifiUtils.wifiState.WifiStateCallback;
import com.julystroy.ap_wifi_master.WifiUtils.wifiState.WifiStateListener;
import com.julystroy.ap_wifi_master.WifiUtils.wifiState.WifiStateReceiver;
import com.julystroy.ap_wifi_master.WifiUtils.wifiWps.ConnectionWpsListener;

import java.util.ArrayList;
import java.util.List;

import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.cleanPreviousConfiguration;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.connectToWifi;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.connectWps;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.matchScanResult;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.matchScanResultBssid;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.matchScanResultSsid;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.reenableAllHotspots;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.registerReceiver;
import static com.julystroy.ap_wifi_master.WifiUtils.ConnectorUtils.unregisterReceiver;
import static java.util.Optional.ofNullable;


public final class WifiUtils implements WifiConnectorBuilder,
        WifiConnectorBuilder.WifiUtilsBuilder,
        WifiConnectorBuilder.WifiSuccessListener,
        WifiConnectorBuilder.WifiWpsSuccessListener {
    @NonNull
    private final WifiManager mWifiManager;
    @NonNull
    private final Context mContext;
    private static boolean mEnableLog;
    private long mWpsTimeoutMillis = 30000;
    private long mTimeoutMillis = 30000;
    @NonNull
    private static final String TAG = WifiUtils.class.getSimpleName();
    //@NonNull private static final WifiUtils INSTANCE = new WifiUtils();
    @NonNull
    private final WifiStateReceiver mWifiStateReceiver;
    @NonNull
    private final WifiConnectionReceiver mWifiConnectionReceiver;
    @NonNull
    private final WifiScanReceiver mWifiScanReceiver;
    @Nullable
    private String mSsid;
    @Nullable
    private String mBssid;
    @Nullable
    private String mPassword;
    @Nullable
    private ScanResult mSingleScanResult;
    @Nullable
    private ScanResultsListener mScanResultsListener;
    @Nullable
    private ConnectionScanResultsListener mConnectionScanResultsListener;
    @Nullable
    private ConnectionSuccessListener mConnectionSuccessListener;
    @Nullable
    private WifiStateListener mWifiStateListener;
    @Nullable
    private ConnectionWpsListener mConnectionWpsListener;

    @NonNull
    private final WifiStateCallback mWifiStateCallback = new WifiStateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onWifiEnabled() {
            wifiLog("WIFI ENABLED...");
            unregisterReceiver(mContext, mWifiStateReceiver);
            ofNullable(mWifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(true));



            if (mScanResultsListener != null || mPassword != null) {
                wifiLog("START SCANNING....");
                if (mWifiManager.startScan())
                    registerReceiver(mContext, mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                else {
                    ofNullable(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                    ofNullable(mConnectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                    mWifiConnectionCallback.errorConnect();
                    wifiLog("ERROR COULDN'T SCAN");
                }
            }
        }
    };

    @NonNull
    private final WifiScanCallback mWifiScanResultsCallback = new WifiScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onScanResultsReady() {
            wifiLog("GOT SCAN RESULTS");
            unregisterReceiver(mContext, mWifiScanReceiver);

            final List<ScanResult> scanResultList = mWifiManager.getScanResults();
            ofNullable(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(scanResultList));
            ofNullable(mConnectionScanResultsListener).ifPresent(connectionResultsListener -> mSingleScanResult = connectionResultsListener.onConnectWithScanResult(scanResultList));

            if (mConnectionWpsListener != null && mBssid != null && mPassword != null) {
                mSingleScanResult = matchScanResultBssid(mBssid, scanResultList);
                if (mSingleScanResult != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    connectWps(mWifiManager, mSingleScanResult, mPassword, mWpsTimeoutMillis, mConnectionWpsListener);
                else {
                    if (mSingleScanResult == null)
                        wifiLog("Couldn't find network. Possibly out of range");
                    mConnectionWpsListener.isSuccessful(false);
                }
                return;
            }

            if (mSsid != null) {
                if (mBssid != null)
                    mSingleScanResult = matchScanResult(mSsid, mBssid, scanResultList);
                else
                    mSingleScanResult = matchScanResultSsid(mSsid, scanResultList);
            }
            if (mSingleScanResult != null && mPassword != null) {
                if (connectToWifi(mContext, mWifiManager, mSingleScanResult, mPassword)) {
                    registerReceiver(mContext, mWifiConnectionReceiver.activateTimeoutHandler(mSingleScanResult),
                            new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                    registerReceiver(mContext, mWifiConnectionReceiver,
                            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                } else
                    mWifiConnectionCallback.errorConnect();
            } else
                mWifiConnectionCallback.errorConnect();
        }
    };

    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback = new WifiConnectionCallback() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void successfulConnect() {
            wifiLog("CONNECTED SUCCESSFULLY");
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            //reenableAllHotspots(mWifiManager);
            ofNullable(mConnectionSuccessListener).ifPresent(successListener -> successListener.isSuccessful(true));
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void errorConnect() {
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            reenableAllHotspots(mWifiManager);
            //if (mSingleScanResult != null)
            //cleanPreviousConfiguration(mWifiManager, mSingleScanResult);
            ofNullable(mConnectionSuccessListener).ifPresent(successListener -> {
                successListener.isSuccessful(false);
                wifiLog("DIDN'T CONNECT TO WIFI");
            });
        }
    };

    private WifiUtils(@NonNull Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null)
            throw new RuntimeException("WifiManager is not supposed to be null");
        mWifiStateReceiver = new WifiStateReceiver(mWifiStateCallback);
        mWifiScanReceiver = new WifiScanReceiver(mWifiScanResultsCallback);
        mWifiConnectionReceiver = new WifiConnectionReceiver(mWifiConnectionCallback, mWifiManager, mTimeoutMillis);
    }

    public static WifiUtilsBuilder withContext(@NonNull final Context context) {
        return new WifiUtils(context);
    }

    public static void wifiLog(final String text) {
        if (mEnableLog)
            Log.d(TAG, "WifiUtils: " + text);
    }

    public static void enableLog(final boolean enabled) {
        mEnableLog = enabled;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void enableWifi(@Nullable final WifiStateListener wifiStateListener) {
        mWifiStateListener = wifiStateListener;
        if (mWifiManager.isWifiEnabled())
            mWifiStateCallback.onWifiEnabled();
        else {
            if (mWifiManager.setWifiEnabled(true))
                registerReceiver(mContext, mWifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            else {
                ofNullable(wifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(false));
                ofNullable(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                ofNullable(mConnectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                mWifiConnectionCallback.errorConnect();
                wifiLog("COULDN'T ENABLE WIFI");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void enableWifi() {
        enableWifi(null);
    }

    @NonNull
    @Override
    public WifiConnectorBuilder scanWifi(final ScanResultsListener scanResultsListener) {
        mScanResultsListener = scanResultsListener;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String password) {
        mSsid = ssid;
        mPassword = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String bssid, @NonNull final String password) {
        mSsid = ssid;
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWithScanResult(@NonNull final String password,
                                                     @Nullable final ConnectionScanResultsListener connectionScanResultsListener) {
        mConnectionScanResultsListener = connectionScanResultsListener;
        mPassword = password;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiWpsSuccessListener connectWithWps(@NonNull final String bssid, @NonNull final String password) {
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void cancelAutoConnect() {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        ofNullable(mSingleScanResult).ifPresent(scanResult -> cleanPreviousConfiguration(mWifiManager, scanResult));
        reenableAllHotspots(mWifiManager);
    }

    @NonNull
    @Override
    public WifiSuccessListener setTimeout(final long timeOutMillis) {
        mTimeoutMillis = timeOutMillis;
        mWifiConnectionReceiver.setTimeout(timeOutMillis);
        return this;
    }

    @NonNull
    @Override
    public WifiWpsSuccessListener setWpsTimeout(final long timeOutMillis) {
        mWpsTimeoutMillis = timeOutMillis;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiConnectorBuilder onConnectionWpsResult(@Nullable final ConnectionWpsListener successListener) {
        mConnectionWpsListener = successListener;
        return this;
    }


    @NonNull
    @Override
    public WifiConnectorBuilder onConnectionResult(@Nullable final ConnectionSuccessListener successListener) {
        mConnectionSuccessListener = successListener;
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void start() {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        enableWifi(null);
    }

    @Override
    public void disableWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            unregisterReceiver(mContext, mWifiStateReceiver);
            unregisterReceiver(mContext, mWifiScanReceiver);
            unregisterReceiver(mContext, mWifiConnectionReceiver);
        }
        wifiLog("WiFi Disabled");
    }
}
