package org.jlab.elog;

/**
 *
 * @author ryans
 */
public class Reference {

    public enum RefType {

        LOGBOOK, ATLIS, CTLIS, FELIST, HALIST, HBLIST, HCLIST, HDLIST, INSVACTL, TATL, PSSLOG
    };
    
    private final RefType type;
    private final String id;
    
    public Reference(RefType type, String id) {
        this.type = type;
        this.id = id;
    }

    public RefType getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
