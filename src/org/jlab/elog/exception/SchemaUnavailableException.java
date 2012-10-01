package org.jlab.elog.exception;

/**
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
