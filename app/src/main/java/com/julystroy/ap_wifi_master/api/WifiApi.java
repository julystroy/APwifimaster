package com.julystroy.ap_wifi_master.api;



import com.julystroy.ap_wifi_master.WifiAplication;
import com.julystroy.ap_wifi_master.adapter.WifiBean;
import com.julystroy.ap_wifi_master.adapter.YunLogin;
import com.julystroy.ap_wifi_master.manager.RetrofitUrlManager;
import com.julystroy.ap_wifi_master.util.GsonUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.internal.platform.Platform;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class WifiApi {
    public static WifiApi instance;
    private  WifiService wifiService;

    public WifiApi() {

        OkHttpClient builder = RetrofitUrlManager.getInstance().with(new OkHttpClient.Builder()) //RetrofitUrlManager 初始化
                .readTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true) // 失败重发
              //  .addInterceptor(new HeaderInterceptor2())
              //  .addInterceptor(new LoggingInterceptor.Builder()
              //          .setLevel(Level.BASIC)
             //           .log(Platform.INFO)
              //          .build())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("192.168.0.100")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // 添加Rx适配器
                .addConverterFactory(GsonConverterFactory.create()) // 添加Gson转换器
                .client(builder)
                .build();

         wifiService = retrofit.create(WifiService.class);

    }

    public static WifiApi getService() {
        if (instance == null)
            instance = new WifiApi();
        return instance;
    }

    public Observable<WifiBean> lineLogin(String ssid, String pw, String pid ){
        YunLogin login = new YunLogin();
        login.PRODUCT_ID = pid;
        login.SSID = ssid;
        login.PASSWD = pw;
        String gsonString = GsonUtils.createGsonString(login);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gsonString);
        return wifiService.yunLogin(requestBody);
    }

    public Observable<WifiBean> test( ){

        return wifiService.test();
    }
}
