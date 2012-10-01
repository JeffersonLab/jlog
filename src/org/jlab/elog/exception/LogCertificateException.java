package org.jlab.elog.exception;

/**
 *
 * @author ryans
 */
public class LogCertificateException extends LogException {
    public LogCertificateException(String msg) {
        super(msg);
    }
    
    public LogCertificateException(String msg, Throwable t) {
        super(msg, t);
    }
}
