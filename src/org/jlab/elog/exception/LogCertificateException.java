package org.jlab.elog.exception;

/**
 * Indicates a problem with either the client or server PKI certificate. 
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
