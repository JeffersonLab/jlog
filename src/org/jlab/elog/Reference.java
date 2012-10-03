package org.jlab.elog;

/**
 * A document reference for use in an electronic log book.
 * 
 * @author ryans
 */
public class Reference {

    /**
     * The reference type.
     */
    public enum RefType {

        LOGBOOK, ATLIS, CTLIS, FELIST, HALIST, HBLIST, HCLIST, HDLIST, INSVACTL, TATL, PSSLOG
    };
    
    private final RefType type;
    private final String id;
    
    /**
     * Construct a new Reference with the specified type and id.
     * 
     * @param type The type
     * @param id The id
     */
    public Reference(RefType type, String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * Return the reference type.
     * 
     * @return The type
     */
    public RefType getType() {
        return type;
    }

    /**
     * Return the reference ID.
     * 
     * @return The ID
     */
    public String getId() {
        return id;
    }
}
