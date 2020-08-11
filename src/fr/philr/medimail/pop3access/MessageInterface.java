package fr.philr.medimail.pop3access;

public interface MessageInterface {
	int getSize();
	String asString();
	void delete();
}
