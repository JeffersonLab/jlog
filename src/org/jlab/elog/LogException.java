package org.jlab.elog;

/**
 * Top-level checked exception for the ELog client API that indicates a
 * condition that a reasonable application might want to catch.
 * 
 * @author ryans
 */
public class LogException extends Exception {

    LogException(String msg) {
        super(msg);
    }
    
    LogException(String msg, Throwable t) {
        super(msg, t);
    }
    
}
