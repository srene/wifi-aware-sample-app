package network.datahop.wifiawaresample;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int          MY_PERMISSION_FINE_LOCATION_REQUEST_CODE = 88;
    private static final int          MY_PERMISSION_NEARBY_DEVICES_REQUEST_CODE = 88;
    private final int                 MAC_ADDRESS_MESSAGE             = 55;
    private final int                 IP_ADDRESS_MESSAGE             = 33;

    private static final String TAG = "WifiAwareSample";
    private BroadcastReceiver broadcastReceiver;
    private WifiAwareManager          wifiAwareManager;
    private ConnectivityManager       connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private NetworkSpecifier networkSpecifier;
    private PublishDiscoverySession publishDiscoverySession;
    private SubscribeDiscoverySession subscribeDiscoverySession;
    private PeerHandle peerHandle;
    private byte[]                    myMac;
    private String                    EncryptType;
    private byte[]                    portOnSystem;
    private int                       portToUse;

    private byte[]                    myIP;
    private byte[]                    otherIP;
    private Inet6Address ipv6;
    private ServerSocket serverSocket;
    private Inet6Address              peerIpv6;
    private int                       peerPort;

    private boolean networkBuilt,sent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupPermissions();

        wifiAwareManager = null;
        wifiAwareSession = null;
        networkSpecifier = null;
        publishDiscoverySession = null;
        subscribeDiscoverySession = null;
        peerHandle = null;
        networkBuilt = false;
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        String  status  = null;
        boolean hasNan  = false;

        EncryptType = "open";

        sent = false;
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            status = "Cannot get PackageManager";
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasNan = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
            }
        }

        if (!hasNan) {
            status = "Device does not have NAN";
        } else {

            wifiAwareManager = (WifiAwareManager)getSystemService(Context.WIFI_AWARE_SERVICE);

            if (wifiAwareManager == null) {
                status = "Cannot get WifiAwareManager";
            }
        }

    }

    /**
     * App Permissions for Fine Location
     **/
    private void setupPermissions() {
        // If we don't have the record network permission...
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // And if we're on SDK M or later...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ask again, nicely, for the permissions.
                String[] permissionsWeNeed = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
                requestPermissions(permissionsWeNeed, MY_PERMISSION_FINE_LOCATION_REQUEST_CODE);
            }
        }*/

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            // And if we're on SDK M or later...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ask again, nicely, for the permissions.
                String[] permissionsWeNeed = new String[]{ Manifest.permission.NEARBY_WIFI_DEVICES };
                requestPermissions(permissionsWeNeed, MY_PERMISSION_NEARBY_DEVICES_REQUEST_CODE);
            }
        }


        //-------------------------------------------------------------------------------------------- -----
    }

    /**
     * Resuming activity
     *
     */
    @Override
    @TargetApi(26)
    protected void onResume() {
        super.onResume();

        String  status = null;
        Log.d(TAG, "Current phone build" + Build.VERSION.SDK_INT +"\tMinimum:"+ Build.VERSION_CODES.O);
        Log.d(TAG,"Supported Aware: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Entering OnResume is executed");
            IntentFilter filter   = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
            broadcastReceiver     = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String status = "";
                    wifiAwareManager.getCharacteristics();
                    boolean nanAvailable = wifiAwareManager.isAvailable();
                    Log.d(TAG, "NAN is available");
                    if (nanAvailable) {
                        attachToNanSession();
                        status = "NAN has become Available";
                        Log.d(TAG, "NAN attached");
                    } else {
                        status = "NAN has become Unavailable";
                        Log.d(TAG, "NAN unavailable");
                    }

                    setStatus(status);
                }
            };

            getApplicationContext().registerReceiver(broadcastReceiver, filter);

            boolean nanAvailable = wifiAwareManager.isAvailable();
            if (nanAvailable) {
                attachToNanSession();
                status = "NAN is Available";
            } else {
                status = "NAN is Unavailable";
            }
        } else {
            status = "NAN is only supported in O+";
        }

        setStatus(status);

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
            setStatus("NAN is Unavailable in attach");
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
                publishService();
                subscribeToService();
            }

            @Override
            public void onAttachFailed() {
                super.onAttachFailed();
                //setHaveSession(false);
                setStatus("attach() failed.");
            }

        }, new IdentityChangedListener() {
            @Override
            public void onIdentityChanged(byte[] mac) {
                super.onIdentityChanged(mac);
                setMacAddress(mac);
            }
        }, null);
    }

    private void setMacAddress(byte[] mac) {
        myMac = mac;
    }

    /**
     * Helper to set the status field.
     *
     * @param status
     */
    private void setStatus(String status) {
        TextView textView = (TextView)findViewById(R.id.status);
        textView.setText(status);
    }



    private void closeSession() {

        if (publishDiscoverySession != null) {
            publishDiscoverySession.close();
            publishDiscoverySession = null;
        }

        if (subscribeDiscoverySession != null) {
            subscribeDiscoverySession.close();
            subscribeDiscoverySession = null;
        }

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
    }

    @TargetApi(26)
    private void publishService() {

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
                if(!networkBuilt) {
                    startServer(0,3);

                    NetworkSpecifier networkSpecifier = new WifiAwareNetworkSpecifier.Builder(publishDiscoverySession, peerHandle)
                            .build();
                    NetworkRequest myNetworkRequest = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                            .setNetworkSpecifier(networkSpecifier)
                            .build();

                    Log.d("publishService", "building network");

                    ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            Log.d("publishService", "onAvailable");

                        }

                        @Override
                        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                            Log.d("publishService", "onCapabilitiesChanged");

                        }

                        @Override
                        public void onLost(Network network) {
                            Log.d("publishService", "onLost");

                        }

                        //-------------------------------------------------------------------------------------------- +++++
                        @Override
                        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                            super.onLinkPropertiesChanged(network, linkProperties);
                            //TODO: create socketServer on different thread to transfer files
                            try {

                                NetworkInterface awareNi = NetworkInterface.getByName(
                                        linkProperties.getInterfaceName());

                                Enumeration<InetAddress> Addresses = awareNi.getInetAddresses();
                                while (Addresses.hasMoreElements()) {
                                    InetAddress addr = Addresses.nextElement();
                                    if (addr instanceof Inet6Address) {
                                        Log.d("myTag", "netinterface ipv6 address: " + addr.toString());
                                        if (((Inet6Address) addr).isLinkLocalAddress()) {

                                            ipv6 = Inet6Address.getByAddress("WifiAware",addr.getAddress(),awareNi);
                                            myIP = addr.getAddress();
                                            Log.d("myTag","sending top "+new String(myIP));
                                            if (publishDiscoverySession != null && peerHandle != null) {
                                                Log.d("myTag","sending to subs");
                                                publishDiscoverySession.sendMessage(peerHandle, IP_ADDRESS_MESSAGE, myIP);
                                            } else if(subscribeDiscoverySession != null && peerHandle != null){
                                                Log.d("myTag","sending to pub");
                                                subscribeDiscoverySession.sendMessage(peerHandle,IP_ADDRESS_MESSAGE, myIP);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            catch (SocketException e) {
                                Log.d("myTag", "socket exception " + e.toString());
                            }
                            catch (Exception e) {
                                //EXCEPTION!!! java.lang.NullPointerException: Attempt to invoke virtual method 'java.util.Enumeration java.net.NetworkInterface.getInetAddresses()' on a null object reference
                                Log.d("myTag", "EXCEPTION!!! " + e.toString());
                            }
                            Log.d("myTag", "entering linkPropertiesChanged "+peerIpv6+" "+peerPort+" "+otherIP);


                        }
                    };

                    connectivityManager.requestNetwork(myNetworkRequest, callback);

                    Log.d("publishService", "sending message");

                    publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, portToBytes(serverSocket.getLocalPort()));

                    networkBuilt = true;
                }
            }
        }, null);
        //-------------------------------------------------------------------------------------------- -----
    }

    //-------------------------------------------------------------------------------------------- +++++
    @TargetApi(26)
    private void subscribeToService() {

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
            public void onServiceDiscovered(PeerHandle peerHandle_, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);

                peerHandle = peerHandle_;

                Log.d("subscribeToService", "onServiceDiscovered");
                if(!networkBuilt)subscribeDiscoverySession.sendMessage(peerHandle,MAC_ADDRESS_MESSAGE,myMac);
            }


            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                Log.d("subscribeToService", "received message "+message.length);

                if(message.length == 2) {
                    portToUse = byteToPortInt(message);
                } else if (message.length == 16) {
                    otherIP = message;
                }
                if(!networkBuilt) {
                    NetworkSpecifier networkSpecifier = new WifiAwareNetworkSpecifier.Builder(subscribeDiscoverySession, peerHandle)
                            .build();
                    NetworkRequest myNetworkRequest = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                            .setNetworkSpecifier(networkSpecifier)
                            .build();

                    Log.d("subscribeToService", "building network");

                    ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            Log.d("subscribeToService", "onAvailable");

                        }

                        @Override
                        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                            WifiAwareNetworkInfo peerAwareInfo = (WifiAwareNetworkInfo) networkCapabilities.getTransportInfo();
                            peerIpv6 = peerAwareInfo.getPeerIpv6Addr();
                            peerPort = peerAwareInfo.getPort();
                            Log.d("subscribeToService", "onCapabilitiesChanged "+peerIpv6+" "+peerPort+" "+otherIP);
                            try {
                                if(!sent){
                                    Log.d("subscribeToService","sending file");
                                    clientSendFile(Inet6Address.getByAddress("WifiAwareHost",otherIP, peerIpv6.getScopedInterface()), portToUse);
                                    sent=true;
                                }
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }


                        }

                        //-------------------------------------------------------------------------------------------- +++++
                        @Override
                        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                            super.onLinkPropertiesChanged(network, linkProperties);
                            //TODO: create socketServer on different thread to transfer files
                            Toast.makeText(MainActivity.this, "onLinkPropertiesChanged", Toast.LENGTH_SHORT).show();

                            try {

                                NetworkInterface awareNi = NetworkInterface.getByName(
                                        linkProperties.getInterfaceName());

                                Enumeration<InetAddress> Addresses = awareNi.getInetAddresses();
                                while (Addresses.hasMoreElements()) {
                                    InetAddress addr = Addresses.nextElement();
                                    if (addr instanceof Inet6Address) {
                                        Log.d("myTag", "netinterface ipv6 address: " + addr.toString());
                                        if (((Inet6Address) addr).isLinkLocalAddress()) {
                                            ipv6 = Inet6Address.getByAddress("WifiAware",addr.getAddress(),awareNi);
                                            myIP = addr.getAddress();
                                            if (publishDiscoverySession != null && peerHandle != null) {
                                                publishDiscoverySession.sendMessage(peerHandle, IP_ADDRESS_MESSAGE, myIP);
                                            } else if(subscribeDiscoverySession != null && peerHandle != null){
                                                subscribeDiscoverySession.sendMessage(peerHandle,IP_ADDRESS_MESSAGE, myIP);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            catch (SocketException e) {
                                Log.d("myTag", "socket exception " + e.toString());
                            }
                            catch (Exception e) {
                                //EXCEPTION!!! java.lang.NullPointerException: Attempt to invoke virtual method 'java.util.Enumeration java.net.NetworkInterface.getInetAddresses()' on a null object reference
                                Log.d("myTag", "EXCEPTION!!! " + e.toString());
                            }
                            Log.d("myTag", "entering linkPropertiesChanged "+peerIpv6+" "+peerPort+" "+otherIP);

                        }

                        @Override
                        public void onLost(Network network) {
                            Log.d("subscribeToService", "onLost");

                        }
                    };
                    connectivityManager.requestNetwork(myNetworkRequest, callback);
                }
            }
        }, null);
    }

    public int byteToPortInt(byte[] bytes){
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }


    public byte[] portToBytes(int port){
        byte[] data = new byte [2];
        data[0] = (byte) (port & 0xFF);
        data[1] = (byte) ((port >> 8) & 0xFF);
        return data;
    }

    @TargetApi(26)
    public void startServer(final int port, final int backlog) {
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d("serverThread", "thread running");
                    Thread.sleep(1000);
                    serverSocket = new ServerSocket(port, backlog);
                    //ServerSocket serverSocket = new ServerSocket();
                    while (true) {
                        portToUse = serverSocket.getLocalPort();
                        if (EncryptType.equals("open")) {
                            portOnSystem = portToBytes(serverSocket.getLocalPort());
                        }

                        Log.d("serverThread", "server waiting to accept on " + serverSocket.toString());
                        Socket clientSocket = serverSocket.accept();
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                        DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                        byte[] buffer = new byte[4096];
                        int read;
                        int totalRead = 0;
                        ContentValues values = new ContentValues();

                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "nanFile");       //file name
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");        //file extension, will automatically add to file
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS );     //end "/" is not mandatory

                        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);      //important!

                        OutputStream fos = getContentResolver().openOutputStream(uri);
                        //FileOutputStream fos = new FileOutputStream("/sdcard/Download/newfile");
                        Log.d("serverThread", "Socket being written to begin... ");
                        while ((read = in.read(buffer)) > 0) {
                            fos.write(buffer,0,read);
                            totalRead += read;
                            if (totalRead%(4096*2500)==0) {//every 10MB update status
                                Log.d("clientThread", "total bytes retrieved:" + totalRead);
                            }
                        }
                        Log.d("serverThread", "finished file transfer: " + totalRead);

                    }
                } catch (IOException e) {
                    Log.d("serverThread", "socket exception " + e.toString());
                    Log.d("serverThread",  e.getStackTrace().toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();

    }

    public void clientSendFile(final Inet6Address serverIP,final int serverPort) {
        Runnable clientTask = new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[4096];
                int bytesRead;
                Socket clientSocket = null;
                int fsize = 1;
                InputStream is = null;
                OutputStream outs = null;
                Log.d("clientThread", "thread running socket info "+ serverIP.getHostAddress() + "\t" + serverPort);
                try {
                    clientSocket = new Socket( serverIP , serverPort );
                    is = clientSocket.getInputStream();
                    outs = clientSocket.getOutputStream();
                    Log.d("clientThread", "socket created ");
                } catch (IOException ex) {
                    Log.d("clientThread", "socket could not be created " + ex.toString());
                }
                try {
                    String message = "hola";
                    InputStream in = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
                    int count;
                    int totalSent = 0;
                    DataOutputStream dos = new DataOutputStream(outs);
                    Log.d("clientThread", "beginning to send file (log updates every 2MB)");
                    while ((count = in.read(buffer))>0){
                        totalSent += count;
                        dos.write(buffer, 0, count);
                    }
                    in.close();
                    dos.close();
                    Log.d("clientThread", "finished sending file!!! "+totalSent);
                    //setStatus("Finished sending file");

                } catch(IOException e){
                    Log.d("clientThread", e.toString());
                }

            }
        };
        Thread clientThread = new Thread(clientTask);
        clientThread.start();

    }




}