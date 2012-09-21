package org.jlab.elog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import javax.net.ssl.HttpsURLConnection;
import org.jlab.elog.LogItem.Body;
import org.jlab.elog.LogItem.ContentType;
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
        entry = new LogEntry("Drinking", "TLOG");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTitle() throws LogException {
        System.out.println("Title Test");
        String expected = "Testing title";
        entry.setTitle(expected);
        String actual = entry.getTitle();
        assertEquals(expected, actual);
    }

    @Test
    public void testCreated() throws LogException {
        System.out.println("Created Test");
        GregorianCalendar expected = new GregorianCalendar();
        entry.setCreated(expected);
        GregorianCalendar actual = entry.getCreated();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS Z");
        String expectedStr = formatter.format(expected.getTime());
        String actualStr = formatter.format(actual.getTime());
        System.out.println("Expected: " + expectedStr);
        System.out.println("Actual: " + actualStr);
        assertEquals(expectedStr, actualStr);
    }

    @Test
    public void testLognumber() throws LogException {
        System.out.println("Lognumber Test");
        Long expected = 1234L;
        entry.setLogNumber(expected);
        Long actual = entry.getLogNumber();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetLogbooks() throws LogException {
        System.out.println("Logbooks Set Test");
        String expected = "YOULOG,MELOG,WELOG";
        entry.setLogbooks(expected);
        String actual = entry.getLogbooks();
        assertEquals(expected, actual);
    }

    @Test
    public void testAddLogbooks() throws LogException {
        System.out.println("Logbooks Add Test");
        String expected = "TLOG,YOULOG,MELOG,WELOG";
        String addlist = "YOULOG,MELOG,WELOG";
        entry.addLogbooks(addlist);
        String actual = entry.getLogbooks();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEntrymakers() throws LogException {
        System.out.println("Entrymakers Set Test");
        String expected = "ryans,theo,cjs";
        entry.setEntryMakers(expected);
        String actual = entry.getEntryMakers();
        System.out.println("actual: " + actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testAddEntrymakers() throws LogException {
        System.out.println("Entrymakers Add Test");
        String expected = "ryans,theo,cjs";
        String addlist = "theo,cjs";
        entry.addEntryMakers(addlist);
        String actual = entry.getEntryMakers();
        System.out.println("actual: " + actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testBody() throws LogException {
        System.out.println("Body test");
        Body expected = new Body(ContentType.HTML, "<b>I like to make bold statements.</b>");
        entry.setTitle("<b>Title!</b>");
        entry.setBody(expected);
        Body actual = entry.getBody();
        System.out.println(entry.getXML());
        assertEquals(expected.getContent(), actual.getContent());
        assertEquals(expected.getType(), actual.getType());
    }

    @Test
    public void testGetXML() throws LogException {
        System.out.println("Get XML Test");
        String xml = entry.getXML();
        System.out.println(xml);
    }

    @Test
    public void testValidate() throws LogException {
        System.out.println("Validate Test");
        boolean obtainedSchema = entry.validate();
        System.out.println("Obtained Schema: " + obtainedSchema);
    }

    @Test
    public void testQueue() throws LogException {
        System.out.println("Queue Test");
        entry.queue();
    }

    @Test
    public void testServerCert() throws Exception {
        String requestURL = "https://logbooks.jlab.org";

        HttpsURLConnection con = null;

        URL url = new URL(requestURL);
        con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(SecurityUtil.getTrustySocketFactory());
        con.setRequestMethod("GET");
        con.setDoInput(true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String line = null;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        reader.close();
    }

    @Test
    public void testClientCertJKS() throws Exception {
        String requestURL = "https://logbooks.jlab.org/authtest";
        String keystorePath = "C:/Users/ryans/Desktop/logclient.jks";

        HttpsURLConnection con = null;

        URL url = new URL(requestURL);
        con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(SecurityUtil.getSocketFactoryJKS(keystorePath));
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.setDoOutput(false);
        con.connect();

        String line = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        reader.close();
        con.disconnect();
    }

    @Test
    public void testClientCertP12() throws Exception {
        String requestURL = "https://logbooks.jlab.org/authtest";
        String p12Path = "C:/Users/ryans/Desktop/ryans2.p12";

        HttpsURLConnection con = null;

        URL url = new URL(requestURL);
        con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(SecurityUtil.getSocketFactoryPKCS12(p12Path));
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.setDoOutput(false);
        con.connect();

        String line = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        reader.close();
        con.disconnect();
    }

    @Test
    public void testClientCertPEM() throws Exception {
        String requestURL = "https://logbooks.jlab.org/authtest";
        String pemPath = "C:/Users/ryans/Desktop/ryans.pem";

        HttpsURLConnection con = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(requestURL);
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(pemPath, true));
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(false);
            con.connect();

            String line;

            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }
}
