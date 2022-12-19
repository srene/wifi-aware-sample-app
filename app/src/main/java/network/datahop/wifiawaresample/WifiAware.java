package network.datahop.wifiawaresample;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.util.Log;

public class WifiAware {

    private static final String TAG="WifiAware";
    private BroadcastReceiver broadcastReceiver;
    private WifiAwareManager wifiAwareManager;
    private ConnectivityManager connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private NetworkSpecifier networkSpecifier;
    private Context context;

    public WifiAware(Context context){
        wifiAwareManager = null;
        wifiAwareSession = null;
        networkSpecifier = null;
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.context = context;
    }

    public boolean startManager(){
        PackageManager packageManager = context.getPackageManager();
        boolean hasNan  = false;

        if (packageManager == null) {
            return false;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasNan = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
            }
        }

        if (!hasNan) {
            return false;
        } else {

            wifiAwareManager = (WifiAwareManager)context.getSystemService(Context.WIFI_AWARE_SERVICE);

            if (wifiAwareManager == null) {
                return false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Entering OnResume is executed");
            IntentFilter filter   = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
            broadcastReceiver     = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    wifiAwareManager.getCharacteristics();
                    boolean nanAvailable = wifiAwareManager.isAvailable();
                    Log.d(TAG, "NAN is available");
                    if (nanAvailable) {
                        attachToNanSession();
                        Log.d(TAG, "NAN attached");
                    } else {
                        Log.d(TAG, "NAN unavailable");
                        //return false;
                    }

                }
            };

            context.registerReceiver(broadcastReceiver, filter);

            boolean nanAvailable = wifiAwareManager.isAvailable();
            if (nanAvailable) {
                attachToNanSession();
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Handles attaching to NAN session.
     *
     */
    @TargetApi(26)
    private void attachToNanSession() {
        Log.d(TAG,"attachToNanSession");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Only once
        if (wifiAwareSession != null) {
            return;
        }

        if (wifiAwareManager == null || !wifiAwareManager.isAvailable()) {
            //setStatus("NAN is Unavailable in attach");
            return;
        }

        Log.d(TAG,"attaching...");

        wifiAwareManager.attach(new AttachCallback() {
            @Override
            public void onAttached(WifiAwareSession session) {
                super.onAttached(session);
                Log.d(TAG,"onAttached");

                closeSession();
                wifiAwareSession = session;
                //setHaveSession(true);
                Publication pub = new Publication(wifiAwareSession);
                pub.publishService();
                Subscription sub = new Subscription(wifiAwareSession);
                sub.subscribeToService();
            }

            @Override
            public void onAttachFailed() {
                super.onAttachFailed();
                //setHaveSession(false);
                //setStatus("attach() failed.");
                Log.d(TAG,"attach() failed");
            }

        }, new IdentityChangedListener() {
            @Override
            public void onIdentityChanged(byte[] mac) {
                super.onIdentityChanged(mac);
                //setMacAddress(mac);
            }
        }, null);
    }


    public void startDiscovery(){

    }

    public void startNetwork(){

    }

    public void connect(){

    }

    private void closeSession() {

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
    }
}
