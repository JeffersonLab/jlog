package org.jlab.elog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import javax.net.ssl.HttpsURLConnection;
import org.jlab.elog.exception.LogException;
import org.jlab.elog.util.IOUtil;
import org.jlab.elog.util.SecurityUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ryans
 */
public class LogEntryTest {

    private LogEntry entry;

    public LogEntryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws LogException {
        entry = new LogEntry("Testing 123", "TLOG");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTitle() throws LogException {
        String expected = "Testing title";
        entry.setTitle(expected);
        String actual = entry.getTitle();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAuthor() {
        String actual = entry.getAuthor();
        String expected = System.getProperty("user.name");
        assertEquals(expected, actual);
    }

    @Test
    public void testCreated() throws LogException {
        GregorianCalendar expected = new GregorianCalendar();
        entry.setCreated(expected);
        GregorianCalendar actual = entry.getCreated();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS Z");
        String expectedStr = formatter.format(expected.getTime());
        String actualStr = formatter.format(actual.getTime());
        assertEquals(expectedStr, actualStr);
    }

    @Test
    public void testAttachment() throws Exception {
        String expectedFilename = "testing.xml";
        String expectedCaption = "Testing 123";
        String expectedMimeType = "xml";
        String expectedContent = entry.getXML();
        String filepath = new File(System.getProperty("java.io.tmpdir"), expectedFilename).getAbsolutePath();
        entry.queue(filepath);
        entry.addAttachment(filepath, expectedCaption, expectedMimeType);
        String actualFilename = entry.getAttachments()[0].getFileName();
        String actualCaption = entry.getAttachments()[0].getCaption();
        String actualMimeType = entry.getAttachments()[0].getMimeType();
        String actualContent = IOUtil.streamToString(entry.getAttachments()[0].getData(), "UTF-8");
        assertEquals(expectedFilename, actualFilename);
        assertEquals(expectedCaption, actualCaption);
        assertEquals(expectedMimeType, actualMimeType);
        assertEquals(expectedContent, actualContent);
        
        entry.deleteAttachments();
        int expectedLength = 0;
        int actualLength = entry.getAttachments().length;
        assertEquals(expectedLength, actualLength);
    }
    
    @Test
    public void testLognumber() throws LogException {
        Long expected = 1234L;
        entry.setLogNumber(expected);
        Long actual = entry.getLogNumber();
        assertEquals(expected, actual);
    }

    @Test
    public void testEmailNotify() throws Exception {
        String expected = "ryans@jlab.org";
        entry.setEmailNotify(expected);
        String actual = entry.getEmailNotify();
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSetLogbooks() throws LogException {
        String expected = "YOULOG,MELOG,WELOG";
        entry.setLogbooks(expected);
        String actual = entry.getLogbooks();
        assertEquals(expected, actual);
    }

    @Test
    public void testAddLogbooks() throws LogException {
        String expected = "TLOG,YOULOG,MELOG,WELOG";
        String addlist = "YOULOG,MELOG,WELOG";
        entry.addLogbooks(addlist);
        String actual = entry.getLogbooks();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEntrymakers() throws LogException {
        String expected = "cjs,theo";
        entry.setEntryMakers(expected);
        String actual = entry.getEntryMakers();
        assertEquals(expected, actual);
    }

    @Test
    public void testAddEntrymakers() throws LogException {
        String expected = "theo,cjs";
        String addlist = "theo,cjs";
        entry.addEntryMakers(addlist);
        String actual = entry.getEntryMakers();
        assertEquals(expected, actual);
    }

    @Test
    public void testSticky() {
        boolean expected = true;
        entry.setSticky(expected);
        boolean actual = entry.isSticky();
        assertEquals(expected, actual);        
    }
    
    @Test
    public void testBody() throws LogException {
        Body expected = new Body(Body.ContentType.HTML, "<b>I like to make bold statements.</b>");
        entry.setTitle("<b>Title!</b>");
        entry.setBody(expected);
        Body actual = entry.getBody();
        assertEquals(expected.getContent(), actual.getContent());
        assertEquals(expected.getType(), actual.getType());
    }

    @Test
    public void testGetXML() throws LogException {
        String xml = entry.getXML();
        String expected = "Testing 123";
        String actual = xml.split("<title>")[1].split("</title>")[0];
        assertEquals(expected, actual);
    }

    @Test
    public void testValidate() throws LogException {
        entry.validate();
    }

    @Test
    public void testSubmit() throws Exception {
        entry.submit();
    }
    
    @Test
    public void testQueue() throws LogException {
        String expected = "Save and then load me";
        entry.setTitle(expected);
        String filepath = new File(System.getProperty("java.io.tmpdir"), "test.xml").getAbsolutePath();
        entry.queue(filepath);
        LogEntry tmp = new LogEntry(filepath);
        String actual = tmp.getTitle();
        assertEquals(expected, actual);
    }

    @Test
    public void testServerCert() throws Exception {
        String requestURL = "https://logbooks.jlab.org";

        HttpsURLConnection con = null;

        InputStream is = null;

        String expected = "<!DOCTYPE html>";
        String actual = null;

        try {
            URL url = new URL(requestURL);
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getTrustySocketFactory());
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.connect();

            is = con.getInputStream();

            String content = IOUtil.streamToString(is, "UTF-8");
            actual = content.substring(0, 15);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }

        assertEquals(expected, actual);

    }

    @Test
    public void testClientCertPEM() throws Exception {
        String requestURL = "https://logbooks.jlab.org/authtest";
        String pemPath = new File(System.getProperty("user.home"), ".elogcert").getAbsolutePath();

        HttpsURLConnection con = null;
        InputStream is = null;

        String expected = "howdy";
        String actual = null;

        try {
            URL url = new URL(requestURL);
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(pemPath, true));
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(false);
            con.connect();

            is = con.getInputStream();

            String content = IOUtil.streamToString(is, "UTF-8");
            actual = content.substring(0, 5);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }

        assertEquals(expected, actual);
    }
}
