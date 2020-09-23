package org.jlab.elog.example;

import org.jlab.elog.Comment;
import org.jlab.elog.LogEntry;
import org.jlab.elog.exception.LogException;

/**
 * Basic example of client API usage.
 * 
 * @author ryans
 */
public class BasicDemo {
    public static void main(String[] args) throws LogException {
        LogEntry entry = new LogEntry("Drinking", "TLOG");
        entry.setBody("After I read about the evils of drinking, I gave up reading.");
        entry.setTags("Autolog,Readme");
        
        long lognumber = entry.submit();
        
        if(lognumber == 0) {
            System.out.println("The log entry was queued.");
        } else {
            Comment comment = new Comment(lognumber, "24 hours in a day, 24 beers in a case.  Coincidence?");
            comment.submit();
            
            LogEntry revision = LogEntry.getLogEntry(lognumber, "Add notification");
            revision.setEmailNotify("ryans@jlab.org");
            revision.submit();
            
            System.out.println("Done with submission, comment, and revision.");
        }
    }
}
