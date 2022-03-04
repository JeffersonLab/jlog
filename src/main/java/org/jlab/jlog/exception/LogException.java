package org.jlab.jlog.exception;

/**
 * Top-level checked exception for the ELog client API that indicates a
 * condition that a reasonable application might want to catch. A LogException
 * generally indicates a misuse of the API or a condition correctable by the
 * user.
 *
 * @author ryans
 */
public class LogException extends Exception {

    /**
     * Create a new LogException with a message.
     *
     * @param msg The message
     */
    public LogException(String msg) {
        super(msg);
    }

    /**
     * Create a new LogException with a message and cause.
     *
     * @param msg The message
     * @param cause The cause
     */
    public LogException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
