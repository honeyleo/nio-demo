package server;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadServer {
	
    private ExecutorService ioPool;
    private ServerSocketChannel ssc;
    private Selector selector;
    private int n;

    public static void main(String[] args) throws IOException {
        MultiThreadServer server = new MultiThreadServer(60000, 2);
        server.start();
    }

    public MultiThreadServer(int port, int ioThread) throws IOException {
    	
    	ioPool = Executors.newFixedThreadPool(ioThread);

        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ServerSocket ss = ssc.socket();
        ss.bind(new InetSocketAddress(port));
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started listen " + port + "...");
    }

    public void start() {
        while (true) {
            try {
                n = selector.select();
            } catch (IOException e) {
                throw new RuntimeException("Selector.select()异常!");
            }
            if (n == 0) {
            	continue;
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = null;
                    try {
                        sc = ((ServerSocketChannel) key.channel()).accept();
                        sc.configureBlocking(false);
                        System.out.println("客户端:"
                                + sc.socket().getInetAddress().getHostAddress()+"端口"+sc.socket().getPort()
                                + " 已连接");
                        sc.register(selector,
                                SelectionKey.OP_READ);
                    } catch (Exception e) {
                    	System.err.println(e);
                        try {
                            sc.close();
                        } catch (Exception ex) {
                        	System.err.println(ex);
                        }
                    }
                } else if (key.isReadable()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                    ioPool.execute(new Reader(key));
                }
            }
        }
    }

    public static byte[] _read(SocketChannel channel, int length) throws IOException {
    	
		int nrecvd = 0;
		byte[] data = new byte[length];
		ByteBuffer buffer = ByteBuffer.wrap(data);
		try {
			while (nrecvd < length) {
				long n = channel.read(buffer);
				if (n < 0) {
					throw new EOFException();
				}
				nrecvd += (int) n;
			}
		} finally {
			
		}
		return data;
	}
    public static class Reader implements Runnable {
    	
        private SelectionKey key;

        public Reader(SelectionKey key) {
            this.key = key;
        }

        @Override
        public void run() {
            SocketChannel sc = (SocketChannel) key.channel();
            try {
            	//读两个字节包大小
        		byte[] lenData = _read(sc, 2);
        		int length = ((lenData[0] & 0xFF) << 8) + (lenData[1] & 0xFF);
        		
        		byte[] data = _read(sc, length);
        		ByteBuffer dataBuf = ByteBuffer.wrap(data);
        		int cmd = dataBuf.getShort();
        		System.out.println("[length=" + length + ",cmd=" + cmd + "]");
        		ByteBuffer respBuf = ByteBuffer.allocate(4);
        		respBuf.putShort((short)length);
        		respBuf.putShort((short)cmd);
        		respBuf.flip();
        		sc.write(respBuf);
                //没有可用字节,继续监听OP_READ
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                key.selector().wakeup();
            } catch (Exception e) {
            	System.err.println(e);
                try {
                    sc.close();
                } catch (IOException e1) {
                	System.err.println(e1);
                }
            }
        }
    }
}
