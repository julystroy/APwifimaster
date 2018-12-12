package com.julystroy.ap_wifi_master;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.julystroy.ap_wifi_master.WifiUtils.WifiUtils;
import com.julystroy.ap_wifi_master.adapter.ListAdapter;
import com.julystroy.ap_wifi_master.adapter.WifiBean;
import com.julystroy.ap_wifi_master.api.WifiApi;
import com.julystroy.ap_wifi_master.manager.RetrofitUrlManager;
import com.julystroy.ap_wifi_master.manager.UDPSocket;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements ListAdapter.OnsetItemClickListener {
    int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1125;
    private ListView lvWifiDetails;
    private WifiManager mainWifi;
    private WifiReceiver receiverWifi;
    List wifiList;
    private ListAdapter adapter;
    private LinearLayout llConnect;
    private TextView tvDetails;
    private TextView tvTest;
    private UDPSocket socket;

    private boolean isFirst = true;//网络切换次数判断
    private WifiConnectDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //权限校验

        }

        findView();


        // mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        //注册广播
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        scanWifiList();


    }

    private void findView() {
        lvWifiDetails = (ListView) findViewById(R.id.lvWifiDetails);
        llConnect = (LinearLayout) findViewById(R.id.ll_item);
        tvDetails = (TextView) findViewById(R.id.tvDetails);
        tvTest = (TextView) findViewById(R.id.tv_test);

        tvTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //连接AP模式的IP可以写入到工程里，但是硬件连接wifi成功后，会重新被当地路由重新分配一个IP，这时候就需要动态换个新生成的IP
                HttpUrl httpUrl = RetrofitUrlManager.getInstance().fetchDomain("wifi_name");//application 处设置的同名
                if (httpUrl == null || !httpUrl.toString().equals("192.168.0.100")) { //可以在 App 运行时随意切换某个接口的 BaseUrl
                    //  RetrofitUrlManager.getInstance().putDomain(Constant.WIFI, Constant.WIFI_BASE_URL);
                    RetrofitUrlManager.getInstance().setGlobalDomain("udp收到的address");
                    //如果您已经确定了最终的 BaseUrl ,不需要再动态切换 BaseUrl, 请
                    //RetrofitUrlManager.getInstance().setRun(false);
                    // FIXME: 2018/12/12 再进行下面的请求
                    WifiApi.getService().test().compose(MainActivity.this.<WifiBean>getDefaultTransformer())
                            .subscribe(new Observer<WifiBean>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onNext(WifiBean r) {
                                    //成功连接WiFi的硬件，就像个小服务器，可以根据需求进行POST 和 GET请求
                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            lvWifiDetails.setVisibility(View.GONE);
            llConnect.setVisibility(View.VISIBLE);
            scanWifiList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setAdapter() {
        adapter = new ListAdapter(getApplicationContext(), wifiList);
        adapter.setclick(this);
        lvWifiDetails.setAdapter(adapter);
    }

    private void scanWifiList() {
        mainWifi.startScan();
        wifiList = mainWifi.getScanResults();
        setAdapter();
    }

    @Override//连接wifi
    public void click(ScanResult result) {
        lvWifiDetails.setVisibility(View.GONE);
        llConnect.setVisibility(View.VISIBLE);
        tvDetails.setText("SSID :: " + result.SSID
                + "\nStrength :: " + result.level
                + "\nBSSID :: " + result.BSSID)
        ;

        //链接AP路由器  AP模式下密码为空
        connectWithWpa(result.SSID, "");
    }

    private void connectWithWpa(String ssid, String pw) {
        WifiUtils.withContext(getApplicationContext())
                .connectWith(ssid, pw)
                .setTimeout(40000)
                .onConnectionResult(this::checkResult)
                .start();
    }

    private void checkResult(boolean isSuccess) {
        if (isSuccess) {
            if (isFirst) {

                //第一次如果链接成功，向AP路由发送WiFissid以及password
                showDialog();

            } else {
                //切换成功后，启动广播，以及接收
                socket = new UDPSocket(MainActivity.this);
                socket.startUDPSocket();
                socket.startHeartbeatTimer("发送的广播内容，随便定义");
            }

        } else {
            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
        }
    }

    //wifi_connect dialog
    private void showDialog() {
        dialog = new WifiConnectDialog(MainActivity.this);
        dialog.setcListener(new WifiConnectDialog.setOnSureClickListener() {
            @Override
            public void onClickListener(String wifiNmae, String pw) {

                //  发送WiFi ssid以及pwy以及硬件需要的数据
                WifiApi.getService().lineLogin(wifiNmae, pw, "ACESPillow_neck20180906000000001").compose(MainActivity.this.<WifiBean>getDefaultTransformer())
                        .subscribe(new Observer<WifiBean>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(WifiBean r) {
                                //成功返回后，进行网络切换，切换到家庭WiFi
                                isFirst = false;
                                //在这里可以用sp把之前的家庭WiFi ssid以及pw保存起来
                                connectWithWpa("ssid", "pw");
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    //广播
    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
        }
    }


    private void checkRunTimePermission() {
        String[] permissionArrays = new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissionArrays, 11111);
        } else {
            // if already permition granted
            // PUT YOUR ACTION (Like Open cemara etc..)
        }
    }

    @Override//权限返回结果
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
        }
    }

    private <T> ObservableTransformer<T, T> getDefaultTransformer() {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.subscribeOn(Schedulers.io())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {

                            }
                        })
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doAfterTerminate(new Action() {
                            @Override
                            public void run() throws Exception {

                            }
                        });
            }
        };
    }
}