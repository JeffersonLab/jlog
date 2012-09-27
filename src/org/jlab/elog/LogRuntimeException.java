package org.jlab.elog;

/**
 * Top-level unchecked exception for the ELog client API that indicates a
 * serious problem such as a bug in the client API implementation as opposed to
 * a misuse of the API, which generally would result in a LogException.
 *
 * This RuntimeException indicates a problem that is unrecoverable in the ELog
 * client API only, and doesn't preclude other modules from continuing to
 * function. If the API user would like a GUI to continue to function for
 * example then the user should catch this exception.
 *
 * @author ryans
 */
public class LogRuntimeException extends RuntimeException {

    LogRuntimeException(String msg) {
        super(msg);
    }

    LogRuntimeException(String msg, Throwable t) {
        super(msg, t);
    }
}
