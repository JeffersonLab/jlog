package org.jlab.jlog.exception;

/**
 * Indicates that the schema is not available.
 * 
 * @author ryans
 */
public class SchemaUnavailableException extends LogException {
    public SchemaUnavailableException(String msg) {
        super(msg);
    }
    
    public SchemaUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
