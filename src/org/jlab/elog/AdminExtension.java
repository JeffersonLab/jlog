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
 * Wraps a LogItem to provide administrative capabilities. Generally these
 * features will only be needed on the server for administrator use.
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

    /**
     * Constructs a new AdminExtension with the specified LogItem.
     *
     * @param item The LogItem
     */
    public AdminExtension(LogItem item) {
        this.item = item;
        doc = item.getDocument();
        root = item.getRoot();
        xpath = item.getXPath();

        try {
            authorExpression = xpath.compile("/*/Author/username");
            createdExpression = xpath.compile("/*/created");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to construct XML XPath query", e);
        }
    }

    /**
     * Set the author username. Note: the author username is automatically
     * initialized to the submitter (program user). The server will ignore
     * values that do not match the submitter unless the submitter is an
     * administrator.
     *
     * @param author The author username
     * @throws LogRuntimeException If unable to set the author username
     */
    public void setAuthor(String author) throws LogRuntimeException {
        Element authorElement = null;

        try {
            authorElement = (Element) authorExpression.evaluate(doc,
                    XPathConstants.NODE);

            if (authorElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        authorElement.setTextContent(author);
    }

    /**
     * Set the created date/time. Note: the created date/time is automatically
     * initialized to the date/time at which the LogEntry object is
     * instantiated.
     *
     * @param created The created date/time.
     * @throws LogRuntimeException If unable to set the created date/time.
     */
    public void setCreated(GregorianCalendar created)
            throws LogRuntimeException {
        if (created == null) {
            created = new GregorianCalendar();
        }

        Element createdElement = null;

        try {
            createdElement = (Element) createdExpression.evaluate(doc,
                    XPathConstants.NODE);

            if (createdElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        createdElement.setTextContent(XMLUtil.toXMLFormat(created));
    }

    /**
     * Set the log number. This method provides a way to revise a LogEntry
     * without loading its original field values from the server first.
     *
     * @param lognumber The log number
     * @throws LogRuntimeException If unable to set the log number
     */
    public void setLogNumber(long lognumber) throws LogRuntimeException {
        item.setLogNumber(lognumber);
    }
}
