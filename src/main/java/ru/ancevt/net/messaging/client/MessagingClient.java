package ru.ancevt.net.messaging.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ru.ancevt.net.messaging.MessagingConnection;
import ru.ancevt.net.messaging.MessagingConnectionListener;
import ru.ancevt.net.messaging.message.MessageData;

/**
 *
 * @author ancevt
 */
public class MessagingClient implements MessagingConnectionListener {

    private final List<MessagingClientListener> listeners;
    private MessagingConnection connection;
    private Throwable exception;

    public MessagingClient() {
        listeners = new CopyOnWriteArrayList<>();
    }

    public void addMessagingClientListener(MessagingClientListener listener) {
        listeners.add(listener);
    }

    public void removeMessagingClientListener(MessagingClientListener listener) {
        listeners.remove(listener);
    }

    public void dispatchClientMessagingConnectionOpened(MessagingConnection connection) {
        listeners.stream().forEach(l -> l.clientMessagingConnectionOpened(connection));
    }

    public void dispatchClientMessagingConnectionClosed(MessagingConnection connection, Throwable ex) {
        listeners.stream().forEach(l -> l.clientMessagingConnectionClosed(connection, ex));
    }

    public void dispatchClientMessagingConnectionError(Throwable ex) {
        listeners.stream().forEach(l -> l.clientMessagingConnectionError(ex));
    }

    public void connect(String host, int port) throws IOException {
        if (connection != null && connection.isOpened()) {
            throw new IOException("connection is already opened");
        }

        try {
            connection = new MessagingConnection(host, port);
            connection.addMessagingConnectionListener(this);
            connection.start();
        } catch (IOException ex) {
            dispatchClientMessagingConnectionError(exception = ex);
        }
    }

    public Throwable getException() {
        return exception;
    }

    public MessagingConnection getConnection() {
        return connection;
    }

    @Override
    public void connectionOpened(MessagingConnection connection) {
        dispatchClientMessagingConnectionOpened(connection);
    }

    @Override
    public void incomingMessageData(MessagingConnection connection, MessageData messageData) {

    }

    @Override
    public void connectionClosed(MessagingConnection connection, Throwable exception) {
        dispatchClientMessagingConnectionClosed(connection, exception);
    }

}
