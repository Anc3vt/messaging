package ru.ancevt.net.messaging.client;

import ru.ancevt.net.messaging.MessagingConnection;

/**
 * @author ancevt
 */
public interface MessagingClientListener {
    void clientMessagingConnectionOpened(MessagingConnection connection);
    void clientMessagingConnectionClosed(MessagingConnection connection, Throwable exception);
    void clientMessagingConnectionError(Throwable exception);
}
