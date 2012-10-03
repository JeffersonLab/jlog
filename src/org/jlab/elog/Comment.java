package org.jlab.elog;

import java.util.ResourceBundle;
import org.jlab.elog.exception.LogRuntimeException;

/**
 * A log book comment.
 * 
 * @author ryans
 */
public class Comment extends LogItem {

    private static final String COMMENT_SCHEMA_URL;    
    
    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        COMMENT_SCHEMA_URL = bundle.getString("COMMENT_SCHEMA_URL");        
    }
    
    /**
     * Construct a new Comment with the specified log number and content of
     * type plain text.
     * 
     * @param lognumber The log number
     * @param content The content
     * @throws LogRuntimeException If unable to construct the comment 
     */
    public Comment(long lognumber, String content) throws LogRuntimeException {
        this(lognumber, new Body(Body.ContentType.TEXT, content));
    }
    
    /**
     * Construct a new Comment with the specified log number, content, and 
     * content type.
     * 
     * @param lognumber The log number
     * @param content The content
     * @param type The content type
     * @throws LogRuntimeException If unable to construct the comment
     */
    public Comment(long lognumber, String content, Body.ContentType type) 
            throws LogRuntimeException {
        this(lognumber, new Body(type, content));
    }
    
    /**
     * Construct a new Comment with the specified log number and Body.
     * 
     * @param lognumber The log number
     * @param body The Body
     * @throws LogRuntimeException If unable to construct the comment
     */
    public Comment(long lognumber, Body body) throws LogRuntimeException {
        super("Comment");
        setLogNumber(lognumber);
        setBody(body);
    }
    
    @Override
    String getSchemaURL() {
        return COMMENT_SCHEMA_URL;
    }
    
}
