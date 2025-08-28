package org.jlab.jlog.exception;

/**
 * Indicates that XML is not well-formed.
 *
 * @author ryans
 */
public class MalformedXMLException extends LogException {

  /**
   * Create a new MalformedXMLException with a message.
   *
   * @param msg The message
   */
  public MalformedXMLException(String msg) {
    super(msg);
  }

  /**
   * Create a new MalformedXMLException with a message and cause.
   *
   * @param msg The message
   * @param cause The cause
   */
  public MalformedXMLException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
