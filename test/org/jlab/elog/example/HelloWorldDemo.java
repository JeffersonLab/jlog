package org.jlab.elog.example;

import org.jlab.elog.LogEntry;
import org.jlab.elog.exception.LogException;

/**
 * Hello World example of client API usage.
 * 
 * @author ryans
 */
public class HelloWorldDemo {
    public static void main(String[] args) throws LogException {
        LogEntry entry = new LogEntry("Hello World", "TLOG");
        
        long lognumber = entry.submitNow();
        
        System.out.println("Successfully submitted log entry number: " + lognumber);
    }
}
