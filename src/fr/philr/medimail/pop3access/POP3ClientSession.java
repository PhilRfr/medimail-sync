package fr.philr.medimail.pop3access;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class POP3ClientSession implements Runnable {

	enum POP3State {
		AUTHORIZATION, TRANSACTION
	}

	String lastUser = "";

	POP3MessageBoxInterface messageBox = null;

	public void attachBox(POP3MessageBoxInterface box) {
		messageBox = box;
	}

	protected String greet() {
		// TODO Auto-generated method stub
		return "+OK POP3 server ready";
	}

	protected String readCommand(String incoming) {
		String[] args = incoming.strip().split(" ", 2);
		String command = args[0];
		if (command.equalsIgnoreCase("capa")) {
			return CAPA();
		} else if (command.equalsIgnoreCase("user") && args.length == 2) {
			return USER(args[1]);
		} else if (command.equalsIgnoreCase("pass") && args.length == 2) {
			return PASS(args[1]);
		} else if (command.equalsIgnoreCase("dele") && args.length == 2) {
			int i = Integer.parseInt(args[1]);
			return DELE(i);
		} else if (command.equalsIgnoreCase("retr") && args.length == 2) {
			int i = Integer.parseInt(args[1]);
			return RETR(i);
		} else if (command.equalsIgnoreCase("quit")) {
			return QUIT();
		} else if (command.equalsIgnoreCase("stat")) {
			return STAT();
		} else if (command.equalsIgnoreCase("list")) {
			return LIST();
		}
		return "-err";
	}

	private String CAPA() {
		return "+OK methods supported\r\nTOP\r\nUSER\r\n.";
	}

	private String USER(String user) {
		lastUser = user;
		boolean ret = messageBox != null && messageBox.checkUser(user);
		return ret ? "+OK" : "+ERR Invalid user";
	}

	private String PASS(String pass) {
		boolean ret = messageBox != null && messageBox.checkUserPass(lastUser, pass);
		return ret ? "+OK" : "+ERR Invalid credentials";
	}

	private String RETR(int msgid) {
		String ret = "-ERR";
		if (messageBox != null) {
			MessageInterface msg = messageBox.getMessages().get(msgid - 1);
			StringBuilder sb = new StringBuilder("+OK ").append(msg.getSize()).append(" octets");
			sb.append("\r\n");
			sb.append(msg.asString());
			sb.append("\r\n.");
			ret = sb.toString();
		}
		return ret;
	}

	private String DELE(int msgid) {
		if (messageBox != null) {
			messageBox.delete(msgid);
		}
		return "+OK Marked to be deleted.";
	}

	private String QUIT() {
		return "+OK Logging out";
	}

	private String LIST() {
		StringBuilder sb = new StringBuilder("+OK \r\n");
		if (messageBox != null && messageBox.getMessages().size() > 0) {
			int i = 1;
			for (MessageInterface message : messageBox.getMessages()) {
				sb.append(i++).append(' ').append(message.getSize()).append("\r\n");
			}
		}
		sb.append('.');
		return sb.toString();
	}

	private String STAT() {
		String ret = "+OK ";
		if (messageBox != null) {
			List<MessageInterface> msgs = messageBox.getMessages();
			int total = 0;
			for (MessageInterface msg : msgs)
				total += msg.getSize();
			ret += msgs.size() + " " + total;
		} else
			ret += " 0 0";
		return ret;
	}

	private Socket socket;

	private PrintWriter outFS;
	private Thread thread;
	private BufferedReader inFS;

	public POP3ClientSession(Socket s) {
		this.socket = s;
		try {
			this.outFS = new PrintWriter(s.getOutputStream(), true);
			this.inFS = new BufferedReader(new InputStreamReader(s.getInputStream()));
			thread = new Thread(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void launch() {
		thread.start();
	}

	@Override
	public void run() {
		System.out.println("New client connection.");
		try {
			String userInput = "";
			outFS.print(greet()+"\r\n");
			outFS.flush();
			char cur[] = new char[1];
			while (inFS.read(cur, 0, 1) != -1) {
				userInput += cur[0];
				if (userInput.endsWith("\n")) {
					System.out.println(userInput);
					String reply = readCommand(userInput);
					outFS.print(reply + "\r\n");
					outFS.flush();
					userInput = "";
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				System.out.println("Client closed.");
				socket.close();
			} catch (IOException e) {
			}
		}
	}
}
