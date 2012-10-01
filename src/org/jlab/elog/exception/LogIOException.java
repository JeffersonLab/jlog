package org.jlab.elog.exception;

/**
 *
 * @author ryans
 */
public class LogIOException extends LogException {

    public LogIOException(String msg) {
        super(msg);
    }
    
    public LogIOException(String msg, Throwable t) {
        super(msg, t);
    }
    
}
