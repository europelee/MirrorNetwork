package org.eu.mirror.network;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * 
 * @ClassName: NetworkServiceDiscovery
 * @Description: refer from https://github.com/alwx/android-jmdns
 * @author orion.li
 * @date 2016-1-16 ÉÏÎç10:26:48
 * 
 */
public class NetworkServiceDiscovery {
	private final String LOG_TAG = NetworkServiceDiscovery.class.getName();
	private final String TYPE = "_mirror._tcp.local.";
	private String SERVICE_NAME = "LocalMirrorComm";
	private String  mMacAddr = null;
	private Context mContext = null;
	private JmDNS mJmDNS = null;
	private ServiceInfo mServiceInfo = null;
	private ServiceListener mServiceListener = null;
	private WifiManager.MulticastLock mMulticastLock = null;

	public NetworkServiceDiscovery(Context context) {
		mContext = context;
	}

	public boolean initNSD() {

		boolean bRet = isNetworkAvailable();
		if (false == bRet)
			return bRet;

		try {
			WifiManager wifi = (WifiManager) mContext
					.getSystemService(android.content.Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifi.getConnectionInfo();
			
			mMacAddr = wifiInfo.getMacAddress();
			SERVICE_NAME = SERVICE_NAME + "-" + mMacAddr;
			int intaddr = wifiInfo.getIpAddress();

			byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff),
					(byte) (intaddr >> 8 & 0xff),
					(byte) (intaddr >> 16 & 0xff),
					(byte) (intaddr >> 24 & 0xff) };
			InetAddress addr = InetAddress.getByAddress(byteaddr);
			mJmDNS = JmDNS.create(addr);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error in JmDNS creation: " + e);
			return false;
		}

		return true;
	}

	public void startNetworkService(int port) {
		try {
			wifiLock();
			mServiceInfo = ServiceInfo.create(TYPE, SERVICE_NAME, port,
					SERVICE_NAME);
			mJmDNS.registerService(mServiceInfo);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error in JmDNS initialization: " + e);
		}
	}

	public void searchNetworkService(final OnFoundListener listener) {
		mJmDNS.addServiceListener(TYPE,
				mServiceListener = new ServiceListener() {
					@Override
					public void serviceAdded(ServiceEvent serviceEvent) {
						Log.d(LOG_TAG,
								"Service added   : " + serviceEvent.getName()
										+ "  " + serviceEvent.getType());
						ServiceInfo info = mJmDNS.getServiceInfo(
								serviceEvent.getType(), serviceEvent.getName());
						listener.onFound(info);
					}

					@Override
					public void serviceRemoved(ServiceEvent serviceEvent) {
						Log.d(LOG_TAG,
								"serviceRemoved: " + serviceEvent.getInfo());
					}

					@Override
					public void serviceResolved(ServiceEvent serviceEvent) {
						Log.d(LOG_TAG,
								"Service resolved: " + serviceEvent.getInfo());
						mJmDNS.requestServiceInfo(serviceEvent.getType(),
								serviceEvent.getName(), 1);
					}
				});
	}

	public void closeNSD() {
		if (mJmDNS != null) {
			if (mServiceListener != null) {
				mJmDNS.removeServiceListener(TYPE, mServiceListener);
				mServiceListener = null;
			}
			mJmDNS.unregisterAllServices();
			try {
				mJmDNS.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (mMulticastLock != null && mMulticastLock.isHeld()) {
			mMulticastLock.release();
		}
	}

	private void wifiLock() {
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(android.content.Context.WIFI_SERVICE);
		mMulticastLock = wifiManager.createMulticastLock(SERVICE_NAME);
		mMulticastLock.setReferenceCounted(true);
		mMulticastLock.acquire();
	}

	private boolean isNetworkAvailable() {

		ConnectivityManager connectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connectivityManager == null) {
			return false;
		} else {

			NetworkInfo networkInfo = connectivityManager
					.getActiveNetworkInfo();

			if (networkInfo != null && networkInfo.isConnected()) {

				Log.d(LOG_TAG,  "===×´Ì¬==="
						+ networkInfo.getState());
				Log.d(LOG_TAG,  "===ÀàÐÍ==="
						+ networkInfo.getTypeName());
				
				if (networkInfo.getState() == NetworkInfo.State.CONNECTED
						&& networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					return true;
				}

			}
		}
		return false;
	}

	public interface OnFoundListener {
		void onFound(ServiceInfo info);
	}
}
