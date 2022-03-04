package org.jlab.jlog.exception;

/**
 * Indicates that the schema is not available.
 * 
 * @author ryans
 */
public class SchemaUnavailableException extends LogException {

    /**
     * Create a new SchemaUnavailableException with a message.
     *
     * @param msg The message
     */
    public SchemaUnavailableException(String msg) {
        super(msg);
    }

    /**
     * Create a new SchemaUnavailableException with a message and cause.
     *
     * @param msg The message
     * @param cause The cause
     */
    public SchemaUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
