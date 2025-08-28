package org.jlab.jlog.exception;

/**
 * Indicates an exception occurred during IO.
 *
 * @author ryans
 */
public class LogIOException extends LogException {

  /**
   * Create a new LogIOException with a message.
   *
   * @param msg The message
   */
  public LogIOException(String msg) {
    super(msg);
  }

  /**
   * Create a new LogIOException with a message and cause.
   *
   * @param msg The message
   * @param cause The cause
   */
  public LogIOException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
