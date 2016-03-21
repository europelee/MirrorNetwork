package org.eu.mirror.network;
/**
 * for supporting different network communication solution
 * @author orion.li
 *
 */
public interface INetworkConnection {

	/**
	 * 
	* @Title: start  
	* @Description: cli/svr endpoint connects into network(connect/bind) 
	* void
	* @throws
	 */
	public void start();
	
	/**
	 * 
	* @Title: send  
	* @Description: send msg to peer, notice:
	* for supporting cli:svr=*:1
	* if svr sends msg to cli, mirrormessage req must set peerAddress.
	* if cli sends msg to svr, 	peerAddress not need set.			 
	* @param req
	* @return
	* MIRROR_TRANSFSTATUS
	* @throws
	 */
	public MIRROR_NETSTATUS send(MirrorMessage req);
	
	/**
	 * 
	* @Title: stop  
	* @Description: just close network connection, you can start again for connecting again
	* 				recommend using stop for simple. 
	* void
	* @throws
	 */
	public void stop();
	
	/**
	 * 
	* @Title: close  
	* @Description: close network connection and release all resources. 
	* void
	* @throws
	 */
	public void close();
	
	/**
	 * 
	* @Title: setMirrorMsgListener  
	* @Description: set msglistener for recving msg from peers,
	* 				you are allowed registering listener once for each appType. 
	* @param appType
	* @param li
	* @return
	* boolean
	* @throws
	 */
	public boolean setMirrorMsgListener(MIRROR_APPTYPE appType, IMirrorMsgListener li);
	
	/**
	 * 
	* @Title: isClosed  
	* @Description: check if network connection was closed.
	* @return
	* boolean
	* @throws
	 */
	public boolean isClosed();
	
	/**
	 * 
	* @Title: setIMirrorNetMonitor  
	* @Description: a special listener for monitoring mirror netwrok status
	* 				case: connect, disconnect, error 
	* @param mon
	* void
	* @throws
	 */
	public void setIMirrorNetMonitor(IMirrorNetMonitor mon);
}
