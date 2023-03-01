package org.jlab.jlog.example;

import org.jlab.jlog.Library;
import org.jlab.jlog.LogEntry;
import org.jlab.jlog.exception.LogException;

/**
 * TLog test example of client API usage that accepts server from env.
 * 
 * @author ryans
 */
public class TLog {
    public static void main(String[] args) throws LogException {
        String server = System.getenv("LOGBOOK_SERVER");

        if(server != null && !server.isEmpty()) {
            Library.setServer(server);
        }

        String submitUrl = Library.getConfiguration().getProperty("SUBMIT_URL");
        System.out.println("Using Submit URL: " + submitUrl);

        LogEntry entry = new LogEntry("Testing", "TLOG");

        long lognumber = entry.submitNow();

        System.out.println("Successfully submitted log entry number: " + lognumber);
    }
}
