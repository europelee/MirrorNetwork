package org.eu.mirror.network;

import android.net.wifi.WifiConfiguration.KeyMgmt;
/**
 * 
* @ClassName: WIFI_SECURITY  
* @Description: TODO 
* @author orion.li  
* @date 2016-1-15 
*
 */
public enum WIFI_SECURITY {

	SECURITY_NONE(KeyMgmt.NONE), SECURITY_WEP(KeyMgmt.NONE), SECURITY_PSK(
			KeyMgmt.WPA_PSK), SECURITY_EAP(KeyMgmt.WPA_EAP);

	private int mAndroidKey;

	private WIFI_SECURITY(int keyCode) {
		this.setmAndroidKey(keyCode);
	}

	public int getmAndroidKey() {
		return mAndroidKey;
	}

	public void setmAndroidKey(int mAndroidKey) {
		this.mAndroidKey = mAndroidKey;
	}
}
