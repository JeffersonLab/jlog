package org.jlab.jlog.util;

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
public class IOUtilUnitTest {
    
    public IOUtilUnitTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testCsvToArray() {
        String values = "1, 2,3, 4 , 5";
        String[] expected = new String[] {"1","2","3","4","5"};
        String[] actual = IOUtil.csvToArray(values);
        assertArrayEquals(expected, actual);
    }
}
