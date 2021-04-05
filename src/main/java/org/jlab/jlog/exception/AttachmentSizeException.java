package org.jlab.jlog.exception;

/**
 * Indicates that that an Attachment is too large or that the total size of all
 * attachments is too large.
 *
 * @author ryans
 */
public class AttachmentSizeException extends LogException {

    public AttachmentSizeException(String msg) {
        super(msg);
    }

    public AttachmentSizeException(String msg, Throwable t) {
        super(msg, t);
    }
}
