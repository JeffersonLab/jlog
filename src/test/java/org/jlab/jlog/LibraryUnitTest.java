package org.jlab.jlog;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.Ignore;
import org.junit.Test;

public class LibraryUnitTest {

  private static final Properties release = new Properties();

  static {
    try (InputStream releaseIn =
        Library.class.getClassLoader().getResourceAsStream("release.properties")) {
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

  @Test
  @Ignore // Not really sure how else to test this other than manually
  public void testUserProp() throws Exception {
    Properties props = Library.getConfiguration();

    System.out.println("QUEUE_PATH: " + props.getProperty("QUEUE_PATH"));

    // Set config in ~/jlog.properties and see if they are read!
  }
}
