package com.julystroy.ap_wifi_master.adapter;

/**
 * Created by Diku on 08-06-2017.
 */
import   java.util.List;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.julystroy.ap_wifi_master.R;

public class ListAdapter extends BaseAdapter {
    Context context;
    LayoutInflater inflater;
    List<ScanResult> wifiList;
    private OnsetItemClickListener listener;

    public ListAdapter(Context context, List<ScanResult> wifiList) {
        this.context = context;
        this.wifiList = wifiList;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return wifiList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public interface OnsetItemClickListener{
        void click(ScanResult result);
    }
    public void setclick(OnsetItemClickListener listener){
        this.listener = listener;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        System.out.println("viewpos" + position);
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.layout_wifilist, null);
            holder = new Holder();
            holder.tvDetails = (TextView) view.findViewById(R.id.tvDetails);
            holder.llItem = (LinearLayout) view.findViewById(R.id.ll_item);

            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        holder.tvDetails.setText("SSID :: " + wifiList.get(position).SSID
                + "\nStrength :: " + wifiList.get(position).level
                + "\nBSSID :: " + wifiList.get(position).BSSID
                + "\nChannel :: "
                + convertFrequencyToChannel(wifiList.get(position).frequency)
                + "\nFrequency :: " + wifiList.get(position).frequency
                + "\nCapability :: " + wifiList.get(position).capabilities);
        holder.llItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.click( wifiList.get(position));
            }
        });

        return view;
    }

    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    class Holder {
        TextView tvDetails;
        LinearLayout llItem;

    }
}
