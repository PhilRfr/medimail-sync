package fr.philr.medimail.pop3access;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class ProtocolSession {

	private AsynchronousSocketChannel channel;

	final protected String name;

	protected boolean running;

	public ProtocolSession(AsynchronousSocketChannel ch, String name) {
		this.channel = ch;
		this.name = name;
	}

	abstract protected String greet();

	protected void send(String what) {
		// Echo back to the caller
		channel.write(ByteBuffer.wrap(what.getBytes()));
	}

	private String doCmd(String in){
		//System.out.println("IN " + name + " : " + clean);
		String reply = readCommand(in);
		return reply;
	}

	public void initiate() {
		System.out.println("Initiating new " + name + " connection.");
		send(greet());
		run();
	}

	private void run() {
		// Allocate a byte buffer (4K) to read from the client
		ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
		try {
			byteBuffer.flip();
			int bytesRead = channel.read(byteBuffer).get(20, TimeUnit.SECONDS);
			running = true;
			while (bytesRead != -1 && running) {
				// Make the buffer ready to read
				byteBuffer.flip();
				// Convert the buffer into a line
				byte[] lineBytes = new byte[bytesRead];
				byteBuffer.get(lineBytes, 0, bytesRead);
				String data = new String(lineBytes);
				String[] lines = data.split("\n");
				StringBuilder sb = new StringBuilder();
				byteBuffer.flip();
				for (String line : lines) {
					String reply = doCmd(line);
					if (reply != null)
						sb.append(reply).append("\r\n");
				}
				String reply = sb.toString();
				// Make the buffer ready to write
				if(!running)
					break;
				byteBuffer.clear();
				send(reply);
				// Read the next line
				bytesRead = channel.read(byteBuffer).get(20, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			System.out.println("Connection " + name + " timed out, closing connection");
		}
		System.out.println("Connection " + name + " closed.");
	}

	abstract protected String readCommand(String incoming);
}
