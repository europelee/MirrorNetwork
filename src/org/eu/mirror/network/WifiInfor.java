package org.eu.mirror.network;

import java.io.Serializable;

/**
 * 
 * @ClassName: WifiInfor
 * @Description: TODO
 * @author orion.li
 * @date 2016-1-15
 * 
 */

public class WifiInfor implements Serializable {
	/**  
	* @Fields serialVersionUID : TODO 
	*/  
	private static final long serialVersionUID = 1L;
	private String SSID;
	private String PASSWORD;
	private WIFI_SECURITY TYPE;

	WifiInfor(String ssid, String passwd, WIFI_SECURITY security) {
		this.setSsid(ssid);
		this.setPasswd(passwd);
		this.setSecurity(security);
	}

	public String getSsid() {
		return SSID;
	}

	public void setSsid(String ssid) {
		this.SSID = ssid;
	}

	public String getPasswd() {
		return PASSWORD;
	}

	public void setPasswd(String passwd) {
		this.PASSWORD = passwd;
	}

	public WIFI_SECURITY getSecurity() {
		return TYPE;
	}

	public void setSecurity(WIFI_SECURITY security) {
		this.TYPE = security;
	}
}
