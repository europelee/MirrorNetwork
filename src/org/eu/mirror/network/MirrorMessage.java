package org.eu.mirror.network;

import java.io.Serializable;

/**
 * 
 * @ClassName: MirrorMessage
 * @Description: TODO
 * @author orion.li
 * @date 2016-1-19 ÏÂÎç2:17:32
 * 
 */
public class MirrorMessage implements Serializable {
	
	/**  
	* @Fields serialVersionUID : TODO 
	*/  
	private static final long serialVersionUID = 1L;
	private MIRROR_APPTYPE appType;
	private String peerAddress;
	private Object content;

	public MirrorMessage(MIRROR_APPTYPE appType, String peerAddress,
			Object content) {
		this.appType = appType;
		this.peerAddress = peerAddress;
		this.content = content;
	}

	public MIRROR_APPTYPE getAppType() {
		return appType;
	}

	public void setAppType(MIRROR_APPTYPE appType) {
		this.appType = appType;
	}

	public String getPeerAddress() {
		return peerAddress;
	}

	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}
}
