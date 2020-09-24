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

    private static Properties configuration;

    /**
     * Release properties are separate since they are managed by the build process - we don't want
     * users confused or able to Library.setConfiguration() overriding release info.
     */
    private static Properties release;

    static {
        try ( // If either one fails to load, we bail out!
            InputStream elogIn = Library.class.getClassLoader().getResourceAsStream("elog.properties");
            InputStream releaseIn = Library.class.getClassLoader().getResourceAsStream("release.properties");
        ) {

            configuration = new Properties();
            release = new Properties();

            configuration.load(elogIn);
            release.load(releaseIn);
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
     * Returns the library version String. A programmatically accessible version
     * String is a requirement of JLab accelerator software certification.
     *
     * Versioning follows <a href="https://semver.org/">Semantic Versioning</a>.
     *
     * @return The version String
     */
    public static String getVersion() {
        return release.getProperty("VERSION");
    }

    /**
     * The date in which this version of the library was released.
     *
     * @return The date string
     */
    public static String getReleaseDate() {
        return release.getProperty("RELEASE_DATE");
    }
}
