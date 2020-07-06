package ru.ancevt.net.messaging.dev;

import java.io.IOException;
import ru.ancevt.net.messaging.Log;
import ru.ancevt.net.messaging.MessagingClientAdapter;
import ru.ancevt.net.messaging.MessagingConnection;
import ru.ancevt.net.messaging.client.MessagingClient;
import ru.ancevt.net.messaging.client.MessagingClientListener;
import ru.ancevt.net.messaging.message.Message;
import ru.ancevt.net.messaging.message.MessageData;
import ru.ancevt.net.messaging.message.UTF8Message;
import ru.ancevt.net.messaging.server.MessagingServer;
import ru.ancevt.net.messaging.server.MessagingServerListener;
import ru.ancevt.util.system.UnixDisplay;
import ru.ancevt.util.args.Args;
import ru.ancevt.util.repl.ReplInterpreter;

/**
 *
 * @author ancevt
 */
public class Psvm {
    
    private static final int PORT = 7777;
    
    private static MessagingServer messagingServer;
    private static MessagingClient messagingClient;
    
    private static MessagingConnection serverConnection;
    private static MessagingConnection clientConnection;
    
    private static final Object lock = new Object();
    
    public static void main(String[] args) throws IOException, InterruptedException {
        prepareClientAndServer();
        
        final ReplInterpreter ri = new ReplInterpreter("> ");
        
        ri.addCommand("cs", (a) -> {
            final int count = a.getInt("-c", 1);
            final String messageText = a.getString("-m", "From client to server");
            try {
                for (int i = 0; i < count; i++) {
                    final UTF8Message message = new UTF8Message(messageText);
                    clientToServer(message);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        
        ri.addCommand("sc", (a) -> {
            final int count = a.getInt("-c", 1);
            final String messageText = a.getString("-m", "From server to client");
            try {
                for (int i = 0; i < count; i++) {
                    final UTF8Message message = new UTF8Message(messageText);
                    serverToClient(message);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        
        ri.addCommand("sd", (a) -> {
            try {
                messagingServer.shutdown();
            } catch (IOException ex) {
                Log.dev.error(ex, ex);
            }
        });
        
        ri.start();
    }
    
    private static void acceptServerConnection(MessagingConnection serverConnection) {
        serverConnection.addMessagingConnectionListener(new MessagingClientAdapter() {
            
            @Override
            public void incomingMessageData(MessagingConnection connection, MessageData messageData) {
                final UTF8Message message = new UTF8Message(messageData);
                Log.dev.info(UnixDisplay.GREEN + "S <-: " + message.toString() + UnixDisplay.RESET);
            
                final Args args = new Args(message.getText());
                if(args.contains("--ping-pong")) {
                    try {
                        serverToClient(message);
                    } catch (IOException ex) {
                        Log.dev.error(ex,ex);
                    }
                }
            
            }
            
            
        });
    }
    
    private static void acceptClientConnection(MessagingConnection clientConnection) {
        clientConnection.addMessagingConnectionListener(new MessagingClientAdapter() {
            
            @Override
            public void incomingMessageData(MessagingConnection connection, MessageData messageData) {
                final UTF8Message message = new UTF8Message(messageData);
                
                Log.dev.info(UnixDisplay.GREEN + "C <-: " + message.toString() + UnixDisplay.RESET);
                
                final Args args = new Args(message.getText());
                if(args.contains("--ping-pong")) {
                    try {
                        clientToServer(message);
                    } catch (IOException ex) {
                        Log.dev.error(ex,ex);
                    }
                }
            }
            
        });
    }
    
    private static void serverToClient(Message message) throws IOException {
        message.prepare();
        Log.dev.info(UnixDisplay.CYAN + "S ->: " + message.toString() + UnixDisplay.RESET);
        serverConnection.send(message);
    }
    
    private static void clientToServer(Message message) throws IOException {
        message.prepare();
        Log.dev.info(UnixDisplay.CYAN + "C ->: " + message.toString() + UnixDisplay.RESET);
        clientConnection.send(message);
    }
    
    private static void prepareClientAndServer() throws IOException {
        messagingServer = new MessagingServer();
        messagingServer.addMessagingServerListener(new MessagingServerListener() {
            
            @Override
            public void messagingServerStarted() {
                Log.dev.info("MessagingServer messagingServerStarted, port " + PORT);
                try {
                    messagingClient.connect("localhost", PORT);
                } catch (IOException ex) {
                    Log.dev.error(ex, ex);
                }
            }
            
            @Override
            public void acceptMessagingConnection(MessagingConnection connection) {
                Log.dev.info("MessagingServer acceptMessagingConnection, " + connection);
                acceptServerConnection(serverConnection = connection);
            }
            
            @Override
            public void closeMessagingConnection(MessagingConnection connection, Throwable ex) {
                Log.dev.info("MessagingServer closeMessagingConnection, " + connection + ", " + ex);
            }
            
            @Override
            public void serverShutdown() {
                Log.dev.info(UnixDisplay.RED + "MessagingServer shutdown" + UnixDisplay.RESET);
            }
            
        });
        new Thread(() -> {
            try {
                messagingServer.start(PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
        
        messagingClient = new MessagingClient();
        messagingClient.addMessagingClientListener(new MessagingClientListener() {
            
            @Override
            public void clientMessagingConnectionOpened(MessagingConnection connection) {
                Log.dev.info("MessagingClient clientMessagingConnectionOpened, " + connection);
                acceptClientConnection(clientConnection = connection);
                
            }
            
            @Override
            public void clientMessagingConnectionClosed(MessagingConnection connection, Throwable exception) {
                Log.dev.info("MessagingClient clientMessagingConnectionClosed, " + connection + ", " + exception);
            }
            
            @Override
            public void clientMessagingConnectionError(Throwable exception) {
                Log.dev.info("MessagingClient clientMessagingConnectionError, " + exception);
            }
            
        });
        
    }
    
}
