package fr.philr.medimail.pop3access;

import java.net.Socket;

public interface POP3ServerFactory {

	public void handleConnection(Socket clientSocket);

}
