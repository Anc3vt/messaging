package ru.ancevt.net.messaging.message;

/**
 *
 * @author ancevt
 */
public class MessagingException extends RuntimeException {

    /**
     * Creates a new instance of <code>MessagingException</code> without detail message.
     */
    public MessagingException() {
    }

    /**
     * Constructs an instance of <code>MessagingException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public MessagingException(String msg) {
        super(msg);
    }
}
