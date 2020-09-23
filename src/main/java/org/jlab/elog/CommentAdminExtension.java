package org.jlab.elog;

/**
 * Wraps a Comment to provide administrative capabilities. Generally these
 * features will only be needed on the server for administrator use.
 * 
 * @author ryans
 */
public class CommentAdminExtension extends AdminExtension {
    /**
     * Construct a new CommentAdminExtension for the specified Comment.
     * 
     * @param comment The Comment 
     */
    public CommentAdminExtension(Comment comment) {
        super(comment);
    }
}
