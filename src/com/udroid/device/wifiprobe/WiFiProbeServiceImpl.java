package com.udroid.device.wifiprobe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import com.udroid.device.wifiprobe.OnTaskStaListener;
import com.udroid.device.wifiprobe.OnSwitchListener;
import com.udroid.device.wifiprobe.WiFiProbeService;

import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rocky on 17-11-1.
 */

public class WiFiProbeServiceImpl implements WifiP2pManager.ChannelListener, WiFiDirectBroadcastReceiver.MyPeerListListener {
    public static final String TAG = "UPOS" +WiFiProbeServiceImpl.class.getSimpleName();
    private Context mContext;
    private WiFiProbeServiceStub mWiFiProbeServiceStub;
    private boolean isActivate;
    private WifiManager mWifiManager;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private static final long SCANWIFI_DELAY = 25 * 60 * 1000;
    //---- WiFiProbe Error -----
    public final static int WiFiProbe_Base_Error = -21000;
    /**开启探针成功*/
    public final static int WiFiProbe_Open_Succeed = WiFiProbe_Base_Error - 1;
    /**开启探针失败*/
    public final static int WiFiProbe_Open_Failed = WiFiProbe_Base_Error - 2;
    /**关闭探针成功*/
    public final static int WiFiProbe_Close_Succeed = WiFiProbe_Base_Error - 3;
    /**关闭探针失败*/
    public final static int WiFiProbe_Close_Failed = WiFiProbe_Base_Error - 4;
    /**停止接收数据成功*/
    public final static int WiFiProbe_Stop_Succeed = WiFiProbe_Base_Error - 5;
    /**停止接收数据失败*/
    public final static int WiFiProbe_Stop_Failed = WiFiProbe_Base_Error - 6;
    public WiFiProbeServiceImpl(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction("android.net.wifi.p2p.RSPDEV_CHANGED");

        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    }
    /**
     * 释放该设备所占用的所有资源
     */
    public int close() {
        Log.d(TAG, "device close");
        mWiFiProbeServiceStub = null;
        if(mInputListeners != null)
            mInputListeners.clear();
        mISwitchCallback = null;
        mOpenISwitchCallback = null;
        mCloseISwitchCallback = null;
        mPortalHandler.removeMessages(MESSAGE_PROBE_WORK);
        mPortalHandler.removeMessages(MESSAGE_PROBE_FORCE_RESPONSE);
        unregReceiver();
        if(wakeLock != null) {
            wakeLock.release();
        }
        wakeLock = null;
        powerManager = null;
        try{
            if(receiver != null) {
                mContext.unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    BroadcastReceiver mWifiReceiver;
    boolean mIsNetworkAvailiable;
    private void regReceiver() {
        IntentFilter mIntentfilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentfilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //mIsNetworkAvailiable = isNetworkAvailable();
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){

                } else if (action.equals(Intent.ACTION_SCREEN_ON)){

                } else if (action.equals(Intent.ACTION_SCREEN_OFF)){

                } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                    updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN));
                }

            }
        };
        mContext.registerReceiver(mWifiReceiver, mIntentfilter);
    }
    private void updateWifiState(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLING:
                //closeProbe();
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                Log.i(TAG, "WIFI_STATE_ENABLED");
                try {
                    openProbe();
                } catch (Exception e) {
                    e.printStackTrace();
                    ;
                }
                break;
            default:
        }
    }
    private void unregReceiver() {
        if (mWifiReceiver != null) {
            try {
                mContext.unregisterReceiver(mWifiReceiver);
            }catch(Exception e) {
                e.printStackTrace();
            }
            mWifiReceiver = null;
        }
    }
    /**
     * 释放该设备所占用的所有资源
     */
    public int getStatus() {
        return -1;
    }
    public static final int MESSAGE_PROBE_FORCE_RESPONSE = 1;
    public static final int MESSAGE_PROBE_WORK = 2;
    private static int peerCounts = 2;
    private Handler mPortalHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_PROBE_WORK: {
                    peerCounts = 2;
                    if(manager != null && channel != null) {
                        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "Discovery Initiated");
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                Log.i(TAG, "Discovery Failed : " );
                            }
                        });
                    }
                }
                    break;
                case MESSAGE_PROBE_FORCE_RESPONSE: {
                    Log.i(TAG, "MESSAGE_PROBE_FORCE_RESPONSE" );
                    mPortalHandler.sendEmptyMessageDelayed(MESSAGE_PROBE_FORCE_RESPONSE, SCANWIFI_DELAY);
                    if(mInputListeners != null && mInputListeners.size() > 0) {
                        long time = System.currentTimeMillis();
                        for(int i = 0; i < peers.size(); i++) {
                            WifiP2pDevice device = peers.get(i);
                            if (device != null) {
                                updateWifiProbecallBack(device.deviceAddress, "" + device.status, time);//TODO
                            }
                            device = null;
                        }
                    }
                    if(manager != null && channel != null) {
                        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "Discovery Initiated");
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                Log.i(TAG, "Discovery Failed : " );
                            }
                        });
                    }
                }
                    break;
            }
        }
    };
    public IBinder getDeviceBinder() {
        if(mWiFiProbeServiceStub == null) {
            mWiFiProbeServiceStub = new WiFiProbeServiceStub(this);
            regReceiver();
        }
        if(mInputListeners == null) {
            mInputListeners = new ArrayList<TaskStaListener>();
        }
        if(powerManager == null) {
            this.powerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
            this.wakeLock = this.powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Probe Lock");
        }
        return mWiFiProbeServiceStub;
    }
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
    public void setIsWifiEnabled(boolean isWifiEnabled) {
        try {
            if(isWifiEnabled) {
                openProbe();
            } else {
                closeProbe();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void updateThisDevice(WifiP2pDevice device) {
        if(device != null)
        Log.i(TAG, "updateThisDevice = " + device.deviceAddress + " name:" + device.deviceName);
    }
    public void switchWifiProbe(int state) {
        try {
            switch (state) {
                case 0:
                    mPortalHandler.removeMessages(MESSAGE_PROBE_WORK);
                    mPortalHandler.removeMessages(MESSAGE_PROBE_FORCE_RESPONSE);
                    Log.d(TAG, "switchWifiProbe = " + isWifiP2pEnabled);
                    /*if (!isWifiP2pEnabled) {
                        if (mCloseISwitchCallback != null)
                            mCloseISwitchCallback.swich(ServiceResult.WiFiProbe_Close_Failed, "关闭失败");
                    }*/
                    closeProbe();
                    break;
                case 1:
                    if (mWifiManager.isWifiEnabled() == false) {
                        mWifiManager.setWifiEnabled(true);
                    } else {
                        openProbe();
                    }
                    break;
                case 2:
                    Log.d(TAG, "switchWifiProbe = " + isWifiP2pEnabled);
                    /*if (!isWifiP2pEnabled) {
                        if (mISwitchCallback != null)
                            mISwitchCallback.swich(ServiceResult.WiFiProbe_Stop_Failed, "停止失败");
                    } else {*/
                        mPortalHandler.removeMessages(MESSAGE_PROBE_WORK);
                    mPortalHandler.removeMessages(MESSAGE_PROBE_FORCE_RESPONSE);
                        if(manager != null && channel != null) {
                            manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {

                                @Override
                                public void onSuccess() {
                                    Log.i(TAG, "stopPeerDiscovery Initiated");
                                }

                                @Override
                                public void onFailure(int reasonCode) {
                                    Log.i(TAG, "stopPeerDiscovery Failed : " );
                                }
                            });
                            if (mISwitchCallback != null)
                                mISwitchCallback.swich(WiFiProbe_Stop_Succeed, "停止成功");
                        } else {
                            if (mISwitchCallback != null)
                                mISwitchCallback.swich(WiFiProbe_Stop_Failed, "停止失败");
                        }
                    //}

                    break;
                case 3://开始接收数据
                    if(manager != null && channel != null) {
                        mPortalHandler.sendEmptyMessageDelayed(MESSAGE_PROBE_FORCE_RESPONSE, SCANWIFI_DELAY);
                        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "Discovery Initiated");
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                Log.i(TAG, "Discovery Failed : " );
                            }
                        });
                    } else {
                        //LogHelper.appendLog(LogHelper.LOG_NAME, "manager = null discoverPeers fialed");
                    }
                    break;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void closeProbe() {
        try{
            if(receiver != null) {
                mContext.unregisterReceiver(receiver);
            }
            if(wakeLock != null)
                wakeLock.release();
            if (manager != null && channel != null) {
                manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "stopPeerDiscovery Initiated");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.i(TAG, "stopPeerDiscovery Failed : ");
                        /*try{
                            if (mCloseISwitchCallback != null)
                                mCloseISwitchCallback.swich(ServiceResult.WiFiProbe_Close_Failed, "关闭失败");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }*/
                    }
                });

                /*manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "stopPeerDiscovery Initiated");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.i(TAG, "stopPeerDiscovery Failed : ");
                        try{
                            if (mISwitchCallback != null)
                                mISwitchCallback.swich(6, "关闭失败");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });*/
                if (mCloseISwitchCallback != null)
                    mCloseISwitchCallback.swich(WiFiProbe_Close_Succeed, "关闭成功");
            } else {
                try{
                    if (mCloseISwitchCallback != null)
                        mCloseISwitchCallback.swich(WiFiProbe_Close_Failed, "关闭失败");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            receiver = null;
            channel = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void openProbe() throws Exception{
        if (mWifiManager.isWifiEnabled() == false) {
            Log.e(TAG, "isWifiEnabled = " + false);
            //LogHelper.appendLog(LogHelper.LOG_NAME, "isWifiEnabled = false");
            if (mOpenISwitchCallback != null)
                mOpenISwitchCallback.swich(WiFiProbe_Open_Failed, "打开失败");
        }
        try {
            if(channel == null)
                channel = manager.initialize(mContext, mContext.getMainLooper(), null);
            if(receiver == null) {
                receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
                mContext.registerReceiver(receiver, intentFilter);
            }
            if(wakeLock != null)
                wakeLock.acquire();
            if (mOpenISwitchCallback != null)
                mOpenISwitchCallback.swich(WiFiProbe_Open_Succeed, "打开成功");
        } catch (SecurityException e) {
            if (mOpenISwitchCallback != null)
                mOpenISwitchCallback.swich(WiFiProbe_Open_Failed, "打开失败");
        } catch (InvalidParameterException e) {
            if (mOpenISwitchCallback != null)
                mOpenISwitchCallback.swich(WiFiProbe_Open_Failed, "打开失败");
        }
    }

    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Log.d(TAG, "Channel lost. Trying again");
            retryChannel = true;
            manager.initialize(mContext, mContext.getMainLooper(), this);
        } else {
            Log.d(TAG,"Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.");
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        if(mInputListeners != null && mInputListeners.size() > 0) {
            mPortalHandler.removeMessages(MESSAGE_PROBE_WORK);
            synchronized (peers) {
                peers.clear();
                peers.addAll(wifiP2pDeviceList.getDeviceList());
                long time = System.currentTimeMillis();
                int size = peers.size();
                Log.i(TAG, "onPeersAvailable  TID " + Thread.currentThread().getId() + "  size = " + size + " Listener= " + mInputListeners.size());
                //LogHelper.appendLog(LogHelper.LOG_NAME, "onPeersAvailable " + "  size = " + size);
                if(size > 2 && peerCounts > 0) {
                    //LogHelper.appendLog(LogHelper.LOG_NAME, "onPeersAvailable " + "  size = " + size);
                    peerCounts = peerCounts - 1;
                }
                try{
                    for(int i = 0; i < size; i++) {
                        WifiP2pDevice device = peers.get(i);
                        if (device != null) {
                            //Log.i(TAG, "onPeersAvailable = " + device.deviceAddress + " status:" + device.status);
                            updateWifiProbecallBack(device.deviceAddress, "" + device.status, time);//TODO
                            //mOnTaskStaListener.getWiFiProbeOfSta(device.deviceAddress, "" + device.status, time);
                        }
                        device = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mPortalHandler.sendEmptyMessageDelayed(MESSAGE_PROBE_WORK, 6*10*1000);//收到数据后下次扫描
        } else {
            if(mInputListeners != null) {
                Log.i(TAG, "onPeersAvailable  TID " + Thread.currentThread().getId() +  " Listener= " + mInputListeners.size());
            } else {
                Log.i(TAG, "onPeersAvailable  TID " + Thread.currentThread().getId() +  " Listener null" );
            }
        }
    }
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private OnSwitchListener mISwitchCallback;
    private OnSwitchListener mCloseISwitchCallback;
    private OnSwitchListener mOpenISwitchCallback;
    private OnTaskStaListener mOnTaskStaListener;
    private synchronized void updateWifiProbecallBack(String probeinfo,String rssi,long time) {
        synchronized(mInputListeners) {
            int size = mInputListeners.size();
            for (int i = 0; i < size; i++) {
                TaskStaListener listener = mInputListeners.get(i);
                try {
                    listener.mListener.getWiFiProbeOfSta(probeinfo, rssi, time);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in reportScanInfo");
                    mInputListeners.remove(listener);
                    // adjust for size of list changing
                    size--;
                }
                listener = null;
            }
        }
    }
    private class WiFiProbeServiceStub extends WiFiProbeService.Stub {
        WeakReference<WiFiProbeServiceImpl> mService;
        public WiFiProbeServiceStub(WiFiProbeServiceImpl service) {
            Log.i(TAG, "WiFiProbeServiceStub  TID " + Thread.currentThread().getId() + "  pid = " + Binder.getCallingPid() + " uid= " + Binder.getCallingPid());
            mService = new WeakReference<WiFiProbeServiceImpl>(service);
        }
        public void openWifiStaProbe(OnSwitchListener icb) {
            //mService.get().isNetworkAvailable();
            if(icb != null) {
                mOpenISwitchCallback = icb;
            }
            //1（开启成功），3（版本不支持），4（开始失败），msg是返回的具体文本信息
            Log.i(TAG, "openWifiStaProbe");
            mService.get().switchWifiProbe(1);
        }
        //注意：调用该接口关闭探针功能，探针实际停止扫描附近设备，但也不应关闭WIFI模块影响终端正常使用wifi功能。5（关闭成功），6（关闭失败），msg是返回的具体文本信息
        public void closeWifiStaProbe(OnSwitchListener icb) {
            Log.i(TAG, "closeWifiStaProbe");
            if(icb != null) {
                mCloseISwitchCallback = icb;
            }
            mService.get().switchWifiProbe(0);
        }
        //停止接收数据 注意：调用该接口关闭探针信息，不应关闭WIFI模块，探针实际仍然在扫描附近设备。
        public void closeWifiStaProbeInfo(OnSwitchListener icb) {
            if(icb != null) {
                mISwitchCallback = icb;
            }
            Log.i(TAG, "closeWifiStaProbeInfo");
            mService.get().switchWifiProbe(2);
        }
        //注册接收数据的接口回调
        public void registerStaCallback(OnTaskStaListener cb) {
            if(cb != null) {
                Log.i(TAG, "registerStaCallback" + cb);
                mService.get().addTaskStaActionListener(cb);
            }
        }
        //注销接收数据的接口回调
        public void unregisterStaCallback(OnTaskStaListener cb) {
            if(cb != null) {
                Log.i(TAG, "unregisterStaCallback");
                mService.get().removeTaskStaActionListener(cb);
            }
        }
        public void startGetStaProbeInfo() {
            Log.i(TAG, "startGetStaProbeInfo");
            mService.get().switchWifiProbe(3);
        }
    }
    private ArrayList<TaskStaListener> mInputListeners;
    private final class TaskStaListener implements IBinder.DeathRecipient {
        final OnTaskStaListener mListener;

        TaskStaListener(OnTaskStaListener listener) {
            mListener = listener;
        }

        public void binderDied() {
            Log.d(TAG, "wifi probe listener died");
            if(mInputListeners != null) {
                synchronized(mInputListeners) {
                    mInputListeners.remove(this);
                }
                if (mListener != null) {
                    mListener.asBinder().unlinkToDeath(this, 0);
                }
            }
        }
    }

    public void addTaskStaActionListener(OnTaskStaListener listener) {
        try{
            if(mInputListeners != null) {
                synchronized(mInputListeners) {
                    mInputListeners.clear();//only one listener
                    IBinder binder = listener.asBinder();
                    int size = mInputListeners.size();
                    for (int i = 0; i < size; i++) {
                        TaskStaListener test = mInputListeners.get(i);
                        if (binder.equals(test.mListener.asBinder())) {
                            // listener already added
                            test = null;
                            return;
                        }
                        test = null;
                    }
                    Log.e(TAG, "addTaskStaActionListener");
                    TaskStaListener l = new TaskStaListener(listener);
                    binder.linkToDeath(l, 0);
                    mInputListeners.add(l);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "addReadActionListener failed", e);
        }

    }

    public void removeTaskStaActionListener(OnTaskStaListener listener) {
        if(mInputListeners != null) {
            synchronized(mInputListeners) {
                for(TaskStaListener task : mInputListeners) {
                    if(task.mListener.asBinder() == listener.asBinder()) {
                        listener.asBinder().unlinkToDeath(task, 0);
                        mInputListeners.remove(task);
                        Log.e(TAG, "removeTaskStaActionListener");
                        task = null;
                        break;
                    }
                }
            }
        }
    }
}
