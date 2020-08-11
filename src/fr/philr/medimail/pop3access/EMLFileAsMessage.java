package fr.philr.medimail.pop3access;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedMap;

public class EMLFileAsMessage implements MessageInterface {

	final String content;
	final Path originalFileName;
	final Path fileAfterRead;
	private Charset findBestCharset(String fileName) {
	    SortedMap<String, Charset> charsets = Charset.availableCharsets();
	    for (String k : charsets.keySet()) {
	        int line = 0;
	        boolean success = true;
	        try (BufferedReader b = Files.newBufferedReader(Paths.get(fileName),charsets.get(k))) {
	            while (b.ready()) {
	                b.readLine();
	                line++;
	            }
	        } catch (IOException e) {
	            success = false;
	            System.out.println(k+" failed on line "+line);
	        }
	        if (success)
	        	return charsets.get(k);
	    }
	    return StandardCharsets.UTF_8;
	}
	public EMLFileAsMessage(Path originalFileName, Path folderAfterRead) throws IOException {
		Charset charset = findBestCharset(originalFileName.toString());
        content = Files.readString(originalFileName, charset);
		this.originalFileName = originalFileName;
		this.fileAfterRead = Paths.get(folderAfterRead.toString(), originalFileName.toFile().getName().toString());
	}
	
	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return content.length();
	}

	@Override
	public String asString() {
		// TODO Auto-generated method stub
		return content;
	}
	
	public void delete() {
		try {
			Files.move(originalFileName, fileAfterRead);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
