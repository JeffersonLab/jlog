package org.jlab.elog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import javax.net.ssl.HttpsURLConnection;
import org.jlab.elog.exception.AttachmentSizeException;
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
 * LogEntry JUnit tests.
 *
 * @author ryans
 */
public class LogEntryTest {

    private LogEntry entry;
    private LogEntryAdminExtension extension;

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
        extension = new LogEntryAdminExtension(entry);
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
        extension.setCreated(expected);
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
        entry.addAttachment(filepath);
        String actualFilename = entry.getAttachments()[0].getFileName();
        String actualCaption = entry.getAttachments()[0].getCaption();
        String actualMimeType = entry.getAttachments()[0].getMimeType();
        String actualContent = IOUtil.streamToString(entry.getAttachments()[0].getData(), "UTF-8");
        assertEquals(expectedFilename, actualFilename);
        assertEquals(expectedCaption, actualCaption);
        assertEquals(expectedMimeType, actualMimeType);
        assertEquals(expectedContent, actualContent);

        expectedMimeType = "application/xml";
        actualMimeType = entry.getAttachments()[1].getMimeType();
        assertEquals(expectedMimeType, actualMimeType);

        entry.deleteAttachments();
        int expectedLength = 0;
        int actualLength = entry.getAttachments().length;
        assertEquals(expectedLength, actualLength);
    }

    @Test(expected = AttachmentSizeException.class)
    public void testLargeAttachment() throws Exception {
        File tmp = File.createTempFile("eloglibunittest", ".tmp");
        tmp.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
        int numBytes = 17 * 1024 * 1024;
        for (int i = 0; i < numBytes; i++) {
            writer.write(48); // I think int 48 corresponds to ASCII char '0'
        }

        writer.close();
        entry.addAttachment(tmp.getAbsolutePath());
    }

    @Test
    public void testLognumber() throws LogException {
        Long expected = 1234L;
        extension.setLogNumber(expected);
        Long actual = entry.getLogNumber();
        assertEquals(expected, actual);
    }

    @Test
    public void testEmailNotify() throws Exception {
        String expected = "ryans@jlab.org";
        entry.setEmailNotify(expected);
        String actual = entry.getEmailNotifyCSV();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetLogbooks() throws LogException {
        String expected = "YOULOG,MELOG,WELOG";
        entry.setLogbooks(expected);
        String actual = entry.getLogbooksCSV();
        assertEquals(expected, actual);
    }

    @Test
    public void testAddLogbooks() throws LogException {
        String expected = "TLOG,YOULOG,MELOG,WELOG";
        String addlist = "YOULOG,MELOG,WELOG";
        entry.addLogbooks(addlist);
        String actual = entry.getLogbooksCSV();
        assertEquals(expected, actual);
    }

    @Test
    public void testAddTags() throws LogException {
        String expected = "Readme,Autolog";
        entry.addTags(expected);
        String actual = entry.getTagsCSV();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetTags() throws LogException {
        String expected = "Readme,Autolog";
        entry.setTags(expected);
        String actual = entry.getTagsCSV();
        assertEquals(expected, actual);
    }

    @Test
    public void testReferences() throws LogException {
        String expectedType = "atlis";
        String expectedId = "123";
        entry.addReference(new Reference(expectedType, expectedId));
        String actualType = entry.getReferences()[0].getType();
        String actualId = entry.getReferences()[0].getId();
        assertEquals(expectedType, actualType);
        assertEquals(expectedId, actualId);

        entry.deleteReferences();
        int expectedLength = 0;
        int actualLength = entry.getReferences().length;
        assertEquals(expectedLength, actualLength);
    }

    @Test
    public void testAddComments() {
        long expected = 123L;
        extension.addComment(new Comment(expected, new Body(Body.ContentType.TEXT, "Hello World")));
        String xml = entry.getXML();
        long actual = Long.parseLong(xml.split("<Comment>")[1].split("</Comment>")[0].split("<lognumber>")[1].split("</lognumber>")[0]);
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEntrymakers() throws LogException {
        String expected = "cjs,theo";
        entry.setEntryMakers(expected);
        String actual = entry.getEntryMakersCSV();
        assertEquals(expected, actual);
    }

    @Test
    public void testAddEntrymakers() throws LogException {
        String expected = "theo,cjs";
        String addlist = "theo,cjs";
        entry.addEntryMakers(addlist);
        String actual = entry.getEntryMakersCSV();
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
        Long id = entry.submit();

        if (id == 0) {
            throw new Exception("It was queued!", entry.whyQueued());
        }
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
            IOUtil.closeQuietly(is);

            if (con != null) {
                con.disconnect();
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
            IOUtil.closeQuietly(is);

            if (con != null) {
                con.disconnect();
            }
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testRevision() throws Exception {
        LogEntry revision = LogEntry.getLogEntry(2070480L, "Testing");
        String expected = "Testing 123";
        String actual = revision.getTitle();
        assertEquals(expected, actual);
    }
    
    @Test
    public void testLargeSubmit() throws Exception {
        StringBuilder builder = new StringBuilder();
        
        //Inefficiently create a 64MB string!
        for(int i = 0; i < 67108864; i++) {
            builder.append(0);
        }
        
        // Circumvent size checks by using body, not attachment!
        entry.setBody(builder.toString());
        
        Long id = entry.submitNow(); // Don't bother queuing

        if (id == 0) {
            throw new Exception("It was queued!", entry.whyQueued());
        }
    }    
}
