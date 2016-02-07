package org.eu.mirror.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import android.util.Log;

/**
 * 
 * @ClassName: NettyClient
 * @Description: refer from
 *               https://github.com/alibaba/jstorm/blob/master/jstorm-
 *               core/src/main
 *               /java/com/alibaba/jstorm/message/netty/NettyClient.java
 * @author orion.li
 * @date 2016-1-16 ÏÂÎç1:29:36
 * 
 */
public class NettyClient implements INetworkConnection {

	private static final String LOG_TAG = NettyClient.class.getName();

	private String mIpAddress;
	private int mPort;

	private AtomicBoolean mIsRunning;
	private ClientBootstrap bootstrap = null;
	private AtomicReference<Channel> channelRef;
	private Object channelClosing = new Object();
	private Set<Channel> closingChannel;
	private final AtomicBoolean being_closed;
	private AtomicBoolean isConnecting;

	private int max_retries = 30;
	private int base_sleep_ms = 100;
	private int num_retries = 0;
	private HashMap<MIRROR_APPTYPE, IMirrorMsgListener> msgObserList;

	public NettyClient(String ipAddress, int port) {
		this.setmIpAddress(ipAddress);
		this.setmPort(port);
		mIsRunning = new AtomicBoolean(false);
		channelRef = new AtomicReference<Channel>(null);
		closingChannel = new HashSet<Channel>();
		being_closed = new AtomicBoolean(false);
		isConnecting = new AtomicBoolean(false);
		msgObserList = new HashMap<MIRROR_APPTYPE, IMirrorMsgListener>();
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

	/**
	 * close_n_release() is invoked after all messages have been sent.
	 */
	private void close_n_release() {
		if (channelRef.get() != null) {
			setChannel(null);
		}

	}

	private void disconnectChannel(Channel channel) {
		if (isClosed()) {
			// normal close, so dont need reconnect
			return;
		}

		if (channel == channelRef.get()) {
			setChannel(null);
			connect();
		} else {
			closeChannel(channel);
		}

	}

	private void closeChannel(final Channel channel) {
		synchronized (channelClosing) {
			if (closingChannel.contains(channel)) {
				Log.d(LOG_TAG, channel.toString() + " is already closed");
				return;
			}

			closingChannel.add(channel);
		}

		Log.d(LOG_TAG, channel.toString() + " begin to closed");
		ChannelFuture closeFuture = channel.close();
		closeFuture.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future)
					throws Exception {

				synchronized (channelClosing) {
					closingChannel.remove(channel);
				}
				Log.d(LOG_TAG, channel.toString() + " finish closed");
			}
		});
	}

	private void setChannel(Channel newChannel) {

		final Channel oldChannel = channelRef.getAndSet(newChannel);

		if (newChannel != null) {
			num_retries = 0;
		}

		final String oldLocalAddres = (oldChannel == null) ? "null"
				: oldChannel.getLocalAddress().toString();
		String newLocalAddress = (newChannel == null) ? "null" : newChannel
				.getLocalAddress().toString();
		Log.i(LOG_TAG, "Use new channel " + newLocalAddress
				+ " replace old channel " + oldLocalAddres);

		// avoid one netty client use too much connection, close old one
		if (oldChannel != newChannel && oldChannel != null) {

			closeChannel(oldChannel);
			Log.i(LOG_TAG, "Successfully close old channel " + oldLocalAddres);

		}
	}

	public String getmIpAddress() {
		return mIpAddress;
	}

	public void setmIpAddress(String mIpAddress) {
		this.mIpAddress = mIpAddress;
	}

	public int getmPort() {
		return mPort;
	}

	public void setmPort(int mPort) {
		this.mPort = mPort;
	}

	@Override
	public void start() {

		Log.i(LOG_TAG, "start");

		if (mIsRunning.compareAndSet(false, true)) {
			Log.d(LOG_TAG, "nettyclient start running");
		} else {
			Log.d(LOG_TAG, "nettyclient already running");
			return;
		}

		// Configure the client.
		if (null == bootstrap) {
			bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
					Executors.newCachedThreadPool(),
					Executors.newCachedThreadPool()));

			// Set up the default event pipeline.
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					// return Channels.pipeline(new StringDecoder(),
					// new StringEncoder(), new ClientHandler());
					return Channels.pipeline(
							new ObjectDecoder(
									ClassResolvers
											.weakCachingConcurrentResolver(MirrorMessage.class
													.getClassLoader())),
							new ObjectEncoder(), new ChunkedWriteHandler(),
							new ClientHandler());
				}
			});
			bootstrap.setOption("tcpNoDelay", true);
			bootstrap.setOption("writeBufferHighWaterMark", 3 * 1024 * 1024);
			bootstrap.setOption("writeBufferLowWaterMark", 2816 * 1024);
			bootstrap.setOption("sendBufferSize", 1048576);
		}

		// Start the connection attempt.
		connect();
	}

	private void reconnect() {
		Thread tmp = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				++num_retries;
				Log.d(LOG_TAG, "try reconnect " + num_retries);

				try {

					if (num_retries > max_retries) {
						Thread.sleep(base_sleep_ms * max_retries);
					} else {
						Thread.sleep(base_sleep_ms * num_retries);
					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				connect();
			}

		});
		tmp.start();
	}

	private void connect() {
		Log.i(LOG_TAG, "connect");
		Thread tmp = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				if (channelRef.get() != null) {
					Log.i(LOG_TAG, "already exists a channel, so give up");
					return;
				}

				if (isClosed() == true) {
					Log.i(LOG_TAG, "already close " + mIpAddress + " normally");
					return;
				}

				if (isConnecting.getAndSet(true)) {
					Log.i(LOG_TAG, "Connect twice " + mIpAddress);
					return;
				}

				ChannelFuture future = bootstrap.connect(new InetSocketAddress(
						mIpAddress, mPort));

				future.addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future)
							throws Exception {
						isConnecting.set(false);
						Channel channel = future.getChannel();
						if (future.isSuccess()) {
							// do something else
							Log.i(LOG_TAG, "Connection established, channel = "
									+ channel);
							setChannel(channel);

						} else {
							Log.i(LOG_TAG, "Failed to reconnect ... channel "
									+ channel + " cause " + future.getCause());
							reconnect();
						}
					}
				});
			}

		});
		tmp.start();
	}

	@SuppressWarnings("unused")
	private void sayHello() {
		if (isClosed()) {
			Log.w(LOG_TAG,
					"Client is being closed, and does not take requests any more");
			return;
		}

		if (null != this.channelRef.get()) {
			MirrorMessage reqO = new MirrorMessage(MIRROR_APPTYPE.MIRROR,
					mIpAddress, "hello nettysvr");
			this.channelRef.get().write(reqO);
		}
	}

	@Override
	public void stop() {
		Log.i(LOG_TAG, "stop");

		Log.d(LOG_TAG, "Close netty connection to " + mIpAddress);

		Channel channel = channelRef.get();
		if (channel == null) {
			Log.i(LOG_TAG, "Channel {} has been closed before" + mIpAddress);
			return;
		}

		if (channel.isWritable()) {

			// TODO: flush all request
		}

		close_n_release();
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
		// Shut down thread pools to exit.
		if (null != bootstrap) {
			bootstrap.releaseExternalResources();
			bootstrap = null;
		}

		mIsRunning.set(false);
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
		// Log.i(LOG_TAG, "send");
		if (isClosed()) {
			Log.w(LOG_TAG,
					"Client is being closed, and does not take requests any more");
			broadcastNetStatus2Listener(MIRROR_NETSTATUS.NET_DISCONNECT,
					"Client is being closed, and does not take requests any more");
			return MIRROR_NETSTATUS.SEND_FAIL;
		}

		if (null != this.channelRef.get()) {
			if (this.channelRef.get().isWritable()) {
				this.channelRef.get().write(req);

				return MIRROR_NETSTATUS.SEND_SUCC;
			} else {
				// Log.d(LOG_TAG, "current not writable");
				return MIRROR_NETSTATUS.SEND_AGAIN;
			}
		} else {
			broadcastNetStatus2Listener(MIRROR_NETSTATUS.SEND_FAIL,
					"channelRef is null");			
			return MIRROR_NETSTATUS.SEND_FAIL;
		}
	}

	public MIRROR_NETSTATUS sendFile(final String filePath) {
		Log.i(LOG_TAG, "sendFile");
		if (isClosed()) {
			Log.w(LOG_TAG,
					"Client is being closed, and does not take requests any more");
			return MIRROR_NETSTATUS.SEND_FAIL;
		}
		if (null != this.channelRef.get()) {
			if (this.channelRef.get().isWritable()) {
				//
				File file = new File(filePath);
				if (file.isHidden() || !file.exists()) {
					Log.d(LOG_TAG, filePath + " is hidden or not exists");
					return MIRROR_NETSTATUS.SEND_FAIL;
				}
				if (!file.isFile()) {
					Log.d(LOG_TAG, filePath + "is not file");
					return MIRROR_NETSTATUS.SEND_FAIL;
				}
				final RandomAccessFile raf;
				try {
					raf = new RandomAccessFile(file, "r");
				} catch (FileNotFoundException fnfe) {
					fnfe.printStackTrace();
					return MIRROR_NETSTATUS.SEND_FAIL;
				}
				long fileLength = 0;
				try {
					fileLength = raf.length();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				final FileRegion region = new DefaultFileRegion(
						raf.getChannel(), 0, fileLength);
				ChannelFuture writeFuture;
				writeFuture = this.channelRef.get().write(region);
				writeFuture.addListener(new ChannelFutureProgressListener() {
					public void operationComplete(ChannelFuture future) {
						Log.d(LOG_TAG, "transf file over");
						region.releaseExternalResources();
						try {
							raf.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					public void operationProgressed(ChannelFuture future,
							long amount, long current, long total) {
						System.out.printf("%s: %d / %d (+%d)%n", filePath,
								current, total, amount);
					}
				});

				return MIRROR_NETSTATUS.SEND_SUCC;
			} else {
				// Log.d(LOG_TAG, "current not writable");
				return MIRROR_NETSTATUS.SEND_AGAIN;
			}
		} else
			return MIRROR_NETSTATUS.SEND_FAIL;
	}

	private class ClientHandler extends SimpleChannelHandler {

		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {

			// Log all channel state changes.
			if (e instanceof ChannelStateEvent) {
				Log.d(LOG_TAG, "Channel state changed: " + e);
				Log.d(LOG_TAG, "state: " + ((ChannelStateEvent) e).getState());
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
				Log.d(LOG_TAG, message);
			}

			if (e.getMessage() instanceof MirrorMessage) {
				MirrorMessage msg = (MirrorMessage) e.getMessage();
				Log.d(LOG_TAG,
						"svr " + msg.getAppType() + " " + msg.getPeerAddress()
								+ " " + msg.getContent());
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

			Log.i(LOG_TAG, "exceptionCaught " + e.getCause().toString());
			broadcastNetStatus2Listener(MIRROR_NETSTATUS.NET_EXCEPTION, e
					.getCause().toString());
			super.exceptionCaught(ctx, e);
		}

		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {

			super.channelConnected(ctx, e);

			// sayHello();
		}

		@Override
		public void channelDisconnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {

			Log.i(LOG_TAG,
					"Receive channelDisconnected, channel = " + e.getChannel());

			super.channelDisconnected(ctx, e);

			disconnectChannel(e.getChannel());

		}

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
