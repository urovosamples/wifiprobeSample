package com.udroid.device.wifiprobe;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

/**
 * Created by rocky on 17-11-6.
 * A BroadcastReceiver that notifies of important wifi p2p events.
 * WIFI_P2P_STATE_CHANGED_ACTION

 　　指示　Wi-Fi P2P　是否开启

 WIFI_P2P_PEERS_CHANGED_ACTION

 　　代表对等节点（peer）列表发生了变化

 WIFI_P2P_CONNECTION_CHANGED_ACTION

 　　表明Wi-Fi P2P的连接状态发生了改变

 WIFI_P2P_THIS_DEVICE_CHANGED_ACTION

 　　指示设备的详细配置发生了变化
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private Channel channel;
    private WiFiProbeServiceImpl activity;
    public interface MyPeerListListener {
        void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList);
    }
    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       WiFiProbeServiceImpl activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            //指示　Wi-Fi P2P　是否开启
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                //activity.resetData();

            }
            Log.d(WiFiProbeServiceImpl.TAG, "P2P state changed - " + state);
        } else if (/*WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action) || */action.equals("android.net.wifi.p2p.RSPDEV_CHANGED")) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            /*if (manager != null) {
                manager.requestPeers(channel, (PeerListListener) activity);
            }*/
            WifiP2pDeviceList mWifiP2pDeviceList = (WifiP2pDeviceList)intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
            if(mWifiP2pDeviceList != null) {
                activity.onPeersAvailable(mWifiP2pDeviceList);
            }
            //Log.d(WiFiProbeServiceImpl.TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                //manager.requestConnectionInfo(channel, fragment);//ConnectionInfoListener
            } else {
                // It's a disconnect
                //activity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            activity.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
            Log.d(activity.TAG, action);
        }
    }
    private void updateWifiState(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLING:
                activity.setIsWifiEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                activity.setIsWifiEnabled(true);
                break;
            default:
        }
    }
}
