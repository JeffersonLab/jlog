package org.jlab.elog;

/**
 *
 * @author ryans
 */
public class LogEntryAdminExtension extends AdminExtension {
    
    public LogEntryAdminExtension(LogEntry entry) {
        super(entry);
    }
    
    public void addComment() {
        throw new UnsupportedOperationException();
    }
}
