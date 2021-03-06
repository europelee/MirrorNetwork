package org.eu.mirror.network;
/**
 * 
* @ClassName: MIRROR_TRANSFSTATUS  
* @Description: TODO 
* @author orion.li  
* @date 2016-1-21 ����2:10:08  
*
 */

public enum MIRROR_NETSTATUS {
	/**
	 * send-status should be removed from the class better. 
	 */
	SEND_SUCC(0, "send succ"), 
	SEND_FAIL(1, "send fail"), 
	SEND_AGAIN(2, "hold and try to send again"),
	NET_CONNCLOSE(3, "connection already was closed"),
	NET_EXCEPTION(4, "network exception"),
	NET_DISCONNECT(5, "network disconnect"),
	NET_MSGINVALID(6, "network MSG invalid"),
	NET_CONNECT(7, "network connect");
	
	private int statusCode;
	private String statusDesc;
	
	private MIRROR_NETSTATUS(int statusCode, String statusDesc) {
		this.setStatusCode(statusCode);
		this.setStatusDesc(statusDesc);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusDesc() {
		return statusDesc;
	}

	public void setStatusDesc(String statusDesc) {
		this.statusDesc = statusDesc;
	}
}
