package fr.philr.medimail.pop3access;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class POP3Server {

	protected int serverPort = 8080;
	protected ServerSocket serverSocket = null;
	protected boolean isStopped = false;
	protected Thread runningThread = null;
	protected POP3ServerFactory factory;
	
	public POP3Server(int port, POP3ServerFactory fact) {
		this.serverPort = port;
		factory = fact;
	}

	public void run() {
		synchronized (this) {
			this.runningThread = Thread.currentThread();
		}
		openServerSocket();

		while (!isStopped()) {
			Socket clientSocket = null;
			try {
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				if (isStopped()) {
					System.out.println("Server Stopped.");
					return;
				}
				throw new RuntimeException("Error accepting client connection", e);
			}
			try {
				processClientRequest(clientSocket);
			} catch (Exception e) {
				// log exception and go on to next request.
			}
		}

		System.out.println("Server Stopped.");
	}

	private void processClientRequest(Socket clientSocket) throws Exception {
		factory.handleConnection(clientSocket);
	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void stop() {
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	private void openServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port 8080", e);
		}
	}
}
