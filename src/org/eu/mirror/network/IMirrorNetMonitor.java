package org.eu.mirror.network;
/**
 * 
* @ClassName: IMirrorNetMonitor  
* @Description: monitoring mirror network status 
* @author orion.li  
* @date 2016-3-8 上午10:10:01  
*
 */
public interface IMirrorNetMonitor {
	
	public void onError(MIRROR_NETSTATUS errCode, String errInfo);
	
	public void onConnect(String peerAddr);
	
	public void onDisconnect(MIRROR_NETSTATUS DisCode, String desc);
}
