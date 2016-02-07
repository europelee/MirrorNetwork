package org.eu.mirror.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * 
 * @ClassName: NetConnStatusReceiver
 * @Description: TODO
 * @author orion.li
 * @date 2016-1-15 ÏÂÎç1:57:08
 * 
 */
public class NetConnStatusReceiver extends BroadcastReceiver {

	private NetConnStatusListener mListener = null;
	private static String NET_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

	public void setNetConnStatusListener(NetConnStatusListener li) {
		mListener = li;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if (false == intent.getAction().equals(NET_CHANGE_ACTION)) {
			Log.d(NetConnStatusReceiver.class.getName(), intent.getAction());
			return;
		}

		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiNetInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (!wifiNetInfo.isConnected()) {
			if (mListener != null) {
				mListener.onWifiConnect(false);
			}
		} else {
			if (mListener != null) {
				mListener.onWifiConnect(true);
			}
		}
	}

	public interface NetConnStatusListener {
		public void onWifiConnect(boolean isConnected);
	}
}
