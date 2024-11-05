package org.jlab.jlog;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.naming.InvalidNameException;
import javax.net.ssl.HttpsURLConnection;
import org.jlab.jlog.exception.AttachmentSizeException;
import org.jlab.jlog.exception.LogException;
import org.jlab.jlog.exception.LogIOException;
import org.jlab.jlog.util.IOUtil;
import org.jlab.jlog.util.SecurityUtil;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit tests that require a client certificate and access to a log server.
 *
 * @author ryans
 */
public class CertificateAndServerIntegrationTest {

    private LogEntry entry;
    private LogEntryAdminExtension extension;

    public CertificateAndServerIntegrationTest() {
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

        String logbookHostname = "logbooks9.jlab.org";

        config.setProperty("SUBMIT_URL", "https://" + logbookHostname + "/incoming");
        config.setProperty("FETCH_URL", "https://" + logbookHostname + "/entry");
        config.setProperty("LOG_ENTRY_SCHEMA_URL", "https://" + logbookHostname + "/schema/Logentry.xsd");
        config.setProperty("COMMENT_SCHEMA_URL", "https://" + logbookHostname + "/schema/Comment.xsd");
        config.setProperty("IGNORE_SERVER_CERT_ERRORS", "true");
        config.setProperty("DEFAULT_UNIX_QUEUE_PATH", System.getProperty("java.io.tmpdir"));
        config.setProperty("DEFAULT_WINDOWS_QUEUE_PATH", System.getProperty("java.io.tmpdir"));
        
        /*config.list(System.out);*/
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetAuthorFromXMLWithCertificate() throws LogException, CertificateException, IOException, InvalidNameException {
    	File pemFile = new File(System.getProperty("user.home"), ".elogcert");
        entry.setClientCertificatePath(pemFile.getAbsolutePath(), true);
        String xml = entry.getXML();
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

    @Test
    public void testSubmit() throws Exception {
        Long id = entry.submit();

        if (id == 0) {
            throw new Exception("It was queued!", entry.whyQueued());
        }
    }

    @Test
    public void testSubmitNow() throws Exception {
        try {
            entry.submitNow();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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

    @Test
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
    }

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

            in = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
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

    @Test
    public void testProblemReport() throws Exception {
        ProblemReport report = new ProblemReport(ProblemReportType.OPS, true, 62, 9, 16413);
        entry.setProblemReport(report);
        long entryId = entry.submit();
        if (entryId == 0) {
        	throw new Exception("It was queued!", entry.whyQueued());
        }
        System.out.println("Created log entry: " + entryId);
    }

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
