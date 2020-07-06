package ru.ancevt.net.messaging.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import ru.ancevt.util.string.ToStringBuilder;

/**
 * @author ancevt
 */
public class MessageData {

    private byte[] data;

    public MessageData() {

    }

    public final void setBytes(int requestId, byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {

            baos.write(Message.SIGNATURE);         // 1
            dos.writeInt(data.length + 1 + 4 + 4); // 4
            dos.writeInt(requestId);               // 4
            dos.write(data);                       // ?

            this.data = baos.toByteArray();
        }
    }

    public byte[] getBytes() {
        return data;
    }

    public int length() {
        return data.length;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("length", length())
            .build();
    }

}
