package org.eu.mirror.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import org.eu.mirror.network.NetConnStatusReceiver.NetConnStatusListener;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * 
 * @ClassName: NetworkUtil
 * @Description: TODO
 * @author orion.li
 * @date 2016-1-15
 * 
 */
public class NetworkUtil {

	private static final String LOG_TAG = NetworkUtil.class.getName();

	private static NetConnStatusReceiver mNetMonitor = new NetConnStatusReceiver();

	/**
	 * 
	 * @Title: getSecurity
	 * @Description: get wifi security
	 * @param config
	 * @return WIFI_SECURITY
	 * @throws
	 */
	private static WIFI_SECURITY getSecurity(WifiConfiguration config) {
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
			return WIFI_SECURITY.SECURITY_PSK;
		}
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP)
				|| config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
			return WIFI_SECURITY.SECURITY_EAP;
		}
		return (config.wepKeys[0] != null) ? WIFI_SECURITY.SECURITY_WEP
				: WIFI_SECURITY.SECURITY_NONE;
	}

	/**
	 * 
	 * @Title: getCurWifiInfo
	 * @Description: get ssid and security
	 * @param context
	 * @return WifiInfor
	 * @throws
	 */
	public static WifiInfor getCurWifiInfo(Context context) {
		WifiInfor info = null;

		WifiManager mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
		if (wifiInfo.getSSID() == null) {
			Log.d(LOG_TAG, "wifi ssid is null");
			return info;
		}

		ConnectivityManager connec = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
			WIFI_SECURITY security = null;
			List<WifiConfiguration> wifiConfigList = mWifiManager
					.getConfiguredNetworks();
			for (int i = 0; i < wifiConfigList.size(); i++) {
				Log.d(LOG_TAG, wifiConfigList.get(i).toString());

				if (wifiInfo.getSSID().equals(wifiConfigList.get(i).SSID)) {
					security = getSecurity(wifiConfigList.get(i));
					Log.i(LOG_TAG, "当前网络安全性：" + security);
					break;
				}
			}

			info = new WifiInfor(wifiInfo.getSSID(), "", security);
		}

		return info;
	}

	/**
	 * check port-available refer from
	 * jstorm-core/src/main/java/com/alibaba/jstorm/utils/NetWorkUtils.java
	 * 
	 * @author orion.li
	 * 
	 */
	/**
	 * Check whether the port is available to binding
	 * 
	 * @param port
	 * @return -1 means not available, others means available
	 * @throws IOException
	 */
	public static int tryPort(int port) throws IOException {
		ServerSocket socket = new ServerSocket(port); // port=0 : system would
														// autoly give an
														// available port
		int rtn = socket.getLocalPort();
		socket.close();
		return rtn;
	}

	/**
	 * Check whether the port is available to binding
	 * 
	 * @param prefered
	 * @return -1 means not available, others means available
	 */
	public static int availablePort(int prefered) {
		int rtn = -1;
		try {
			rtn = tryPort(prefered);
		} catch (IOException e) {

		}
		return rtn;
	}

	/**
	 * get one available port
	 * 
	 * @return -1 means failed, others means one availablePort
	 */
	public static int getAvailablePort() {
		return availablePort(0);
	}

	/**
	 * 
	 * @Title: startNetConnStatusMonitor
	 * @Description: TODO
	 * @param context
	 * @param li
	 *            void
	 * @throws
	 */
	public static void startNetConnStatusMonitor(Context context,
			NetConnStatusListener li) {
		IntentFilter filter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		mNetMonitor.setNetConnStatusListener(li);
		context.registerReceiver(mNetMonitor, filter);
	}

	/**
	 * 
	 * @Title: stopNetConnStatusMonitor
	 * @Description: TODO
	 * @param context
	 *            void
	 * @throws
	 */
	public static void stopNetConnStatusMonitor(Context context) {
		try {
			context.unregisterReceiver(mNetMonitor);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	}

	/**
	 * 
	 * @Title: transformUsuarioDTOToJson
	 * @Description: TODO
	 * @param usuario
	 * @return
	 * @throws Exception
	 *             String
	 * @throws
	 */
	private static String WifiInfor2Json(WifiInfor infor) throws Exception {
		return new Gson().toJson(infor, WifiInfor.class);
	}

	/**
	 * 
	 * @Title: toBitmap
	 * @Description: TODO
	 * @param matrix
	 * @return Bitmap
	 * @throws
	 */
	private static Bitmap toBitmap(BitMatrix matrix) {
		int height = matrix.getHeight();
		int width = matrix.getWidth();
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
			}
		}
		return bmp;
	}

	/**
	 * 
	 * @Title: genWifiConnQRCode
	 * @Description: TODO
	 * @param infor
	 * @param width
	 * @param height
	 * @return Bitmap
	 * @throws
	 */
	public static Bitmap genWifiConnQRCode(WifiInfor infor, int width,
			int height) {

		Bitmap bitmap = null;

		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		try {

			BitMatrix byteMatrix = qrCodeWriter.encode(WifiInfor2Json(infor),
					BarcodeFormat.QR_CODE, width, height);
			bitmap = toBitmap(byteMatrix);
		} catch (WriterException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bitmap;
	}
}
