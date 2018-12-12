package com.julystroy.ap_wifi_master.util;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Utils {
    //获取局域网广播码
    public static byte[] getWifiBroadcastIP(Context context) {
        WifiManager wifiMng = ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE));
        DhcpInfo dhcpInfo = wifiMng.getDhcpInfo();

        int bcIp = ~dhcpInfo.netmask | dhcpInfo.ipAddress;
        byte[] retVal = new byte[4];
        retVal[0] = (byte) (bcIp & 0xff);
        retVal[1] = (byte) (bcIp >> 8 & 0xff);
        retVal[2] = (byte) (bcIp >> 16 & 0xff);
        retVal[3] = (byte) (bcIp >> 24 & 0xff);

        return retVal;
    }

    //获取字符串中的数字
    public static String getNum(String text){
        if (text==null) return null;

        String num =null;
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(text);
        if (m!=null){
            num = m.replaceAll("").trim();
        }
        return num;
    }




}
