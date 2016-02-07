package org.eu.mirror.network;

/**
 * 
 * @ClassName: WifiInfor
 * @Description: TODO
 * @author orion.li
 * @date 2016-1-15
 * 
 */

public class WifiInfor {
	private String ssid;
	private String passwd;
	private WIFI_SECURITY security;

	WifiInfor(String ssid, String passwd, WIFI_SECURITY security) {
		this.setSsid(ssid);
		this.setPasswd(passwd);
		this.setSecurity(security);
	}

	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public WIFI_SECURITY getSecurity() {
		return security;
	}

	public void setSecurity(WIFI_SECURITY security) {
		this.security = security;
	}
}
