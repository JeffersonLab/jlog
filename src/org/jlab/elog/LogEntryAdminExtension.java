package org.jlab.elog;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.jlab.elog.exception.LogRuntimeException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Wraps a LogEntry to provide administrative capabilities. Generally these
 * features will only be needed on the server for administrator use.
 * 
 * @author ryans
 */
public class LogEntryAdminExtension extends AdminExtension {
    
    XPathExpression commentsExpression;
    
    /**
     * Construct a new LogEntryAdminExtension with the specified LogEntry.
     * 
     * @param entry The log entry
     * @throws LogRuntimeException If unable to construct the 
     * LogEntryAdminExtension
     */
    public LogEntryAdminExtension(LogEntry entry) throws LogRuntimeException {
        super(entry);
        
        try {
            commentsExpression = xpath.compile("/LogEntry/Comments");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to construct XML XPath query", e);
        }        
    }
    
    /**
     * Add a Comment to this LogEntry.
     * 
     * @param comment The comment
     * @throws LogRuntimeException If unable to add the comment
     */
    public void addComment(Comment comment) throws LogRuntimeException {
        Element commentsElement = null;

        try {
            commentsElement = (Element) commentsExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }
        
        if(commentsElement == null) {
            commentsElement = doc.createElement("Comments");
            root.appendChild(commentsElement);
        }
        
        Node imported = doc.importNode(comment.getRoot(), true);
        
        commentsElement.appendChild(imported);
    }
}
