package org.jlab.jlog.exception;

/**
 * Indicates that XML is not well-formed.
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
