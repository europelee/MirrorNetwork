package org.eu.mirror.network;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;


import android.util.Log;

/**
 * 
 * @ClassName: NettyServer
 * @Description: TODO
 * @author orion.li
 * @date 2016-1-16 ÏÂÎç3:45:54
 * 
 */
public class NettyServer implements INetworkConnection{

	private static final String LOG_TAG = NettyServer.class.getName();
	private AtomicBoolean mIsRunning;
	private ServerBootstrap bootstrap = null;
	private Channel bind = null;
	private int port;
	private Set<Channel> connChannelSet;
	private Object channelConning = new Object();
	private final AtomicBoolean being_closed;
	private HashMap<MIRROR_APPTYPE, IMirrorMsgListener> msgObserList;

	public NettyServer(int port) {
		this.setPort(port);
		mIsRunning = new AtomicBoolean(false);
		connChannelSet = new HashSet<Channel>();
		being_closed = new AtomicBoolean(false);
		msgObserList = new HashMap<MIRROR_APPTYPE, IMirrorMsgListener>();
	}

	@SuppressWarnings("unused")
	private void sayHello(String peer) {
		if (isClosed()) {
			Log.w(LOG_TAG,
					"Client is being closed, and does not take requests any more");
			return;
		}

		MirrorMessage reqO = new MirrorMessage(MIRROR_APPTYPE.MIRROR, peer,
				"hello nettyclient");

		synchronized (channelConning) {
			Iterator<Channel> iter = connChannelSet.iterator();
			while (iter.hasNext()) {
				Channel tmp = iter.next();
				if (tmp.getRemoteAddress().toString().equals(peer)) {
					tmp.write(reqO);
					break;
				}
			}
		}

	}

	@Override
	public boolean setMirrorMsgListener(MIRROR_APPTYPE appType,
			IMirrorMsgListener li) {
		if (msgObserList.containsKey(appType)) {
			Log.w(LOG_TAG, "there exists apptype " + appType
					+ " listener registered in");
			return false;
		}

		if (li == null) {
			Log.e(LOG_TAG, "listener is null");
			return false;
		}

		msgObserList.put(appType, li);

		return true;
	}

	@Override
	public boolean isClosed() {
		return being_closed.get();
	}

	public AtomicBoolean getBeing_closed() {
		return being_closed;
	}

	@Override
	public void start() {

		Log.i(LOG_TAG, "start");
		if (mIsRunning.compareAndSet(false, true)) {
			Log.d(LOG_TAG, "NettyServer start running");
		} else {
			Log.d(LOG_TAG, "NettyServer already running");
			return;
		}

		if (null == bootstrap) {
			bootstrap = new ServerBootstrap(
					(ChannelFactory) new NioServerSocketChannelFactory(
							Executors.newCachedThreadPool(),
							Executors.newCachedThreadPool()));

			// Set up the default event pipeline.
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					// return Channels.pipeline(new StringDecoder(),
					// new StringEncoder(), new ServerHandler());
					return Channels.pipeline(
							new ObjectDecoder(
									ClassResolvers
											.weakCachingConcurrentResolver(MirrorMessage.class
													.getClassLoader())),
							new ObjectEncoder(), new ServerHandler());
				}
			});

			bootstrap.setOption("receiveBufferSize", 1048576);

		}

		// Bind and start to accept incoming connections.
		if (null == bind) {
			bind = bootstrap.bind(new InetSocketAddress(port));
			Log.i(LOG_TAG, "Server bind: " + bind.getLocalAddress());
		}
	}

	@Override
	public void stop() {
		Log.i(LOG_TAG, "stop");

		if (bind == null)
			return;

		bind.close().awaitUninterruptibly();
		bind = null;

		synchronized (channelConning) {
			this.connChannelSet.clear();
		}

		mIsRunning.set(false);
	}

	@Override
	public void close() {
		Log.i(LOG_TAG, "close");

		if (being_closed.compareAndSet(false, true) == false) {
			Log.i(LOG_TAG, "Netty client has been closed.");
			return;
		}
		stop();
		if (null != bootstrap) {
			bootstrap.releaseExternalResources();
			bootstrap = null;
		}

		mIsRunning.set(false);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	private class ServerHandler extends SimpleChannelHandler {
		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {

			// Log all channel state changes.
			if (e instanceof ChannelStateEvent) {
				Log.d(LOG_TAG, "Channel state changed: " + e);
			}

			super.handleUpstream(ctx, e);
		}

		@Override
		public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {

			// Log all channel state changes.
			if (e instanceof MessageEvent) {
				Log.d(LOG_TAG, "Writing:: " + e);
			}

			super.handleDownstream(ctx, e);
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			if (e.getMessage() instanceof String) {
				String message = (String) e.getMessage();
				Log.d(LOG_TAG, "Client "
						+ e.getChannel().getRemoteAddress().toString()
						+ " send:" + message);

				e.getChannel().write("Server alreay recv:" + message);

			}

			if (e.getMessage() instanceof MirrorMessage) {
				MirrorMessage msg = (MirrorMessage) e.getMessage();
				Log.d(LOG_TAG,
						"Client "
								+ e.getChannel().getRemoteAddress().toString()
								+ " send: " + msg.getAppType() + " "
								+ msg.getPeerAddress() + " " + msg.getContent());
				// sayHello(e.getChannel().getRemoteAddress().toString());
				IMirrorMsgListener li = msgObserList.get(msg.getAppType());
				if (null == li) {
					Log.e(LOG_TAG,
							"there not listener for app " + msg.getAppType());
				} else {
					li.onData(e.getChannel().getRemoteAddress().toString(),
							msg.getContent());
				}
			} else {
				Log.d(LOG_TAG, "not a MirrorMessage");
				broadcastNetStatus2Listener(MIRROR_NETSTATUS.NET_MSGINVALID, "");
			}

			super.messageReceived(ctx, e);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			broadcastNetStatus2Listener(MIRROR_NETSTATUS.NET_EXCEPTION, e
					.getCause().toString());
			super.exceptionCaught(ctx, e);
		}

		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			Log.i(LOG_TAG, "a client come on¡£¡£¡£");
			Log.i(LOG_TAG, "Client:" + e.getChannel().getRemoteAddress());
			Log.i(LOG_TAG, "Server:" + e.getChannel().getLocalAddress());
			synchronized (channelConning) {
				connChannelSet.add(e.getChannel());
			}

			super.channelConnected(ctx, e);
		}

		@Override
		public void channelDisconnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {

			Log.i(LOG_TAG,
					"Receive channelDisconnected, channel = " + e.getChannel());

			synchronized (channelConning) {
				if (connChannelSet.contains(e.getChannel())) {
					connChannelSet.remove(e.getChannel());
				} else {
					Log.i(LOG_TAG, "there no exists " + e.getChannel());
				}
			}
			super.channelDisconnected(ctx, e);

		}
	}

	/**
	 * 
	 * @Title: send
	 * @Description: TODO
	 * @param req
	 * @return MIRROR_TRANSFSTATUS
	 * @throws
	 */
	@Override
	public MIRROR_NETSTATUS send(MirrorMessage req) {

		Log.i(LOG_TAG, "send");
		MIRROR_NETSTATUS ret = MIRROR_NETSTATUS.SEND_FAIL;
		if (isClosed()) {
			Log.w(LOG_TAG,
					"Client is being closed, and does not take requests any more");
			broadcastNetStatus2Listener(MIRROR_NETSTATUS.NET_DISCONNECT,
					"Client is being closed, and does not take requests any more");
			return MIRROR_NETSTATUS.SEND_FAIL;
		}

		if (null == req.getPeerAddress()) {
			Log.e(LOG_TAG, "need set client address");
			broadcastNetStatus2Listener(MIRROR_NETSTATUS.SEND_FAIL,
					"need set client address");				
			return MIRROR_NETSTATUS.SEND_FAIL;
		}

		synchronized (channelConning) {
			Iterator<Channel> iter = connChannelSet.iterator();
			while (iter.hasNext()) {
				Channel tmp = iter.next();
				if (tmp.getRemoteAddress().toString()
						.equals(req.getPeerAddress())) {

					if (tmp.isWritable()) {
						tmp.write(req);
						ret = MIRROR_NETSTATUS.SEND_SUCC;
					} else {
						ret = MIRROR_NETSTATUS.SEND_AGAIN;
					}
					break;
				}
			}
		}

		return ret;
	}
	
	private void broadcastNetStatus2Listener(MIRROR_NETSTATUS status,
			String addInfo) {
		Iterator<Entry<MIRROR_APPTYPE, IMirrorMsgListener>> iter = msgObserList
				.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<MIRROR_APPTYPE, IMirrorMsgListener> entry = iter.next();
			MIRROR_APPTYPE key = (MIRROR_APPTYPE) entry.getKey();
			Log.d(LOG_TAG, "notice netstatus to " + key);
			IMirrorMsgListener val = (IMirrorMsgListener) entry.getValue();
			val.onError(status, addInfo);
		}
	}
}
