package com.bouami.com.wifidetect;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Mohammed on 13/02/2017.
 */

public class ConnectionManager {
    private Context context;
    private Activity activity;
    private static final String WPA = "WPA";
    private static final String WEP = "WEP";
    private static final String OPEN = "Open";
    private final static String TAG = "WiFiConnector";
    private WifiManager mwifiManager;
    private WifiConfiguration mwifiConfiguration;
    private int connexionencours;
    private int ALREADY_CONNECTED = 0;
    private int SSID_NOT_FOUND = -1;
    private int UNABLE_TO_FIND_SECURITY_TYPE = -2;
    private int CONNECTION_REQUESTED = -3;


    public ConnectionManager(Context context,WifiManager wifiManager) {
        this.context = context;
        this.activity = (Activity) context;
        mwifiManager = wifiManager;
    }

    public void enableWifi() {
        if (!mwifiManager.isWifiEnabled()) {
            mwifiManager.setWifiEnabled(true);
            new ShowToast(context, "Wifi Activé");
        }
    }

    public WifiManager returnWifiManager() {
        return mwifiManager;
    }
    public int requestWIFIConnection(String networkSSID, String networkPass, int priorite,String proxy, int port) {
        int connection = -1;
        try {
            //Check ssid exists
            if (scanWifi(mwifiManager, networkSSID)) {
                if (getCurrentSSID(mwifiManager) != null && getCurrentSSID(mwifiManager).equals("\"" + networkSSID + "\"")) {
                    new ShowToast(context, "Connexion déjà établie avec  " + networkSSID);
                    return ALREADY_CONNECTED;
                }
                //Security type detection
                String SECURE_TYPE = checkSecurity(mwifiManager, networkSSID);
                if (SECURE_TYPE == null) {
                    new ShowToast(context, "Impossible de trouver le type de connexion pour " + networkSSID);
                    return UNABLE_TO_FIND_SECURITY_TYPE;
                }else {
                    new ShowToast(context, "type de connexion pour " + networkSSID+"-"+SECURE_TYPE);
                }
                if (connexionencours>0) mwifiManager.removeNetwork(connexionencours);
                if (SECURE_TYPE.equals(WPA)) {
                    connection = WPA(mwifiManager,networkSSID, networkPass,priorite,proxy, port);
                } else if (SECURE_TYPE.equals(WEP)) {
                    connection = WEP(mwifiManager,networkSSID, networkPass,priorite,proxy, port);
                } else {
                    connection = OPEN(mwifiManager, networkSSID,priorite,proxy, port);
                }
                connexionencours = connection;
                mwifiManager.enableNetwork(connection, true);
                mwifiManager.disconnect();
                mwifiManager.reconnect();
                return CONNECTION_REQUESTED;

            }
            /*connectME();*/
        } catch (Exception e) {
            new ShowToast(context, "Erreur de connexion WIFI " + e);
        }
        return SSID_NOT_FOUND;
    }

    private int WPA(WifiManager wifiManager,String networkSSID, String networkPass, int priorite,String proxy, int port) {
        boolean b = false;
        int id = -1;
//        wifiManager.disconnect();
        new ShowToast(context, "WPA proxy et port " +proxy.length()+"---"+port );
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + networkSSID + "\"";
        wc.preSharedKey = "\"" + networkPass + "\"";
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.priority = priorite;
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        if (proxy.length()>0) {
            setWifiProxySettings5(wc,proxy,port);
        } else {
            unsetWifiProxySettings(wc);
        }
        id = wifiManager.addNetwork(wc);
        wifiManager.enableNetwork(id, true);
        wifiManager.updateNetwork(wc);
//        wifiManager.disconnect();
//        wifiManager.reconnect();
        return id;
    }

    private int WEP(WifiManager wifiManager,String networkSSID, String networkPass, int priorite,String proxy, int port) {
        int id = -1;
        return id;
    }

    private int OPEN(WifiManager wifiManager, String networkSSID, int priorite,String proxy, int port) {
        boolean b = false;
        int id = -1;
//        wifiManager.disconnect();
        new ShowToast(context, "OPEN proxy et port " +proxy.length()+"---"+port );
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + networkSSID + "\"";
//        wc.hiddenSSID = true;
//        wc.priority = 0xBADBAD;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.priority = priorite;
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        if (proxy.length()>0) {
            setWifiProxySettings5(wc,proxy,port);
        } else {
            unsetWifiProxySettings(wc);
        }
        id = wifiManager.addNetwork(wc);
        wifiManager.enableNetwork(id, true);
        wifiManager.updateNetwork(wc);
//        wifiManager.disconnect();
//        wifiManager.reconnect();
        return id;
    }

    boolean scanWifi(WifiManager wifiManager, String networkSSID) {
//        Log.e(TAG, "scanWifi starts");
        List<ScanResult> scanList = wifiManager.getScanResults();
        for (ScanResult i : scanList) {
            if (i.SSID != null && i.SSID.equals(networkSSID)) {
//                new ShowToast(context, "SSID Trouvé: " + i.SSID);
                return true;
            }
        }
        new ShowToast(context, "SSID " + networkSSID + " Non trouvé");
        return false;
    }

    public String getCurrentSSID(WifiManager wifiManager) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo.isConnected()) {
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }

    public String getCurrentBSSID(WifiManager wifiManager) {
        String bssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo.isConnected()) {
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                bssid = connectionInfo.getSSID();
            }
        }
        return bssid;
    }

    private String checkSecurity(WifiManager wifiManager, String ssid) {
        List<ScanResult> networkList = wifiManager.getScanResults();
        for (ScanResult network : networkList) {
            if (network.SSID.equals(ssid)) {
                String Capabilities = network.capabilities;
                if (Capabilities.contains("WPA")) {
                    return WPA;
                } else if (Capabilities.contains("WEP")) {
                    return WEP;
                } else {
                    return OPEN;
                }

            }
        }
        return null;
    }
    public WifiConfiguration GetWifiConfigurationParSSID(String ssid) {
        if (!mwifiManager.isWifiEnabled())
            return null;
        List<WifiConfiguration> configurationList = mwifiManager.getConfiguredNetworks();
        new ShowToast(context, "GetWifiConfigurationParSSID " + configurationList.size());
        WifiConfiguration configuration = null;
        int cur = mwifiManager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i) {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.SSID == ssid) {
                configuration = wifiConfiguration;
                break;
            }
        }
        return configuration;
    }

    public WifiConfiguration GetWifiConfigurationParBSSID(String bssid) {
        if (!mwifiManager.isWifiEnabled())
            return null;

        List<WifiConfiguration> configurationList = mwifiManager.getConfiguredNetworks();
        WifiConfiguration configuration = null;
        int cur = mwifiManager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i) {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.BSSID == bssid) {
                configuration = wifiConfiguration;
                break;
            }
        }
        return configuration;
    }

    public WifiConfiguration GetCurrentWifiConfiguration() {
        if (!mwifiManager.isWifiEnabled())
            return null;
        List<WifiConfiguration> configurationList = mwifiManager.getConfiguredNetworks();
        WifiConfiguration configuration = null;
        int cur = mwifiManager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i) {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.networkId == cur)
                return wifiConfiguration;
        }
        return configuration;
    }

    private void setWifiProxySettings5(WifiConfiguration config,String proxy, int port)
    {

        new ShowToast(context, "setWifiProxySettings5 : Proxy : "+proxy+" Port : "+port);
        //get the current wifi configuration
//        new ShowToast(getBaseContext(), "setWifiProxySettings5 id "+wifiManager.getConnectionInfo().getNetworkId());
        if(null == config){
            new ShowToast(context, "setWifiProxySettings5 null");
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
            mwifiManager.updateNetwork(config);
            Log.d("Réseau", "setWifiProxySettings5 : "+config.toString());
//            mwifiManager.disconnect();
//            mwifiManager.reconnect();
        }
        catch(Exception e)
        {
            Log.v("wifiProxy", e.toString());
        }
    }


    private static Object getField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }

    private static void setEnumField(Object obj, String value, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    private static void setProxySettings(String assign , WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
        setEnumField(wifiConf, assign, "proxySettings");
    }


    private void unsetWifiProxySettings(WifiConfiguration config) {
//        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Log.d("Réseau", "unsetWifiProxySettings : ");
        if (null == config)
            return;

        try {
            // get the link properties from the wifi configuration
            Object linkProperties = getField(config, "linkProperties");
            if (null == linkProperties)
                return;

            // get the setHttpProxy method for LinkProperties
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            Class[] setHttpProxyParams = new Class[1];
            setHttpProxyParams[0] = proxyPropertiesClass;
            Class lpClass = Class.forName("android.net.LinkProperties");
            Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy",setHttpProxyParams);
            setHttpProxy.setAccessible(true);

            // pass null as the proxy
            Object[] params = new Object[1];
            params[0] = null;
            setHttpProxy.invoke(linkProperties, params);
            setProxySettings("NONE", config);
            // save the config
            mwifiManager.updateNetwork(config);
            Log.d("Réseau", "unsetWifiProxySettings : "+config.toString());
//            mwifiManager.disconnect();
//            mwifiManager.reconnect();
        } catch (Exception e) {
        }
    }

    private class ShowToast {
        public ShowToast(Context context, String s) {
            Toast.makeText(context,s,Toast.LENGTH_LONG).show();
        }
    }
}
