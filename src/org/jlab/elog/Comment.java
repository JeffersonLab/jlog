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
    
    public Comment() throws LogRuntimeException {
        super("Comment");
    }
    
    @Override
    String getSchemaURL() {
        return COMMENT_SCHEMA_URL;
    }
    
}
