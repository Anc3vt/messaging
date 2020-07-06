package ru.ancevt.net.messaging.server;

import ru.ancevt.net.messaging.MessagingConnection;

/**
 *
 * @author ancevt
 */
public interface MessagingServerListener {
    void messagingServerStarted();
    void acceptMessagingConnection(MessagingConnection connection);
    void closeMessagingConnection(MessagingConnection connection, Throwable ex);
    void serverShutdown();
}
