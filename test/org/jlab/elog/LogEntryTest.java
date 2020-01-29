package org.jlab.elog;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.naming.InvalidNameException;
import javax.net.ssl.HttpsURLConnection;
import org.jlab.elog.exception.AttachmentSizeException;
import org.jlab.elog.exception.LogException;
import org.jlab.elog.exception.LogIOException;
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

        Properties config = Library.getConfiguration();

        String logbookHostname = "logbooktest.acc.jlab.org";

        config.setProperty("SUBMIT_URL", "https://" + logbookHostname + "/incoming");
        config.setProperty("FETCH_URL", "https://" + logbookHostname + "/entry");
        config.setProperty("LOG_ENTRY_SCHEMA_URL", "https://" + logbookHostname + "/schema/Logentry.xsd");
        config.setProperty("COMMENT_SCHEMA_URL", "https://" + logbookHostname + "/schema/Comment.xsd");
        config.setProperty("IGNORE_SERVER_CERT_ERRORS", "true");
        config.setProperty("DEFAULT_UNIX_QUEUE_PATH", System.getProperty("java.io.tmpdir"));
        config.setProperty("DEFAULT_WINDOWS_QUEUE_PATH", System.getProperty("java.io.tmpdir"));
        
        config.list(System.out);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAdminSetAuthor() throws LogException {
        String expected = "theo";
        extension.setAuthor(expected);
        
        // Must have special certificate in order to set Author to someone else
        //Long id = entry.submitNow();
        //entry = LogEntry.getLogEntry(id, "checking");
        
        assertEquals(expected, entry.getAuthor());
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
    public void testGetAuthorFromXMLWithCertificate() throws LogException, CertificateException, IOException, InvalidNameException {
    	File pemFile = new File(System.getProperty("user.home"), ".elogcert");
        String xml = entry.getXML(pemFile.getAbsolutePath());
        //System.out.println(xml);
        String expected = SecurityUtil.getCommonNameFromCertificate(SecurityUtil.fetchCertificateFromPEM(IOUtil.fileToBytes(pemFile)));
        String actual = xml.split("<username>")[1].split("</username>")[0];
        assertEquals(expected, actual);
    }
    
    /*@Test
    public void testValidate() throws LogException {
        // logbook server no longer supports validation... (no longer provides schemas that validate)
        //entry.validate();
    }*/

    /*@Test
    public void testSubmit() throws Exception {
        Long id = entry.submit();

        if (id == 0) {
            throw new Exception("It was queued!", entry.whyQueued());
        }
    }*/

    /*@Test
    public void testSubmitNow() throws Exception {
        try {
            entry.submitNow();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }*/

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
    public void testCharacterEncoding() throws Exception {
        String expected = "ΩΨΣΦΠΔ";
        entry.setTitle(expected);
        entry.setBody(expected);
        //entry.submit();
        String actual = entry.getTitle();
        assertEquals(expected, actual);
    }

    @Test(expected = LogIOException.class)
    public void testMissingEntry() throws Exception {
        LogEntry revision = LogEntry.getLogEntry(2070480L, "Testing Missing");
        String expected = "Testing 123";
        String actual = revision.getTitle();
        assertEquals(expected, actual);
    }

    @Test
    public void testRevision() throws Exception {
        // Log Entry attachments only exist on primary server so don't use test server for this one
        String logbookHostname = "logbooks.jlab.org";
        Properties config = Library.getConfiguration();
        config.setProperty("FETCH_URL", "https://" + logbookHostname + "/entry");

        LogEntry revision = LogEntry.getLogEntry(3001286L, "Testing Revision");
        String expected = "Long Shutdown Summary, presented 2013-03-14";
        String actual = revision.getTitle();
        assertEquals(expected, actual);
    }

    /*@Test
    public void testLargeBodySubmit() throws Exception {
        StringBuilder builder = new StringBuilder();

        // Server checks for XML file over 64,000,000 bytes.
        // Of that, a "body" portion over ~8MB will generally result in server returning HTTP 500 due to timeout processing request
        //Inefficiently create a 8,000,000 byte string!
        for (int i = 0; i < 8000000; i++) {
            builder.append(0);
        }

        entry.setBody(builder.toString());

        try {
            Long id = entry.submitNow(); // Don't bother queuing
            System.out.println("Created log entry: " + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    @Test(expected = AttachmentSizeException.class)
    public void testLargeAttachmentSubmit() throws Exception {
        StringBuilder builder = new StringBuilder();

        //Inefficiently create a 64MB string!
        for (int i = 0; i < 67108864; i++) {
            builder.append(0);
        }

        File tmp = File.createTempFile("jlogUnitTest", ".tmp");

        InputStream in = null;
        OutputStream out = null;

        try {

            in = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
            out = new FileOutputStream(tmp);

            IOUtil.copy(in, out);

            entry.addAttachment(tmp.getAbsolutePath());

            Long id = entry.submitNow(); // Don't bother queuing 

            System.out.println("Created log entry: " + id);
        } finally {
            IOUtil.closeQuietly(in);
            IOUtil.closeQuietly(out);
            IOUtil.deleteQuietly(tmp);
        }
    }

    /*@Test
    public void testProblemReport() throws Exception {
        ProblemReport report = new ProblemReport(ProblemReportType.OPS, true, 62, 9, 16413);
        entry.setProblemReport(report);
        long entryId = entry.submit();
        if (entryId == 0) {
        	throw new Exception("It was queued!", entry.whyQueued());
        }
    }*/

    @Test
    public void testLoadProblemReport() throws Exception {
        // Problem Reports only exist on primary server so don't use test server for this one
        String logbookHostname = "logbooks.jlab.org";
        Properties config = Library.getConfiguration();
        config.setProperty("FETCH_URL", "https://" + logbookHostname + "/entry");
        
        
        LogEntry revision = LogEntry.getLogEntry(3260179L, "Testing Problem Report");
        int expected = 20451;
        ProblemReport report = revision.getProblemReport();
        /*System.out.println(revision.getXML());*/
        int actual = report.getComponentId();
        assertEquals(expected, actual);
    }
}
