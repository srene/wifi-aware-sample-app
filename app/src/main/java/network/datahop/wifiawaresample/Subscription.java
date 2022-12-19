package network.datahop.wifiawaresample;

import android.annotation.TargetApi;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

public class Subscription {

    interface Subscribed {
        void messageReceived(String message);
    }

    SubscribeDiscoverySession subscribeDiscoverySession;
    WifiAwareSession wifiAwareSession;
    private final int                 MAC_ADDRESS_MESSAGE             = 55;
    private Subscribed subs;
    public Subscription(Subscribed subs){
        this.subs = subs;
    }
    //-------------------------------------------------------------------------------------------- +++++
    @TargetApi(26)
    public void subscribeToService(WifiAwareSession wifiAwareSession) {
        this.wifiAwareSession = wifiAwareSession;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { return; }

        SubscribeConfig config = new SubscribeConfig.Builder()
                .setServiceName("network.datahop.wifiawaresample")
                .build();

        wifiAwareSession.subscribe(config, new DiscoverySessionCallback() {

            @Override
            public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                super.onSubscribeStarted(session);

                subscribeDiscoverySession = session;
                Log.d("subscribeToService", "onSubscribeStarted");

            }

            @Override
            public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
                //peerHandle = peerHandle_;
                Log.d("subscribeToService", "onServiceDiscovered");
               // if(!networkBuilt)
                subscribeDiscoverySession.sendMessage(peerHandle,MAC_ADDRESS_MESSAGE,null);
            }


            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                //Log.d("subscribeToService", "received message "+message.length);
                subs.messageReceived("Message received "+message.length);
            }
        }, null);
    }

    public void closeSession(){
        if (subscribeDiscoverySession != null) {
            subscribeDiscoverySession.close();
            subscribeDiscoverySession = null;
        }
    }
}

