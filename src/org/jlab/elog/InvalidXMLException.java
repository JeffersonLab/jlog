package org.jlab.elog;

/**
 *
 * @author ryans
 */
public class InvalidXMLException extends LogException {
    public InvalidXMLException(String msg) {
        super(msg);
    }
    
    public InvalidXMLException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
