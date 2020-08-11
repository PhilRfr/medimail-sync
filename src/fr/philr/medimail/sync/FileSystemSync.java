package fr.philr.medimail.sync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map.Entry;

import fr.philr.medimail.api.MedimailMessage;
import fr.philr.medimail.api.MedimailSession;

public class FileSystemSync {

	public static void sync(String folderName, MedimailSession session) {
		Path folder = Paths.get(folderName).toAbsolutePath();
		Path folderNew = Paths.get(folder.toString(), "new");
		Path folderRead = Paths.get(folder.toString(), "read");
		folder.toFile().mkdirs();
		folderNew.toFile().mkdirs();
		folderRead.toFile().mkdirs();
		session.fetchMessageHeaders();
		HashMap<Path, MedimailMessage> toDownload = new HashMap<Path, MedimailMessage>();
		for (MedimailMessage message : session.getMessages()) {
			Path fullname = Paths.get(folderNew.toAbsolutePath().toString(), message.getFileName());
			Path fullnameRead = Paths.get(folderRead.toAbsolutePath().toString(), message.getFileName());
			if (!(Files.exists(fullname) || Files.exists(fullnameRead)))
				toDownload.put(fullname, message);
		}
		int total = toDownload.size();
		int current = 1;
		for (Entry<Path, MedimailMessage> pair : toDownload.entrySet()) {
			MedimailMessage message = pair.getValue();
			Path fullname = pair.getKey();
			System.out
					.println("[" + (current++) + "/" + total + "] Downloading " + message + " to " + fullname + "...");
			message.download(session, fullname);
		}
	}

}
