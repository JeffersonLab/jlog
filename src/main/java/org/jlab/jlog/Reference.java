package org.jlab.jlog;

/**
 * A document reference for use in an electronic log book.
 * 
 * @author ryans
 */
public class Reference {
    
    private final String type;
    private final String id;
    
    /**
     * Construct a new Reference with the specified type and id.
     * 
     * @param type The type
     * @param id The id
     */
    public Reference(String type, String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * Return the reference type.
     * 
     * @return The type
     */
    public String getType() {
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
