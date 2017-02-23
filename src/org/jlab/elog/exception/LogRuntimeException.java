package org.jlab.elog.exception;

/**
 * Top-level unchecked exception for the ELog client API that indicates a
 * problem outside the control of the API user such as a bug in the client API
 * implementation.
 *
 * This RuntimeException indicates a problem that is unrecoverable in the ELog
 * client API only, and doesn't preclude other modules from continuing to
 * function. If the API user would like a GUI to continue to function for
 * example then the user should catch this exception.
 *
 * @author ryans
 */
public class LogRuntimeException extends RuntimeException {

    public LogRuntimeException(String msg) {
        super(msg);
    }

    public LogRuntimeException(String msg, Throwable t) {
        super(msg, t);
    }
}
