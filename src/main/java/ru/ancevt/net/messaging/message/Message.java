package ru.ancevt.net.messaging.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import ru.ancevt.net.messaging.Log;
import ru.ancevt.util.string.ToStringBuilder;

/**
 *
 * @author ancevt
 */
public abstract class Message {

    public static final int SIGNATURE = 0xFF;
    public static final int HEADERS_SIZE = 9;

    private static int idCounter;

    private MessageData messageData;

    private InputStream inputStream;
    private DataInputStream dataInputStream;

    private int requestId;

    public Message() {
        requestId = ++idCounter;
    }

    public Message(MessageData messageData) {
        this();
        try {
            this.messageData = messageData;

            final int sign = getDataInputStream().readUnsignedByte();

            if (sign != SIGNATURE) {
                throw new MessagingException("Invalid message signature 0x" + Integer.toString(sign, 16));
            }

            getDataInputStream().skip(4);
            this.requestId = getDataInputStream().readInt();
        } catch (IOException ex) {
            Log.err(ex, ex);
        }
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getRequestId() {
        return requestId;
    }

    protected final void setMessageData(MessageData messageData) {
        this.messageData = messageData;
    }

    public final MessageData getMessageData() {
        if (messageData == null) {
            messageData = new MessageData();
        }

        return messageData;
    }

    public final InputStream getInputStream() {
        return inputStream == null ? inputStream = new ByteArrayInputStream(getMessageData().getBytes()) : inputStream;
    }

    public final DataInputStream getDataInputStream() {
        return dataInputStream == null ? dataInputStream = new DataInputStream(getInputStream()) : dataInputStream;
    }

    public final int length() {
        return messageData != null ? messageData.length() : 0;
    }

    public abstract Message prepare();

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("requestId")
            .append("length", length())
            .build();
    }

}
