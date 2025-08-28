package org.jlab.jlog.exception;

/**
 * Indicates a problem with either the client or server PKI certificate.
 *
 * @author ryans
 */
public class LogCertificateException extends LogException {

  /**
   * Create a new LogCertificateException with message.
   *
   * @param msg The message
   */
  public LogCertificateException(String msg) {
    super(msg);
  }

  /**
   * Create a new LogCertificateException with message and cause.
   *
   * @param msg The message
   * @param cause The cause
   */
  public LogCertificateException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
