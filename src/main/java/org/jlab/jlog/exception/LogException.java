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

    public LogException(String msg) {
        super(msg);
    }

    public LogException(String msg, Throwable t) {
        super(msg, t);
    }
}
