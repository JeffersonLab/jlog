package org.jlab.jlog;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comment JUnit tests.
 *
 * @author ryans
 */
public class CommentUnitTest {

  private Comment comment;

  public CommentUnitTest() {}

  @BeforeClass
  public static void setUpClass() {}

  @AfterClass
  public static void tearDownClass() {}

  @Before
  public void setUp() {
    comment = new Comment(2070473L, new Body(Body.ContentType.TEXT, "Hello World"));
  }

  @After
  public void tearDown() {}

  @Test
  public void testGetXML() throws Exception {
    String xml = comment.getXML();
    String expected = "<![CDATA[Hello World]]>";
    String actual = xml.split("<body>")[1].split("</body>")[0];

    actual = actual.trim();

    assertEquals(expected, actual);
  }

  @Test
  public void testBody() throws Exception {
    Body expected = new Body(Body.ContentType.TEXT, "Hello World");
    Body actual = comment.getBody();
    assertEquals(expected.getContent(), actual.getContent());
    assertEquals(expected.getType(), actual.getType());
  }

  // @Test
  public void testValidate() throws Exception {
    comment.validate();
  }

  // @Test
  public void testSubmit() throws Exception {
    comment.submit();
  }
}
