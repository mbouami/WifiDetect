package com.bouami.com.wifidetect;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = "WifiDetect";
    private Button boutonRechercher;
    private ListView listeViewWifi;
    private List<WifiItem> listeWifiItem;
    private WifiAdapter wifiAdapter;
    public WifiManager wifiManager;
    private WifiBroadcastReceiver broadcastReceiver;
    private ConnectionManager connectionManager;
    private ConnectivityManager mconnectivityManager;
    private WifiConfiguration mwifiConfiguration;
    WifiProxyManager mWifiProxyManager;
    private TextView info;
    private TextView ssid;
    private TextView mdp;
    private TextView proxy;
    private TextView port;
    private final int REQUEST_PERMISSION_STATE = 1;
    private final int ALREADY_CONNECTED = 0;
    private final int SSID_NOT_FOUND = -1;
    private final int UNABLE_TO_FIND_SECURITY_TYPE = -2;
    private final int CONNECTION_REQUESTED = -3;

    private String internetUrl = "http://dane.ac-creteil.fr/IMG/config/boutons/DANE_2016.jpg";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public void setPacUrl(String pacUrl,WifiManager manager) {
        //Get the current wifi manager
//        WifiManager manager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration configuration = getCurrentWifiConfiguration(manager);
        if(configuration == null) {
            return;
        }

        try {
            Class proxySettings = Class.forName("android.net.IpConfiguration$ProxySettings");
            Class[] setProxyParams = new Class[2];
            setProxyParams[0] = proxySettings;
            setProxyParams[1] = ProxyInfo.class;

            //Change accessibility on WifiConfiguration.setProxy method to allow us to call it
            Class wifiConfig = Class.forName("android.net.wifi.WifiConfiguration");
            Method setProxy = wifiConfig.getDeclaredMethod("setProxy", setProxyParams);
            setProxy.setAccessible(true);
            ProxyInfo desiredProxy = ProxyInfo.buildDirectProxy("172.16.0.1", 3128);
//            ProxyInfo autoConfig = ProxyInfo.buildPacProxy(Uri.parse(pacUrl));

            //build our method parameters being passed
            Object[] methodParams = new Object[2];
            methodParams[0] = Enum.valueOf(proxySettings,"STATIC");
            methodParams[1] = desiredProxy;

            //Pass the enum to setProxy

            setProxy.invoke(configuration, methodParams);

            //save the configuration
            manager.updateNetwork(configuration);
            manager.disconnect();
            manager.reconnect();
        }catch (Exception e) {

        }
    }

    private WifiConfiguration getCurrentWifiConfiguration(WifiManager manager) {
        if(!manager.isWifiEnabled()) {
            return null;
        }
        List<WifiConfiguration> configurations = manager.getConfiguredNetworks();
        WifiConfiguration current = null;
        int networkId = manager.getConnectionInfo().getNetworkId();
        for(WifiConfiguration selection : configurations) {
            if(selection.networkId == networkId) {
                current = selection;
                break;
            }
        }
        return current;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ImageView targetImageView = (ImageView) findViewById(R.id.targetImageView);
        Picasso
                .with(this)
                .load(internetUrl)
                .resize(450,300)
                .centerCrop()
                .into(targetImageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_STATE);
        }

        listeViewWifi = (ListView) findViewById(R.id.listViewWifi);
        boutonRechercher = (Button) findViewById(R.id.buttonRefresh);
        info = (TextView) findViewById(R.id.message);
        ssid = (TextView) findViewById(R.id.ssid);
        mdp = (TextView) findViewById(R.id.mdp);
        proxy = (TextView) findViewById(R.id.proxy);
        port = (TextView) findViewById(R.id.port);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        mconnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectionManager=new ConnectionManager(this,wifiManager);
        connectionManager.enableWifi();
        boutonRechercher.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (wifiManager != null)
                    wifiManager.startScan();
            }
        });
        listeWifiItem = new ArrayList<WifiItem>();
        wifiAdapter = new WifiAdapter(this, listeWifiItem);
        listeViewWifi.setAdapter(wifiAdapter);
        listeViewWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                ssid.setText(listeWifiItem.get(position).getAPName());
                ssid.setText(listeWifiItem.get(position).getAPName());
                mdp.setText("");
                proxy.setText("");
                port.setText("");
                ((LinearLayout) findViewById(R.id.zonessid)).setVisibility(View.VISIBLE);
            }
        });

        // Création du broadcast Receiver
        broadcastReceiver = new WifiBroadcastReceiver();

        // On attache le receiver au scan result
        registerReceiver(broadcastReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    // On remet en route le receiver quand on revient sur l'application
    @Override
    protected void onResume() {
        registerReceiver(broadcastReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    public WifiManager getCurrentWifiManager() {
        return wifiManager;
    }

    public WifiAdapter getWifiAdapter() {
        return wifiAdapter;
    }

    public List<WifiItem> getListeWifiItem() {
        return listeWifiItem;
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("Réseau", "onRequestPermissionsResult " + requestCode);
        switch (requestCode) {
            case REQUEST_PERMISSION_STATE: {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                    return;
                }
        }
    }

    public void seConnecterauWifi(View view) {
//        Log.d("mconnectivityManager","Nombre de réseaux : "+(mconnectivityManager.getActiveNetworkInfo().getType()==ConnectivityManager.TYPE_WIFI));
//        for (Network scanNetwork : mconnectivityManager.getAllNetworks()) {
//            Log.d("mconnectivityManager",mconnectivityManager.getNetworkInfo(scanNetwork).toString());
//            Log.d("mconnectivityManager",mconnectivityManager.getNetworkCapabilities(scanNetwork).toString());
//
//        }
        if (!ssid.getText().equals("")) {
//            wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
            int leport = 0;
            if (port.getText().length() == 0) {
                leport = 0;
            } else {
                leport = Integer.parseInt(port.getText().toString(), 10);
            }
            int retour = connectionManager.requestWIFIConnection(ssid.getText().toString(),
                    mdp.getText().toString(), 90,
                    proxy.getText().toString(), leport);
        }
    }

    private class ShowToast {
        public ShowToast(Context context, String s) {
            Toast.makeText(context,s,Toast.LENGTH_LONG).show();
        }
    }

    public class WifiBroadcastReceiver extends BroadcastReceiver {

        private WifiManager wifiManager;
        private WifiAdapter wifiAdapter;
        private List<WifiItem> listeWifiItem;

        @Override
        public void onReceive(Context context, Intent intent) {
            wifiManager = ((MainActivity) context).getCurrentWifiManager();
            wifiAdapter = ((MainActivity) context).getWifiAdapter();
            listeWifiItem = ((MainActivity) context).getListeWifiItem();
            // On vérifie que notre objet est bien instancié
            if (wifiManager != null) {
//                info.setText("onReceive : "+wifiManager.getConnectionInfo().getSSID());
                // On vérifie que le WiFi est allumé
                if (wifiManager.isWifiEnabled()) {
                    // On récupère les scans
//                    wifiManager.startScan();
                    List<ScanResult> listeScan = wifiManager.getScanResults();
//                    List<WifiConfiguration> configwifi = wifiManager.getConfiguredNetworks();
//                    for (int i = 0; i < listeScan.size(); i++) {
//                        Log.d("Réseau", "WifiConfiguration " + i + " : " + listeScan.get(i).SSID + "--" + listeScan.get(i).BSSID);
//                    }
//                    wifiManager.setTdlsEnabledWithMacAddress("b8:26:6c:c9:57:3c", true);
//                    Log.d("Réseau", "réseaux : "+listeScan.size());
//                    Toast.makeText(context, "réseaux : "+listeScan.size(),
//                            Toast.LENGTH_SHORT);
                    // On vide notre liste
                    listeWifiItem.clear();

                    // Pour chaque scan
                    for (ScanResult scanResult : listeScan) {
                        WifiItem item = new WifiItem();
                        if (rechercher(scanResult.SSID) == -1) {
                            item.setAdresseMac(scanResult.BSSID);
                            item.setAPName(scanResult.SSID);
                            item.setForceSignal(scanResult.level);
//
//                        Log.d("FormationWifi", scanResult.SSID + " LEVEL "
//                                + scanResult.level);
//                            Log.d("Réseau", "WifiConfiguration " + scanResult.SSID+ "--" + scanResult.BSSID);
                            listeWifiItem.add(item);
                        }
                    }
                    // On rafraichit la liste
                    wifiAdapter.notifyDataSetChanged();
                    info.setText("Nombre de réseaux détectés : " + listeWifiItem.size());
                } else {
//                    Toast.makeText(context, "Vous devez activer votre WiFi",
//                            Toast.LENGTH_SHORT);
                    info.setText("Vous devez activer votre WiFi");
                }
            }

        }

    }

    public int rechercher(String item) {
        int resultat = -1;
        for (int i = 0; i < listeWifiItem.size(); i++) {
            if(listeWifiItem.get(i).getAPName().equals(item)) return i;
        }
        return resultat;
    }

    public void setWifiProxySettings5(String proxy, int port)
    {

        new ShowToast(getBaseContext(), "setWifiProxySettings5 : Proxy : "+proxy+" Port : "+port);
        //get the current wifi configuration
        WifiConfiguration config = GetCurrentWifiConfiguration(wifiManager);
//        new ShowToast(getBaseContext(), "setWifiProxySettings5 id "+wifiManager.getConnectionInfo().getNetworkId());
        if(null == config){
            new ShowToast(getBaseContext(), "setWifiProxySettings5 null");
            return;
        }

        try
        {
            //linkProperties is no longer in WifiConfiguration
            Class proxyInfoClass = Class.forName("android.net.ProxyInfo");
            Class[] setHttpProxyParams = new Class[1];
            setHttpProxyParams[0] = proxyInfoClass;
            Class wifiConfigClass = Class.forName("android.net.wifi.WifiConfiguration");
            Method setHttpProxy = wifiConfigClass.getDeclaredMethod("setHttpProxy", setHttpProxyParams);
            setHttpProxy.setAccessible(true);

            //Method 1 to get the ENUM ProxySettings in IpConfiguration
            Class ipConfigClass = Class.forName("android.net.IpConfiguration");
            Field f = ipConfigClass.getField("proxySettings");
            Class proxySettingsClass = f.getType();

            //Method 2 to get the ENUM ProxySettings in IpConfiguration
            //Note the $ between the class and ENUM
            //Class proxySettingsClass = Class.forName("android.net.IpConfiguration$ProxySettings");

            Class[] setProxySettingsParams = new Class[1];
            setProxySettingsParams[0] = proxySettingsClass;
            Method setProxySettings = wifiConfigClass.getDeclaredMethod("setProxySettings", setProxySettingsParams);
            setProxySettings.setAccessible(true);
            ProxyInfo pi = ProxyInfo.buildDirectProxy(proxy, port);
            //Android 5 supports a PAC file
            //ENUM value is "PAC"
            //ProxyInfo pacInfo = ProxyInfo.buildPacProxy(Uri.parse("http://localhost/pac"));

            //pass the new object to setHttpProxy
            Object[] params_SetHttpProxy = new Object[1];
            params_SetHttpProxy[0] = pi;
            setHttpProxy.invoke(config, params_SetHttpProxy);

            //pass the enum to setProxySettings
            Object[] params_setProxySettings = new Object[1];
//            params_setProxySettings[0] = Enum.valueOf((Class<Enum>) proxySettingsClass, "STATIC");
            params_setProxySettings[0] = Enum.valueOf((Class<Enum>) proxySettingsClass, "STATIC");
            setProxySettings.invoke(config, params_setProxySettings);
            //save the settings
            wifiManager.updateNetwork(config);
            Log.d("Réseau", "setWifiProxySettings5 : "+config.toString());
            wifiManager.disconnect();
            wifiManager.reconnect();
        }
        catch(Exception e)
        {
            Log.v("wifiProxy", e.toString());
        }
    }


    WifiConfiguration GetCurrentWifiConfiguration(WifiManager manager) {
        if (!manager.isWifiEnabled())
            return null;
        List<WifiConfiguration> configurationList = manager.getConfiguredNetworks();
        WifiConfiguration configuration = null;
        int cur = manager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i) {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.networkId == cur)
                return wifiConfiguration;
        }
        return configuration;
    }


//    public static Object getField(Object obj, String name)
//            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        Field f = obj.getClass().getField(name);
//        Object out = f.get(obj);
//        return out;
//    }
//
//    public static void setEnumField(Object obj, String value, String name)
//            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        Field f = obj.getClass().getField(name);
//        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
//    }
//
//    public static void setProxySettings(String assign , WifiConfiguration wifiConf)
//            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
//        setEnumField(wifiConf, assign, "proxySettings");
//    }
//
//    void setWifiProxySettings(String prox, String por) {
//        // get the current wifi configuration
//        // WifiManager manager = (WifiManager)
//        // getSystemService(Context.WIFI_SERVICE);
//        WifiConfiguration config = GetCurrentWifiConfiguration(wifiManager);
//        if (null == config)
//            return;
//
//        try {
//            Log.d("Réseau", "setWifiProxySettings : "+config.toString());
//            // get the link properties from the wifi configuration
//            Object linkProperties = getField(config, "linkProperties");
//            if (null == linkProperties)
//                return;
//
//            // get the setHttpProxy method for LinkProperties
//            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
//            Class[] setHttpProxyParams = new Class[1];
//            setHttpProxyParams[0] = proxyPropertiesClass;
//            Class lpClass = Class.forName("android.net.LinkProperties");
//            Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy",setHttpProxyParams);
//            setHttpProxy.setAccessible(true);
//
//            // get ProxyProperties constructor
//            Class[] proxyPropertiesCtorParamTypes = new Class[3];
//            proxyPropertiesCtorParamTypes[0] = String.class;
//            proxyPropertiesCtorParamTypes[1] = int.class;
//            proxyPropertiesCtorParamTypes[2] = String.class;
//
//            Constructor proxyPropertiesCtor = proxyPropertiesClass.getConstructor(proxyPropertiesCtorParamTypes);
//
//            // create the parameters for the constructor
//            Object[] proxyPropertiesCtorParams = new Object[3];
//            proxyPropertiesCtorParams[0] = prox;
//            proxyPropertiesCtorParams[1] = Integer.parseInt(por,10);
//            proxyPropertiesCtorParams[2] = "localhost";
//            proxyPropertiesCtorParams[2] = null;
//
//            // create a new object using the params
//            Object proxySettings = proxyPropertiesCtor.newInstance(proxyPropertiesCtorParams);
//
//            // pass the new object to setHttpProxy
//            Object[] params = new Object[1];
//            params[0] = proxySettings;
//            setHttpProxy.invoke(linkProperties, params);
//
//            setProxySettings("STATIC", config);
//
//            // // save the settings
//            // manager.updateNetwork(config);
//            // manager.disconnect();
//            // manager.reconnect();
//        } catch (Exception e) {
//        }
//    }
//
//    void unsetWifiProxySettings() {
////        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        Log.d("Réseau", "unsetWifiProxySettings : ");
//        WifiConfiguration config = GetCurrentWifiConfiguration(wifiManager);
//        if (null == config)
//            return;
//
//        try {
//            // get the link properties from the wifi configuration
//            Object linkProperties = getField(config, "linkProperties");
//            if (null == linkProperties)
//                return;
//
//            // get the setHttpProxy method for LinkProperties
//            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
//            Class[] setHttpProxyParams = new Class[1];
//            setHttpProxyParams[0] = proxyPropertiesClass;
//            Class lpClass = Class.forName("android.net.LinkProperties");
//            Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy",setHttpProxyParams);
//            setHttpProxy.setAccessible(true);
//
//            // pass null as the proxy
//            Object[] params = new Object[1];
//            params[0] = null;
//            setHttpProxy.invoke(linkProperties, params);
//            setProxySettings("NONE", config);
//            // save the config
//            wifiManager.updateNetwork(config);
//            Log.d("Réseau", "unsetWifiProxySettings : "+config.toString());
//            wifiManager.disconnect();
//            wifiManager.reconnect();
//        } catch (Exception e) {
//        }
//    }
}
