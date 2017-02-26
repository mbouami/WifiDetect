package com.bouami.com.wifidetect;

import android.content.Context;
import android.net.ProxyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Mohammed on 13/02/2017.
 */

public class WifiProxyManager {

    private static int networkID = -1;
    private WifiManager mWifiManager;

    private static final String TAG = "WifiProxyManager";

    public WifiProxyManager(WifiManager manager) {
        mWifiManager = manager;
    }

    public void setWifiProxySettings(String proxy, int port)
    {
        //get the current wifi configuration
        WifiConfiguration config = GetCurrentWifiConfiguration(mWifiManager);
        if(null == config){
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
            params_setProxySettings[0] = Enum.valueOf((Class<Enum>) proxySettingsClass, "STATIC");
            setProxySettings.invoke(config, params_setProxySettings);

//            Class proxySettings = Class.forName("android.net.IpConfiguration$ProxySettings");
//
//            Class[] setProxyParams = new Class[2];
//            setProxyParams[0] = proxySettings;
//            setProxyParams[1] = ProxyInfo.class;
//
//            Method setProxy = config.getClass().getDeclaredMethod("setProxy", setProxyParams);
//            setProxy.setAccessible(true);
//
//            ProxyInfo desiredProxy = ProxyInfo.buildDirectProxy(proxy, port);
//
//            Object[] methodParams = new Object[2];
//            methodParams[0] = Enum.valueOf(proxySettings, "STATIC");
//            methodParams[1] = desiredProxy;
//
//            setProxy.invoke(config, methodParams);



            //save the settings
            mWifiManager.updateNetwork(config);
            mWifiManager.disconnect();
            mWifiManager.reconnect();
        }
        catch(Exception e)
        {
            Log.v("wifiProxy", e.toString());
        }
    }

    WifiConfiguration GetCurrentWifiConfiguration(WifiManager manager) {
        if (!manager.isWifiEnabled())
            return null;

        List<WifiConfiguration> configurationList = manager
                .getConfiguredNetworks();
        WifiConfiguration configuration = null;
        int cur = manager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i) {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.networkId == cur)
                configuration = wifiConfiguration;
        }

        return configuration;
    }

    public static Object getField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        Object out = f.get(obj);
        return out;
    }

    public static void setEnumField(Object obj, String value, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    public static void setProxySettings(String assign , WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
        setEnumField(wifiConf, assign, "proxySettings");
    }

    void unsetWifiProxySettings() {
        WifiConfiguration config = GetCurrentWifiConfiguration(mWifiManager);
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
            mWifiManager.updateNetwork(config);
            mWifiManager.disconnect();
            mWifiManager.reconnect();
        } catch (Exception e) {
        }
    }

//    public void setWifiProxySettings(Context context,String proxy, int port)
//    {
//        //get the current wifi configuration
//        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        WifiConfiguration config = GetCurrentWifiConfiguration(manager);
//        if(null == config)
//            return;
//
//        try
//        {
//            //linkProperties is no longer in WifiConfiguration
//            Class proxyInfoClass = Class.forName("android.net.ProxyInfo");
//            Class[] setHttpProxyParams = new Class[1];
//            setHttpProxyParams[0] = proxyInfoClass;
//            Class wifiConfigClass = Class.forName("android.net.wifi.WifiConfiguration");
//            Method setHttpProxy = wifiConfigClass.getDeclaredMethod("setHttpProxy", setHttpProxyParams);
//            setHttpProxy.setAccessible(true);
//
//            //Method 1 to get the ENUM ProxySettings in IpConfiguration
//            Class ipConfigClass = Class.forName("android.net.IpConfiguration");
//            Field f = ipConfigClass.getField("proxySettings");
//            Class proxySettingsClass = f.getType();
//
//            //Method 2 to get the ENUM ProxySettings in IpConfiguration
//            //Note the $ between the class and ENUM
//            //Class proxySettingsClass = Class.forName("android.net.IpConfiguration$ProxySettings");
//
//            Class[] setProxySettingsParams = new Class[1];
//            setProxySettingsParams[0] = proxySettingsClass;
//            Method setProxySettings = wifiConfigClass.getDeclaredMethod("setProxySettings", setProxySettingsParams);
//            setProxySettings.setAccessible(true);
//
//
//            ProxyInfo pi = ProxyInfo.buildDirectProxy(proxy, port);
//            //Android 5 supports a PAC file
//            //ENUM value is "PAC"
//            //ProxyInfo pacInfo = ProxyInfo.buildPacProxy(Uri.parse("http://localhost/pac"));
//
//            //pass the new object to setHttpProxy
//            Object[] params_SetHttpProxy = new Object[1];
//            params_SetHttpProxy[0] = pi;
//            setHttpProxy.invoke(config, params_SetHttpProxy);
//
//            //pass the enum to setProxySettings
//            Object[] params_setProxySettings = new Object[1];
//            params_setProxySettings[0] = Enum.valueOf((Class<Enum>) proxySettingsClass, "STATIC");
//            setProxySettings.invoke(config, params_setProxySettings);
//
//            //save the settings
//            manager.updateNetwork(config);
//            manager.disconnect();
//            manager.reconnect();
//        }
//        catch(Exception e)
//        {
//            Log.v("wifiProxy", e.toString());
//        }
//    }

//    public static boolean setWifiProxy(String proxy, int port, Context context) {
//
//        try {
//
//            Handler handler = new Handler(context.getMainLooper());
//
//            final WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//
//            if (!manager.isWifiEnabled()) return true;
//            List<WifiConfiguration> configurationList = manager.getConfiguredNetworks();
//            WifiConfiguration configuration = null;
//            int cur = manager.getConnectionInfo().getNetworkId();
//            for (WifiConfiguration wifiConfiguration : configurationList) {
//                if (wifiConfiguration.networkId == cur)
//                    configuration = wifiConfiguration;
//            }
//            if (configuration == null) return true;
//
//            WifiConfiguration config = new WifiConfiguration(configuration);
//            config.ipAssignment = WifiConfiguration.IpAssignment.UNASSIGNED;
//            config.proxySettings = WifiConfiguration.ProxySettings.STATIC;
//            config.linkProperties.clear();
//
//            config.linkProperties.setHttpProxy(new ProxyProperties("127.0.0.1", port, ""));
//            manager.updateNetwork(config);
//
//            manager.setWifiEnabled(false);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    manager.setWifiEnabled(true);
//                }
//            }, 1000);
//
//            networkID = cur;
//        } catch (Exception ignored) {
//            // Ignore all private API exception
//            Log.d(TAG, "Non support API", ignored);
//            return false;
//        }
//        return true;
//    }
//
//    public static void clearWifiProxy(Context context) {
//        if (networkID == -1) return;
//        try {
//
//            Handler handler = new Handler(context.getMainLooper());
//
//            final WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//            List<WifiConfiguration> configurationList = manager.getConfiguredNetworks();
//            WifiConfiguration configuration = null;
//            for (WifiConfiguration wifiConfiguration : configurationList) {
//                if (wifiConfiguration.networkId == networkID)
//                    configuration = wifiConfiguration;
//            }
//            if (configuration == null) return;
//
//            WifiConfiguration config = new WifiConfiguration(configuration);
//            config.ipAssignment = WifiConfiguration.IpAssignment.UNASSIGNED;
//            config.proxySettings = WifiConfiguration.ProxySettings.NONE;
//            config.linkProperties.clear();
//
//            manager.updateNetwork(config);
//
//            manager.setWifiEnabled(false);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    manager.setWifiEnabled(true);
//                }
//            }, 1000);
//
//            networkID = -1;
//        } catch (Exception ignored) {
//            // Ignore all private API exception
//            Log.d(TAG, "Non support API", ignored);
//        }
//    }
}
