package org.jlab.elog;

/**
 *
 * @author ryans
 */
public class LogException extends Exception {

    LogException(String msg) {
        super(msg);
    }
    
    LogException(String msg, Throwable t) {
        super(msg, t);
    }
    
}
