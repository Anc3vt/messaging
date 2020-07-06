package ru.ancevt.net.messaging.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ru.ancevt.net.messaging.Log;
import ru.ancevt.net.messaging.MessagingConnection;
import ru.ancevt.net.messaging.MessagingConnectionListener;
import ru.ancevt.net.messaging.message.MessageData;
import ru.ancevt.net.messaging.message.UTF8Message;
import ru.ancevt.util.args.Args;
import ru.ancevt.util.string.ToStringBuilder;
import ru.ancevt.util.system.UnixDisplay;

/**
 * @author ancevt
 */
public class MessagingServer implements Closeable {

    public static void main(String[] args) throws IOException {
        final MessagingServer ms = new MessagingServer();
        ms.addMessagingServerListener(new MessagingServerListener() {

            @Override
            public void acceptMessagingConnection(MessagingConnection connection) {
                connection.addMessagingConnectionListener(new MessagingConnectionListener() {

                    @Override
                    public void connectionClosed(MessagingConnection connection, Throwable ex) {
                        Log.logger.info(">> Server: connection closed " + connection + ", " + ex);
                    }

                    @Override
                    public void incomingMessageData(MessagingConnection connection, MessageData messageData) {
                        Log.logger.info(UnixDisplay.GREEN + ">> Server: incoming message: " + new UTF8Message(messageData) + UnixDisplay.RESET);
                        try {
                            Log.logger.info(">> Server: incoming message: " + new UTF8Message(messageData));
                            final Args a = new Args(new UTF8Message(messageData).getText());

                            final String commandWord = a.getString(0).trim();

                            switch (commandWord) {
                                case "exit":
                                    ms.close();
                                    ms.shutdownAllConnections();
                                    break;
                                case "echo":
                                    connection.send(new UTF8Message(a.getString(1)));
                                    break;
                                case "test":
                                    connection.send(new UTF8Message("This is a test"));
                                    break;
                                case "large":
                                    connection.send(new UTF8Message(generateLargeText(65536)));
                                    break;
                            }

                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Log.logger.error(ex);
                        }
                    }

                    @Override
                    public void connectionOpened(MessagingConnection connection) {
                    }

                });

            }

            @Override
            public void closeMessagingConnection(MessagingConnection connection, Throwable ex) {
            }

            @Override
            public void messagingServerStarted() {
            }

            @Override
            public void serverShutdown() {
            }
        });

        new Thread(() -> {
            try {
                ms.start(7777);
            } catch (IOException ex) {
                Log.logger.error(ex);
            }
        }).start();

        

    }

    private final List<MessagingServerListener> listeners;
    private final List<MessagingConnection> connections;

    private String host;
    private int port;
    private ServerSocket serverSocket;
    private boolean started;
    private boolean shutdownSignal;
    private Thread acceptThread;

    public MessagingServer() {
        listeners = new CopyOnWriteArrayList<>();
        connections = new CopyOnWriteArrayList<>();
    }

    public final int getConnectionCount() {
        return connections.size();
    }

    public final MessagingConnection getConnection(int index) {
        return connections.get(index);
    }

    public final void addMessagingServerListener(MessagingServerListener listener) {
        listeners.add(listener);
    }

    public final void removeMessagingServerListener(MessagingServerListener listener) {
        listeners.remove(listener);
    }

    public void dispatchAcceptMessagingConnection(MessagingConnection connection) {
        connections.add(connection);
        listeners.stream().forEach((MessagingServerListener l) -> l.acceptMessagingConnection(connection));
    }

    public void dispatchServerShutdown() {
        listeners.stream().forEach((MessagingServerListener l) -> l.serverShutdown());
    }

    public void dispatchCloseMessagingConnection(MessagingConnection connection, Throwable exception) {
        connections.remove(connection);
        Log.logger.info("Server: close connection " + connection.toString() + ", " + exception);
        listeners.stream().forEach((MessagingServerListener l) -> l.closeMessagingConnection(connection, exception));
        if (isNoConnections() && shutdownSignal) {
            try {
                if (started) {
                    close();
                    Log.logger.info("Messaging server shutdown");
                    dispatchServerShutdown();
                }
            } catch (IOException ex) {
                Log.err(ex, ex);
            }
        }
    }

    public void dispatchMessagingServerStarted() {
        Log.logger.info("Starting server at port " + port);
        listeners.stream().forEach((MessagingServerListener l) -> l.messagingServerStarted());
    }
    
    public final void start(String host, int port) throws IOException {
        if (started) {
            throw new IOException("Server is already started " + this.toString());
        }
        this.host = host;
        this.port = port;
        serverSocket = new ServerSocket(port, 0, InetAddress.getByName(host));

        acceptThread = new Thread(() -> {
            while (!shutdownSignal) {
                try {
                    final Socket socket = serverSocket.accept();

                    final MessagingConnection connection = new MessagingConnection(socket, this);
                    dispatchAcceptMessagingConnection(connection);
                    connection.start();

                    Log.logger.info("Server: accept connection " + connection.toString());

                } catch (IOException ex) {
                    if (ex instanceof SocketException && ex.getMessage().equals("Socket closed")) {
                        Log.logger.info("Server socket closed " + this);
                    } else {
                        Log.err(ex, ex);
                    }
                }
            }
        }, "MessagingServer-accept-loop");
        acceptThread.start();

        started = true;

        dispatchMessagingServerStarted();
    }

    public final void start(int port) throws IOException {
        start("0.0.0.0", port);
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void shutdown() throws IOException {
        if (shutdownSignal) {
            throw new IOException("Server is already shut down " + this.toString());
        }

        this.shutdownSignal = true;

        for (int i = 0; i < connections.size(); i++) {
            final MessagingConnection connection = connections.get(i);
            connection.shutdown();
        }

        acceptThread.interrupt();

        if (isNoConnections()) {
            try {
                if (started) {
                    close();
                    Log.logger.info("Messaging server shutdown (no connections)");
                    dispatchServerShutdown();
                }
            } catch (IOException ex) {
                Log.err(ex, ex);
            }
        }
    }

    public boolean isNoConnections() {
        return connections.isEmpty();
    }

    @Override
    public void close() throws IOException {
        if (!started) {
            throw new IOException("Server is already closed" + this.toString());
        }
        started = false;
        serverSocket.close();
    }

    public void shutdownAllConnections() {
        
        for (int i = 0; i < connections.size(); i++) {
            final MessagingConnection connection = connections.get(i);
            connection.shutdown();
        }
    }

    public static String generateLargeText(int size) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(i).append(' ');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("port")
            .append("connectionCount")
            .build();
    }

}
