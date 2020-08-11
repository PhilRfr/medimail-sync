package fr.philr.medimail.pop3access;

import java.util.List;

public interface POP3MessageBoxInterface {
	List<MessageInterface> getMessages();
	boolean checkUser(String user);
	boolean checkUserPass(String user, String pass);
	void delete(int msgid);
}
