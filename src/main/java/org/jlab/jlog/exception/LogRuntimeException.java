package org.jlab.jlog.exception;

/**
 * Top-level unchecked exception for the ELog client API that indicates a problem outside the
 * control of the API user such as a bug in the client API implementation.
 *
 * <p>This RuntimeException indicates a problem that is unrecoverable in the ELog client API only,
 * and doesn't preclude other modules from continuing to function. If the API user would like a GUI
 * to continue to function for example then the user should catch this exception.
 *
 * @author ryans
 */
public class LogRuntimeException extends RuntimeException {

  /**
   * Create a new LogRuntimeException with a message.
   *
   * @param msg The message
   */
  public LogRuntimeException(String msg) {
    super(msg);
  }

  /**
   * Create a new LogRuntimeException with a message and cause.
   *
   * @param msg The message
   * @param cause The cause
   */
  public LogRuntimeException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
