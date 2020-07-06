package ru.ancevt.net.messaging.server;

import ru.ancevt.net.messaging.MessagingConnection;

/**
 *
 * @author ancevt
 */
public class MessagingServerAdapter implements MessagingServerListener {

    @Override
    public void messagingServerStarted() {
    }

    @Override
    public void acceptMessagingConnection(MessagingConnection connection) {
    }

    @Override
    public void closeMessagingConnection(MessagingConnection connection, Throwable exception) {
    }

    @Override
    public void serverShutdown() {
    }
    
    
    
}
