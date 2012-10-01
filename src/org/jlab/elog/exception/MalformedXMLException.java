package org.jlab.elog.exception;

/**
 *
 * @author ryans
 */
public class MalformedXMLException extends LogException {

    public MalformedXMLException(String msg) {
        super(msg);
    }
    
    public MalformedXMLException(String msg, Throwable t) {
        super(msg, t);
    }
    
}
