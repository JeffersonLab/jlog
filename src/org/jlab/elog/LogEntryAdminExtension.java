package org.jlab.elog;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.jlab.elog.exception.LogRuntimeException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author ryans
 */
public class LogEntryAdminExtension extends AdminExtension {
    
    XPathExpression commentsExpression;
    
    public LogEntryAdminExtension(LogEntry entry) {
        super(entry);
        
        try {
            commentsExpression = xpath.compile("/LogEntry/Comments");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to construct XML XPath query", e);
        }        
    }
    
    public void addComment(Comment comment) {
        Element commentsElement = null;

        try {
            commentsElement = (Element) commentsExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }
        
        if(commentsElement == null) {
            commentsElement = doc.createElement("Comments");
            root.appendChild(commentsElement);
        }
        
        Node imported = doc.importNode(comment.getRoot(), true);
        
        commentsElement.appendChild(imported);
    }
}
