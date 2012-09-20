package org.jlab.elog;

/**
 * Top-level unchecked exception for the ELog client API that indicates a
 * serious problem such as a bug in the client API 
 * implementation as opposed to a misuse of the API, which generally would 
 * result in a LogException.  Note this class does not extend Error because 
 * subclasses of Error generally are associated with extreme fatal JVM-wide
 * errors that can not and should not be recovered from such as out of memory or
 * stack overflow.  This RuntimeException indicates a problem that is 
 * unrecoverable in the ELog client API only, and doesn't preclude other modules
 * from continuing to function.  If the API user would like a GUI to continue to 
 * function for example then the user should catch this exception.
 * 
 * @author ryans
 */
public class LogRuntimeException extends RuntimeException {
    
}
