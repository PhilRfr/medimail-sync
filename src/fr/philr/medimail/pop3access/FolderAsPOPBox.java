package fr.philr.medimail.pop3access;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FolderAsPOPBox implements POP3MessageBoxInterface {

	private final String authUser;
	private final String authPW;
	private final ArrayList<MessageInterface> messages;

	private ArrayList<MessageInterface> buildMessageList(Path folderBeforeRead, Path folderAfterRead) {
		ArrayList<MessageInterface> messages = new ArrayList<MessageInterface>();
		try {
			Files.walk(folderBeforeRead).filter(Files::isRegularFile).forEach(new Consumer<Object>() {

				@Override
				public void accept(Object name) {
					String fileName = name.toString();
					if (fileName.endsWith(".eml")) {
						try {
							MessageInterface msg = new EMLFileAsMessage(Paths.get(fileName), folderAfterRead);
							messages.add(msg);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return messages;
	}

	public FolderAsPOPBox(String authUser, String authPW, Path newMessageFolder, Path readMessageFolder) {
		this.authUser = authUser;
		this.authPW = authPW;
		this.messages = buildMessageList(newMessageFolder, readMessageFolder);
	}

	@Override
	public List<MessageInterface> getMessages() {
		// TODO Auto-generated method stub
		return messages;
	}

	@Override
	public boolean checkUser(String user) {
		// TODO Auto-generated method stub
		return authUser.equals(user);
	}

	@Override
	public boolean checkUserPass(String user, String pass) {
		// TODO Auto-generated method stub
		return checkUser(user) && authPW.equals(pass);
	}

	@Override
	public void delete(int msgid) {
		messages.get(msgid - 1).delete();
	}

}
