package ru.ancevt.net.messaging;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ru.ancevt.net.messaging.message.Message;
import static ru.ancevt.net.messaging.message.Message.SIGNATURE;
import ru.ancevt.net.messaging.message.MessageData;
import ru.ancevt.net.messaging.message.MessagingException;
import ru.ancevt.net.messaging.server.MessagingServer;
import ru.ancevt.util.string.ToStringBuilder;
import ru.ancevt.util.system.UnixDisplay;

/**
 *
 * @author ancevt
 */
public class MessagingConnection extends Thread implements Closeable {

    public static final int DEFAULT_CHUNK_SIZE = 1024;

    private static int idCounter;

    private final List<MessagingConnectionListener> listeners;
    private final Socket socket;
    private MessagingServer server;
    private boolean opened;
    private int chunkSize;
    private MessageData messageData;
    private long bytesSent, bytesReceived;
    private boolean shutdownSignal;

    public MessagingConnection(Socket socket) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.socket = socket;
        chunkSize = DEFAULT_CHUNK_SIZE;
        setName("messConnection-" + (++idCounter));
    }

    public MessagingConnection(Socket socket, MessagingServer server) {
        this(socket);
        this.server = server;
    }

    public MessagingConnection(String host, int port) throws IOException {
        this(new Socket(host, port));
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void addMessagingConnectionListener(MessagingConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeMessagingConnectionListener(MessagingConnectionListener listener) {
        listeners.remove(listener);
    }

    private void dispatchIncomingMessageData(MessageData messageData) {
        if (server != null) {
            Log.logger.info("(S)Incoming messageData: " + messageData.toString());
        } else {
            Log.logger.info("Incoming messageData: " + messageData.toString());
        }
        try {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).incomingMessageData(this, messageData);
            }
        } catch (IndexOutOfBoundsException ex) {
            ex.printStackTrace();
        }
    }

    private void dispatchConnectionClosed(Throwable exception) {
        Log.logger.info("Connection closed: " + this);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).connectionClosed(this, exception);
        }
    }

    private void dispatchConnectionOpened() {
        if (server != null) {
            Log.logger.info("(S)Connection opened: " + this);
        } else {
            Log.logger.info("Connection opened: " + this);
        }

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).connectionOpened(this);
        }
    }

    private void sendMessageData(MessageData messageData) throws IOException {
        final OutputStream outputStream = socket.getOutputStream();

        outputStream.write(messageData.getBytes());
        outputStream.flush();
        bytesSent += messageData.getBytes().length;
    }

    public final void send(final Message message) throws IOException {
        message.prepare();

        if (!opened) {
            if (server != null) {
                throw new IOException("(S)attempt to send message via closed connection " + this.toString() + " " + message);
            } else {
                throw new IOException("attempt to send message via closed connection " + this.toString() + " " + message);
            }
        }

        if (server != null) {
            Log.logger.info(UnixDisplay.CYAN + "(S)Send message: " + message.toString() + UnixDisplay.RESET);
        } else {
            Log.logger.info(UnixDisplay.CYAN + "Send message: " + message.toString() + UnixDisplay.RESET);
        }

        sendMessageData(message.getMessageData());
    }

    public boolean isOpened() {
        return opened;
    }

    @Override
    public void run() {
        opened = true;
        dispatchConnectionOpened();

        try {

            final InputStream inputStream = socket.getInputStream();
            final DataInputStream dataInputStream = new DataInputStream(inputStream);
            /*
            
             1 b - sign
             +   4 b - size
             +   4 b - reqId
             ------------
             9
            
             */

            while (opened && !shutdownSignal) {

                synchronized (this) {

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
                        final int sign = dataInputStream.readUnsignedByte();

                        if (sign != SIGNATURE) {
                            if (server != null) {
                                throw new MessagingException("(S)Invalid message signature 0x" + Integer.toString(sign, 16));
                            } else {
                                throw new MessagingException("Invalid message signature 0x" + Integer.toString(sign, 16));
                            }
                        }

                        final int messageSize = dataInputStream.readInt();
                        final int requestId = dataInputStream.readInt();

                        int currentSize = Message.HEADERS_SIZE;

                        while (currentSize < messageSize) {
                            if (inputStream.available() == 0) {
                                continue;
                            }

                            int arraySize = Math.min(chunkSize, inputStream.available());
                            arraySize = Math.min(messageSize - Message.HEADERS_SIZE, arraySize);

                            final byte[] bytes = new byte[arraySize];
                            inputStream.read(bytes);
                            baos.write(bytes);

                            currentSize += bytes.length;

                            if (currentSize >= messageSize) {
                                break;
                            }
                        }

                        final byte[] bytesToSend = baos.toByteArray();

                        messageData = new MessageData();
                        messageData.setBytes(requestId, bytesToSend);

                        bytesReceived += messageData.getBytes().length;

                        if (shutdownSignal) {
                            close();
                        }

                        dispatchIncomingMessageData(messageData);
                    }
                }
            }
        } catch (IOException ex) {
            if (ex instanceof EOFException) {
                if (server != null) {
                    Log.logger.info("(S)Connection " + this + " closed by peer");
                } else {
                    Log.logger.info("Connection " + this + " closed by peer");
                }

                dispatchConnectionClosed(ex);
                try {
                    socket.close();
                } catch (IOException ex1) {
                    Log.err(ex1, ex1);
                }
                if (server != null) {
                    server.dispatchCloseMessagingConnection(this, ex);
                }
            } else {
                if (!(ex instanceof SocketException)) {
                    Log.logger.error(ex);
                }
                dispatchConnectionClosed(ex);
            }
        } catch (MessagingException ex) {
            dispatchConnectionClosed(ex);
            try {
                socket.close();
            } catch (IOException ex1) {
                Log.err(ex1, ex1);
            }
            if (server != null) {
                server.dispatchCloseMessagingConnection(this, ex);
            }
        }

    }

    public MessageData getMessageData() {
        return messageData;
    }

    public String getHost() {
        return socket.getInetAddress().getHostName();
    }

    public int getPort() {
        return socket.getPort();
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        if (!opened) {
            if (server != null) {
                throw new IOException("(S)Connection already closed " + this.toString());
            } else {
                throw new IOException("Connection already closed " + this.toString());
            }

        }

        opened = false;
        dispatchConnectionClosed(null);
        socket.close(); 
        if (server != null) {
            server.dispatchCloseMessagingConnection(this, null);
        }
        interrupt();
    }

    public void shutdown() {
        this.shutdownSignal = true;
        this.interrupt();

        try {
            close();
        } catch (IOException ex) {
            Log.err(ex, ex);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("server", server != null ? "yes" : "no")
            .appendAll(
                "opened",
                "port",
                "localPort",
                "chunkSize",
                "bytesSent",
                "bytesReceived"
            ).build();
    }

}
