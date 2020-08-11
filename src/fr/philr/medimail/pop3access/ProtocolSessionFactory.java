package fr.philr.medimail.pop3access;

import java.nio.channels.AsynchronousSocketChannel;

public interface ProtocolSessionFactory {
	ProtocolSession newSession(AsynchronousSocketChannel ch);
}
