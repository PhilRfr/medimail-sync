package fr.philr.medimail.pop3access;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map.Entry;

public class ProtocolSessionRegister {
	HashMap<Integer, AsynchronousServerSocketChannel> ports;
	
	public ProtocolSessionRegister() {
		ports = new HashMap<Integer, AsynchronousServerSocketChannel>();
	}

	public void register(int port, final ProtocolSessionFactory factory) {
		try {
			AsynchronousServerSocketChannel templistener = null;
			if(ports.containsKey(port)) {
				templistener = ports.get(port);
			}else {
				templistener = AsynchronousServerSocketChannel.open()
						.bind(new InetSocketAddress(port));
				ports.put(port, templistener);
			}
			final AsynchronousServerSocketChannel listener = templistener;
			
			listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
				public void completed(AsynchronousSocketChannel ch, Void att) {
					// Accept the next connection
					listener.accept(null, this);
					ProtocolSession session = factory.newSession(ch);
					session.initiate();
					try {
						ch.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				public void failed(Throwable exc, Void att) {
					/// ...
				}
			});
			System.out.println("Registered new factory for port " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void cancelAll() {
		for(Entry<Integer, AsynchronousServerSocketChannel> entry:ports.entrySet()) {
			try {
				entry.getValue().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
