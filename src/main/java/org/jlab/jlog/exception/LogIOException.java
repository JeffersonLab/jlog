package org.jlab.jlog.exception;

/**
 * Indicates an exception occurred during IO.
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
