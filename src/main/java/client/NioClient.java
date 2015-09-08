package client;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NioClient {

	private Selector selector;
	private SocketChannel channel;

	public void connect(String host, int port) throws IOException {
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(host, port));

		selector = Selector.open();
		channel.register(selector, SelectionKey.OP_CONNECT);
	}

	public void process() throws IOException {
		while (true) {
			int keyCount = selector.select();
			if (keyCount <= 0) {
				continue;
			}
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIt = readyKeys.iterator();
			while (keyIt.hasNext()) {
				SelectionKey key = keyIt.next();
				keyIt.remove();
				if (key.isConnectable()) {
					SocketChannel channel = (SocketChannel) key.channel();
					if (channel.isConnectionPending()) {
						channel.finishConnect();
					}
					channel.configureBlocking(false);
					break;
				} else if (key.isReadable()) {
					SocketChannel channel = (SocketChannel) key.channel();
					this.read(channel);
				}
			}
		}
	}

	public void heartBeat(SocketChannel channel) {

	}

	private byte[] _read(SocketChannel channel, int length) throws IOException {
		int nrecvd = 0;
		byte[] data = new byte[length];
		ByteBuffer buffer = ByteBuffer.wrap(data);
		try {
			while (nrecvd < length) {
				long n = channel.read(buffer);
				if (n < 0)
					throw new EOFException();
				nrecvd += (int) n;
			}
		} finally {

		}
		return data;
	}

	public void read(SocketChannel channel) throws IOException {
		byte[] buf = _read(channel, 2);
		int length = ((buf[0] & 0xFF) << 8) + (buf[1] & 0xFF);
		byte[] recvData = _read(channel, length);
		ByteBuffer buffer = ByteBuffer.wrap(recvData);
		int cmd = buffer.getShort();
		System.out.println("msg[length=" + length + ",cmd=" + cmd + "]");
	}

	public void write(byte[] bytes) throws IOException {
		ByteBuffer requestBuffer = ByteBuffer.wrap(bytes);
		while (requestBuffer.hasRemaining()) {
			channel.write(requestBuffer);
		}
		channel.register(selector, SelectionKey.OP_READ);

	}
	
	public void write(ByteBuffer buf) throws IOException {
		while (buf.hasRemaining()) {
			channel.write(buf);
		}
		channel.register(selector, SelectionKey.OP_READ);

	}
	static NioClient client = null;
	public static void main(String[] args) {
		try {
			ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
			scheduled.scheduleWithFixedDelay(new Runnable() {
				
				@Override
				public void run() {
					try {
						ByteBuffer buf = ByteBuffer.allocate(4);
						buf.putShort((short)2);
						buf.putShort((short)1);
						buf.flip();
						client.write(buf);
					} catch(Exception e) {
						System.err.println(e);
					}
				}
			}, 10, 10, TimeUnit.SECONDS);
			client = new NioClient();
			client.connect("127.0.0.1", 60000);
			client.process();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}