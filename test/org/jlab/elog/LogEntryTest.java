package org.jlab.elog;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
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
}
