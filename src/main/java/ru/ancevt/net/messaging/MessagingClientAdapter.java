/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.ancevt.net.messaging;

import ru.ancevt.net.messaging.message.MessageData;

/**
 *
 * @author ancevt
 */
public abstract class MessagingClientAdapter implements MessagingConnectionListener {

    @Override
    public void connectionOpened(MessagingConnection connection) {
    }

    @Override
    public void incomingMessageData(MessagingConnection connection, MessageData messageData) {
    }

    @Override
    public void connectionClosed(MessagingConnection connection, Throwable exception) {
    }
    
}
