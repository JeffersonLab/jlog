package org.jlab.elog;

import java.util.GregorianCalendar;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.jlab.elog.exception.LogRuntimeException;
import org.jlab.elog.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author ryans
 */
abstract class AdminExtension {
    final LogItem item;
    final Document doc;
    final Element root;
    final XPath xpath;
    
    XPathExpression authorExpression;
    XPathExpression createdExpression;   
    
    public AdminExtension(LogItem item) {
        this.item = item;
        doc = item.getDocument();
        root = item.getRoot();
        xpath = item.getXPath();
        
        try {
            authorExpression = xpath.compile("/*/Author/username");
            createdExpression = xpath.compile("/*/created");            
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to construct XML XPath query", e);
        }         
    }
    
    public void setAuthor(String author) throws LogRuntimeException {
        Element authorElement = null;

        try {
            authorElement = (Element) authorExpression.evaluate(doc, XPathConstants.NODE);

            if (authorElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        authorElement.setTextContent(author);
    }
    
    public void setCreated(GregorianCalendar created) throws LogRuntimeException {
        if (created == null) {
            created = new GregorianCalendar();
        }

        Element createdElement = null;

        try {
            createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);

            if (createdElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        createdElement.setTextContent(XMLUtil.toXMLFormat(created));
    }    
    
    public void setLogNumber(Long lognumber) throws LogRuntimeException {
        item.setLogNumber(lognumber);
    }    
}
