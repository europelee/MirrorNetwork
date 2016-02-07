package org.eu.mirror.network;

/**
 * interface for recving network msg. app layer need implement it
 * @author orion.li
 *
 */
public interface IMirrorMsgListener {
	
	/**
	 * recv data from network, then do some work
	 * @param msg
	 */
	public void onData(String peerAddress, Object o);
	
	/**
	 * net data invaid, or network disconnect etc.
	 * @param errCode
	 * @param errInfo
	 */
	public void onError(MIRROR_NETSTATUS errCode, String errInfo);
}
