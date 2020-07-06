package ru.ancevt.net.messaging;

import org.apache.log4j.Logger;

/**
 *
 * @author ancevt
 */
public final class Log {

    public final static Logger logger = Logger.getLogger("Messaging");
    public final static Logger dev = Logger.getLogger("DEV");
    
    public final static void err(Object message, Throwable th) {
        logger.error(message, th);
    }
    
    public final static void err(Throwable th) {
        logger.error(th);
    }
}
