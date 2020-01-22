package com.faust93.proxymator2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by faust on 05.02.14.
 */
public class NetworkChangeReceiver extends BroadcastReceiver implements Constants {
    private static boolean firstConnect = true;

    @Override
    public void onReceive(Context context, Intent intent) {

        MainFragment.stub_init(context);

        if(!MainFragment.getPreferences().getBoolean("globalDisable",false)){

         final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ) {

          if(activeNetwork.isConnected() && firstConnect){
                firstConnect = false;
                String network_name = "";

                // get wifi ssid by wifimanager coz on some devices extra is always NULL! WTF!?
                if(activeNetwork.getExtraInfo() != null){
                    network_name = activeNetwork.getExtraInfo().replaceAll("\"","");
                } else {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    network_name = wifiInfo.getSSID().replaceAll("\"","");
                }

              int profile = MainFragment.matchProfile(context, network_name, true);

                if(MainFragment.getLogging()){
                  Log.d(TAG,network_name);
                  Log.d(TAG,"r_state: " + MainFragment.get_rstate());
                  Log.d(TAG,"profile: " + profile);
                }

                if( profile != NOT_FOUND && !MainFragment.get_rstate()){
                    MainFragment.iptablesSave();
                    if(MainFragment.start_proxy_nr(context,profile)){
                        SharedPreferences.Editor editor = MainFragment.getPreferences().edit();
                        editor.putInt("activeProfile", profile);
                        editor.commit();
                    }
                }
           // if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
           //     Log.d("Proxymator2",activeNetwork.toString());
            }
        } else
            // NOT_CONNECTED;
            firstConnect = true;
        }
    }
}
