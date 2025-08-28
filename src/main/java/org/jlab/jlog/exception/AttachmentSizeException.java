package org.jlab.jlog.exception;

/**
 * Indicates that an Attachment is too large or that the total size of all attachments is too large.
 *
 * @author ryans
 */
public class AttachmentSizeException extends LogException {

  /**
   * Create a new AttachmentSizeException with message.
   *
   * @param msg The messages
   */
  public AttachmentSizeException(String msg) {
    super(msg);
  }

  /**
   * Create a new AttachementSizeException with message and cause.
   *
   * @param msg The message
   * @param t The cause
   */
  public AttachmentSizeException(String msg, Throwable t) {
    super(msg, t);
  }
}
