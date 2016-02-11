# MirrorNetwork

Most of network programming on android are http based, it is ok to communication over WAN, such as Volley, android-async-http etc, but sometimes There are other solutions better suited to the application than http-based on communication over LAN, MirrorNetwork provides a proximal tcp networking technology based jmdns and netty.

##user guide

###client

1. find server based jmdns

        svr = new NetworkServiceDiscovery(MainActivity.this);
        if (false == svr.initNSD()) {
        	Log.d(TAG, "initMDNS fail");
        	return;
        }
        svr.searchNetworkService(new NetworkServiceDiscovery.OnFoundListener() {
        
        	@Override
        	public void onFound(ServiceInfo info) {
        		// TODO Auto-generated method stub
        		Log.d(TAG, info.getHostAddresses()[0]+" "+info.getPort());
        		MainActivity.this.ip = info.getHostAddresses()[0]; //get server's ip
        	}
        });
2. start client to connect server and set a application type and the corresponding listener for recving data from server 
          
          INetworkConnection cli = null;
          if (cli == null) {
          cli = new NettyClient(MainActivity.this.ip,  8000);
          }
          cli.setMirrorMsgListener(MIRROR_APPTYPE.MAP, new testMapMsgListener());
          cli.start();

        public class testMapMsgListener implements IMirrorMsgListener {
        
        	@Override
        	public void onData(String arg0, Object arg1) {
        		// TODO Auto-generated method stub
        		testMap map = (testMap) arg1;
        		}
        	}
        
        	@Override
        	public void onError(MIRROR_NETSTATUS arg0, String arg1) {
        		// TODO Auto-generated method stub
        		
        	}
        	
        }
        
3. send msg to server(you can make a class implements Serializable, like testMap)

        //
        MirrorMessage msg = new MirrorMessage(MIRROR_APPTYPE.MAP, null, new testMap(34, null));
        cli.send(msg);

###server

1. start network service based jmdns

        svr = new NetworkServiceDiscovery(MainActivity.this);
        if (svr.initNSD() == false) {
        	Log.d("ssssssssss", "initNSD fail");
        	return;
        }
        
        int port = NetworkUtil.getAvailablePort();
        if (-1 == port) {
        	Log.d("xxxxx", " not any available ports!");
        	return;
        }
        
        svr.startNetworkService(port);
        
2. start server and set a application type and the corresponding listener for recving data from client

        if (nettysvr == null) {
        nettysvr = new NettyServer(8000);
        }
        nettysvr.setMirrorMsgListener(MIRROR_APPTYPE.MAP, new testMapMsgListener());
        nettysvr.start();
       
3. send msg to client

        //peerAddr can be got from the first param of testMapMsgListener onData function
        
        //MirrorNetwork support multi clients connecting server, and server send msg to one of them with peerAddr
        
        nettysvr.send(new MirrorMessage(MIRROR_APPTYPE.MAP, peerAddr, new testMap(35, null)));
		
