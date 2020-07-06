package ru.ancevt.net.messaging;

import ru.ancevt.net.messaging.message.MessageData;

/**
 *
 * @author ancevt
 */
public interface MessagingConnectionListener {
    void connectionOpened(MessagingConnection connection);
    void incomingMessageData(MessagingConnection connection, MessageData messageData);
    void connectionClosed(MessagingConnection connection, Throwable ex);
}
