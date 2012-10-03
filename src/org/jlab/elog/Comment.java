package org.jlab.elog;

import java.util.ResourceBundle;
import org.jlab.elog.exception.LogRuntimeException;

/**
 *
 * @author ryans
 */
public class Comment extends LogItem {

    private static final String COMMENT_SCHEMA_URL;    
    
    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        COMMENT_SCHEMA_URL = bundle.getString("COMMENT_SCHEMA_URL");        
    }
    
    public Comment(long lognumber, String content) {
        this(lognumber, new Body(Body.ContentType.TEXT, content));
    }
    
    public Comment(long lognumber, String content, Body.ContentType type) {
        this(lognumber, new Body(type, content));
    }
    
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
