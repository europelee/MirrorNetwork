package org.eu.mirror.network;
/**
 * 
* @ClassName: MIRROR_APPTYPE  
* @Description: TODO 
* @author orion.li  
* @date 2016-1-19 обнГ2:16:38  
*
 */
public enum MIRROR_APPTYPE {
	MIRROR(0), WEATHER(1), MAP(2), MUSIC(3), SCHEDULE(4), MAIL(5), VOICE(6), CAMERA(7), DATE(8), TOUCHPAD(9), SETTINGS(10);

	private int appId;
	
	private MIRROR_APPTYPE(int appId) {
		this.setAppId(appId);
	}

	public int getAppId() {
		return appId;
	}

	public void setAppId(int appId) {
		this.appId = appId;
	}
}
