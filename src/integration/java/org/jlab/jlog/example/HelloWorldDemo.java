package org.jlab.jlog.example;

import org.jlab.jlog.LogEntry;
import org.jlab.jlog.exception.LogException;

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
