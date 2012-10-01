package org.jlab.elog.exception;

/**
 * Top-level checked exception for the ELog client API that indicates a
 * condition that a reasonable application might want to catch.
 * 
 * @author ryans
 */
public class LogException extends Exception {

    public LogException(String msg) {
        super(msg);
    }
    
    public LogException(String msg, Throwable t) {
        super(msg, t);
    }
    
}
