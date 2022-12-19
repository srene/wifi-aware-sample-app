package network.datahop.wifiawaresample;


import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;


public class Publication {

    interface Published {
        void messageReceived(String message);
    }

    private WifiAwareSession wifiAwareSession;
    private PublishDiscoverySession publishDiscoverySession;
    private Published pubs;
    public Publication(Published pubs){
        this.pubs = pubs;
    }

    public void publishService(WifiAwareSession wifiAwareSession) {

        this.wifiAwareSession = wifiAwareSession;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { return; }

        PublishConfig config = new PublishConfig.Builder()
                .setServiceName("network.datahop.wifiawaresample")
                .build();

        wifiAwareSession.publish(config, new DiscoverySessionCallback() {
            @Override
            public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                super.onPublishStarted(session);

                publishDiscoverySession = session;

                Log.d("publishService", "onPublishStarted");


            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                Log.d("publishService", "received message "+message.length);


            }
        }, null);
        //-------------------------------------------------------------------------------------------- -----
    }

    public void closeSession(){
        if (publishDiscoverySession != null) {
            publishDiscoverySession.close();
            publishDiscoverySession = null;
        }

    }
}
