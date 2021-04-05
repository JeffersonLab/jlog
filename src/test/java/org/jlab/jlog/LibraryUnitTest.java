package org.jlab.jlog;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;


public class LibraryUnitTest {

    private static final Properties release = new Properties();

    static {
        try (
              InputStream releaseIn = Library.class.getClassLoader().getResourceAsStream("release.properties")
        ) {
            release.load(releaseIn);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load properties for testing.", e);
        }
    }

    @Test
    public void testReleaseInfo() throws Exception {
        String expectedReleaseDate = release.getProperty("RELEASE_DATE");
        String actualReleaseDate = Library.getReleaseDate();

        String expectedVersion = release.getProperty("VERSION");
        String actualVersion = Library.getVersion();

        assertEquals(expectedReleaseDate, actualReleaseDate);
        assertEquals(expectedVersion, actualVersion);
    }
}
