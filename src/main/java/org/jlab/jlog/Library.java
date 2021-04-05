package org.jlab.jlog;

import java.io.*;
import java.util.Properties;
import org.jlab.jlog.exception.LogRuntimeException;

/**
 * Captures the jlog library global version and configuration information.
 * The Overview page contains a list of all available configuration properties.
 * 
 * @author ryans
 */
public final class Library {

    private static final Properties defaultConfiguration;
    private static Properties userConfiguration;

    /**
     * Release properties are separate since they are managed by the build process - we don't want
     * users confused or able to Library.setConfiguration() overriding release info.
     */
    private static final Properties release;

    static {
        try ( // If either one fails to load, we bail out!
              InputStream defaultIn = Library.class.getClassLoader().getResourceAsStream("jlog-default.properties");
              InputStream releaseIn = Library.class.getClassLoader().getResourceAsStream("release.properties")
        ) {

            defaultConfiguration = new Properties();
            release = new Properties();

            defaultConfiguration.load(defaultIn);
            release.load(releaseIn);
        } catch (IOException e) {
            throw new LogRuntimeException("Unable to load properties.", e);
        }

        userConfiguration = new Properties(defaultConfiguration);

        // If user configuration exists in home dir, load it
        // If we want to get fancy (and complex) consider: https://github.com/harawata/appdirs
        String home = System.getProperty("user.home");
        File userPropsFile = new File(home + "/jlog.properties");
        try(InputStream userIn = new FileInputStream(userPropsFile)) {
            userConfiguration.load(userIn);
        } catch (FileNotFoundException e) {
            // OK, no user props, fine.
        } catch (IOException e) {
            e.printStackTrace(); // Probably a permissions or format issue...
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
        return userConfiguration;
    }

    /**
     * Set the configuration properties.
     * 
     * @param configuration The configuration
     */
    public static void setConfiguration(Properties configuration) {
        Library.userConfiguration = configuration;
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
