package org.jlab.jlog.exception;

/**
 * Indicates that XML is invalid.
 * 
 * @author ryans
 */
public class InvalidXMLException extends LogException {
    /**
     * Create a new InvalidXMLException with message.
     *
     * @param msg The message
     */
    public InvalidXMLException(String msg) {
        super(msg);
    }

    /**
     * Create a new InvalidXMLException with message and cause.
     *
     * @param msg The message
     * @param cause The cause
     */
    public InvalidXMLException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
