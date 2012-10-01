package org.jlab.elog;

import java.io.IOException;
import java.util.ResourceBundle;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.jlab.elog.exception.InvalidXMLException;
import org.jlab.elog.exception.LogIOException;
import org.jlab.elog.exception.LogRuntimeException;
import org.jlab.elog.exception.MalformedXMLException;
import org.jlab.elog.exception.SchemaUnavailableException;
import org.jlab.elog.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author ryans
 */
public class LogEntry extends LogItem {

    private static final String LOG_ENTRY_SCHEMA_URL;
    final XPathExpression titleExpression;
    final XPathExpression logbooksExpression;
    final XPathExpression logbookListExpression;
    final XPathExpression entrymakersExpression;
    final XPathExpression usernameListExpression;
    final XPathExpression stickyExpression;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        LOG_ENTRY_SCHEMA_URL = bundle.getString("LOG_ENTRY_SCHEMA_URL");
    }

    {
        try {
            titleExpression = xpath.compile("/Logentry/title");
            logbooksExpression = xpath.compile("/Logentry/Logbooks");
            logbookListExpression = xpath.compile("/Logentry/Logbooks/logbook");
            entrymakersExpression = xpath.compile("/Logentry/Entrymakers");
            usernameListExpression = xpath.compile("/Logentry/Entrymakers/Entrymaker/username");
            stickyExpression = xpath.compile("/Logentry/sticky");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to construct XML XPath query", e);
        }

    }

    public LogEntry(String title, String books) throws LogRuntimeException {
        super("Logentry");

        XMLUtil.appendElementWithText(doc, root, "title", title);

        Element logbooks = doc.createElement("Logbooks");
        root.appendChild(logbooks);
        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooks, "logbook", books);
    }

    public LogEntry(long id) throws LogRuntimeException {
        super("Logentry");

        throw new UnsupportedOperationException();
    }

    public LogEntry(String filePath) throws SchemaUnavailableException, MalformedXMLException, InvalidXMLException, LogIOException, LogRuntimeException {
        try {
            doc = builder.parse(filePath);
        } catch (SAXException e) {
            throw new MalformedXMLException("File is not well formed XML.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to parse XML file.", e);
        }

        validate(); // Alternatively we could call builder.setSchema() and it would be a validating parser (no way to differentiate Malformed vs Invalid though)
    }

    public void addLogbooks(String books) throws LogRuntimeException {
        if (books == null || books.isEmpty()) {
            return;
        }

        Element logbooksElement = null;

        try {
            logbooksElement = (Element) logbooksExpression.evaluate(doc, XPathConstants.NODE);

            if (logbooksElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooksElement, "logbook", books);
    }

    public void setLogbooks(String books) throws LogRuntimeException {
        if (books == null) {
            books = "";
        }

        Element logbooksElement = null;

        try {
            logbooksElement = (Element) logbooksExpression.evaluate(doc, XPathConstants.NODE);

            if (logbooksElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        XMLUtil.removeChildren(logbooksElement);
        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooksElement, "logbook", books);
    }

    public String getLogbooks() throws LogRuntimeException {
        NodeList logbookElements = null;

        try {
            logbookElements = (NodeList) logbookListExpression.evaluate(doc, XPathConstants.NODESET);

            if (logbookElements == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        return XMLUtil.buildCommaDelimitedFromText(logbookElements);
    }

    public void setTitle(String title) throws LogRuntimeException {
        Element titleElement = null;

        try {
            titleElement = (Element) titleExpression.evaluate(doc, XPathConstants.NODE);

            if (titleElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        titleElement.setTextContent(title);
    }

    public String getTitle() throws LogRuntimeException {
        Element titleElement = null;

        try {
            titleElement = (Element) titleExpression.evaluate(doc, XPathConstants.NODE);

            if (titleElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        return titleElement.getTextContent();
    }

    public void addEntryMakers(String entrymakers) throws LogRuntimeException {
        Element entrymakersElement = null;

        try {
            entrymakersElement = (Element) entrymakersExpression.evaluate(doc, XPathConstants.NODE);

        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (entrymakersElement == null) {
            entrymakersElement = doc.createElement("Entrymakers");
            root.appendChild(entrymakersElement);
        }

        XMLUtil.appendCommaDelimitedElementsWithGrandchildAndText(doc, entrymakersElement, "Entrymaker", "username", entrymakers);
    }

    public void setEntryMakers(String entrymakers) throws LogRuntimeException {
        if (entrymakers == null) {
            entrymakers = "";
        }

        Element entrymakersElement = null;

        try {
            entrymakersElement = (Element) entrymakersExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (entrymakersElement == null) {
            entrymakersElement = doc.createElement("Entrymakers");
            root.appendChild(entrymakersElement);
        } else {
            XMLUtil.removeChildren(entrymakersElement);
        }

        XMLUtil.appendCommaDelimitedElementsWithGrandchildAndText(doc, entrymakersElement, "Entrymaker", "username", entrymakers);
    }

    public String getEntryMakers() throws LogRuntimeException {
        NodeList usernameElements = null;

        try {
            usernameElements = (NodeList) usernameListExpression.evaluate(doc, XPathConstants.NODESET);

            if (usernameElements == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        return XMLUtil.buildCommaDelimitedFromText(usernameElements);
    }

    public void setSticky(boolean sticky) throws LogRuntimeException {
        Element stickyElement = null;

        try {
            stickyElement = (Element) stickyExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (stickyElement == null && sticky) {
            stickyElement = doc.createElement("sticky");
            root.appendChild(stickyElement);
            stickyElement.setTextContent("1");
        } else {
            if (sticky) {
                stickyElement.setTextContent("1");
            } else {
                root.removeChild(stickyElement);
            }
        }
    }

    public boolean isSticky() throws LogRuntimeException {
        boolean sticky = false;

        Element stickyElement = null;

        try {
            stickyElement = (Element) stickyExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (stickyElement != null) {
            String value = stickyElement.getTextContent();
            
            try {
                int number = Integer.parseInt(value);

                if (number != 0) {
                    sticky = true;
                }
            } catch (NumberFormatException e) {
                throw new LogRuntimeException("Unable to obtain sticky due to non-numeric format.", e);
            }
        }

        return sticky;
    }

    @Override
    String getSchemaURL() {
        return LOG_ENTRY_SCHEMA_URL;
    }
}
