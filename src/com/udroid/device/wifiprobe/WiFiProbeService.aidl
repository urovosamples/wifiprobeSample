package com.udroid.device.wifiprobe;
import com.udroid.device.wifiprobe.OnTaskStaListener;
import com.udroid.device.wifiprobe.OnSwitchListener;
interface WiFiProbeService{
	void openWifiStaProbe(in OnSwitchListener icb);
	void closeWifiStaProbe(in OnSwitchListener icb);
	void closeWifiStaProbeInfo(in OnSwitchListener icb);
	void registerStaCallback(in OnTaskStaListener cb);
	void unregisterStaCallback(in OnTaskStaListener cb);
	void startGetStaProbeInfo();
}