package com.julystroy.ap_wifi_master.api;


import com.julystroy.ap_wifi_master.adapter.WifiBean;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface WifiService {

    @POST("cgi-bin/wifi/login.cgi")
    Observable<WifiBean> yunLogin(@Body RequestBody body);


    Observable<WifiBean> test();
}
