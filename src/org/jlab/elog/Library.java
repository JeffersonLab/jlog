package org.jlab.elog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jlab.elog.exception.LogRuntimeException;

/**
 * Captures the jlog library global version and configuration information.
 * The Overview page contains a list of all available configuration properties.
 * 
 * @author ryans
 */
public final class Library {

    private static final String version = "2.3";
    private static Properties configuration;

    static {
        InputStream in = Library.class.getClassLoader().getResourceAsStream(
                "org/jlab/elog/elog.properties");

        configuration = new Properties();

        try {
            configuration.load(in);
        } catch (IOException e) {
            throw new LogRuntimeException("Unable to load properties.", e);
        }
    }

    private Library() {
        // Can't instantiate publicly
    }

    /**
     * Get the configuration properties.
     * 
     * @return The configuration
     */
    public static Properties getConfiguration() {
        return configuration;
    }

    /**
     * Set the configuration properties.
     * 
     * @param configuration The configuration
     */
    public static void setConfiguration(Properties configuration) {
        Library.configuration = configuration;
    }

    /**
     * Returns the library version String. A programmaticaly accessible version
     * String is a requirement of JLab accelerator software certification.
     *
     * The major version number corresponds to the API version. The minor
     * version number corresponds to the implementation version.
     *
     * @return The version String
     */
    public static String getVersion() {
        return version;
    }
}
