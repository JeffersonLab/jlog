package org.jlab.elog;

/**
 * Captures the library version number.  A programmaticaly accessible version
 * number is a requirements of JLab accelerator software certification.
 * 
 * The major version number corresponds to the API version.
 * The minor version number corresponds to the implementation version.
 * 
 * @author ryans
 */
public final class Version {
    public static final String VERSION = "2.3";
            
    private Version() {
        // Can't instantiate publicly
    }
}
