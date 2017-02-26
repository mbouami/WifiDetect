package com.bouami.com.wifidetect;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Mohammed on 09/02/2017.
 */

public class WifiAdapter extends BaseAdapter {

    private List<WifiItem> listeWifiItem;
    private LayoutInflater layoutInflater;
    private WifiManager wifiManager;

    public WifiAdapter(Context context, List<WifiItem> objects) {

        listeWifiItem = objects;
        layoutInflater = LayoutInflater.from(context);
        wifiManager= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public int getCount() {
        return listeWifiItem.size();
    }

    public Object getItem(int position) {
        return listeWifiItem.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewWifiHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewWifiHolder();

            convertView = layoutInflater.inflate(R.layout.item_wifi, null);

            viewHolder.tvApName = (TextView) convertView.findViewById(R.id.tvWifiName);
            viewHolder.tvAdresseMac = (TextView) convertView.findViewById(R.id.tvWifiMac);
            viewHolder.ForceSignal = (TextView) convertView.findViewById(R.id.ForceSignal);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewWifiHolder)convertView.getTag();
        }
        String wifiencours = wifiManager.getConnectionInfo().getSSID().substring(1,wifiManager.getConnectionInfo().getSSID().length()-1);
        // On affecte les valeurs
        viewHolder.tvApName.setText(listeWifiItem.get(position).getAPName());
        viewHolder.tvAdresseMac.setText(listeWifiItem.get(position).getAdresseMac());
//        Log.d("wifiadapter", "networkId " +viewHolder.tvApName.getText()+"---"+wifiencours);
        if (viewHolder.tvApName.getText().equals(wifiencours)) {
            viewHolder.tvApName.setBackgroundColor(Color.LTGRAY);
        } else {
            viewHolder.tvApName.setBackgroundColor(Color.TRANSPARENT);
        }
        // On change la couleur en fonction de la force du signal
        if(listeWifiItem.get(position).getForceSignal() <= -80) {
            viewHolder.ForceSignal.setBackgroundColor(Color.RED);
        } else if(listeWifiItem.get(position).getForceSignal() <= -50) {
            viewHolder.ForceSignal.setBackgroundColor(Color.YELLOW);
        } else {
            viewHolder.ForceSignal.setBackgroundColor(Color.GREEN);
        }

        return convertView;
    }

    private class ViewWifiHolder {
        TextView tvApName;
        TextView tvAdresseMac;
        TextView ForceSignal;
    }

}
