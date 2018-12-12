package com.julystroy.ap_wifi_master;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;


public class WifiConnectDialog extends Dialog {

    private String pw;
    private String ssid;
    private setOnSureClickListener listener;

    public interface setOnSureClickListener{
        void onClickListener(String wifiNmae, String pw);
    }
    public void setcListener(setOnSureClickListener listener) {
        this.listener =listener;
    }
    public WifiConnectDialog(Context context) {
        super(context, R.style.Common_Dialog);
        //修改显示位置 本质就是修改 WindowManager.LayoutParams让内容水平居中 底部对齐
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        //android:gravity=bottom|center_horizonal
        attributes.gravity= Gravity.CENTER| Gravity.CENTER_HORIZONTAL;
        getWindow().setAttributes(attributes);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_wificonnect);
        EditText wifiNmae = (EditText) findViewById(R.id.wifi_name);
        EditText wifiPw    = (EditText) findViewById(R.id.wifi_pw);
        TextView tvCancel  = (TextView) findViewById(R.id.cancel);
        TextView TVsure   = (TextView) findViewById(R.id.sure);

        if (ssid != null && pw != null) {
            wifiNmae.setText(ssid);
            wifiPw.setText(pw);
        }
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        TVsure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = wifiNmae.getText().toString();
                String pw = wifiPw.getText().toString();
                if (name != null && !name.equals("")) {
                    if (pw != null && !pw.equals("")) {
                        if (listener != null){
                            listener.onClickListener(name,pw);
                        }
                    }
                }
            }


        });

    }
}
