package ru.ancevt.net.messaging.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ru.ancevt.net.messaging.Log;
import ru.ancevt.util.fs.SimpleFileReader;
import ru.ancevt.util.string.ToStringBuilder;

/**
 *
 * @author ancevt
 */
public class UTF8Message extends Message {

    private String text;

    public UTF8Message(String text) throws IOException {
        super();
        this.text = text;
    }

    public UTF8Message(MessageData messageData) {
        super(messageData);

        try {
            this.text = SimpleFileReader.readUtf8(getInputStream());
        } catch (IOException ex) {
            Log.err(ex, ex);
            text = "error";
        }
    }

    public final String getText() {
        return text;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("requestId", getRequestId())
            .append("length", length())
            .append("text")
            .build();
    }

    @Override
    public Message prepare() {
        try {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                baos.write(text.getBytes());
                getMessageData().setBytes(getRequestId(), baos.toByteArray());
            }
        } catch (IOException ex) {
            Log.logger.error(ex);
        }
        return this;
    }

}
