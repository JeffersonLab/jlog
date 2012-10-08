package org.jlab.elog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.jlab.elog.exception.InvalidXMLException;
import org.jlab.elog.exception.LogIOException;
import org.jlab.elog.exception.LogRuntimeException;
import org.jlab.elog.exception.MalformedXMLException;
import org.jlab.elog.exception.SchemaUnavailableException;
import org.jlab.elog.util.IOUtil;
import org.jlab.elog.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An electronic log book log entry.
 *
 * @author ryans
 */
public class LogEntry extends LogItem {

    private static final String LOG_ENTRY_SCHEMA_URL;
    private static final String FETCH_URL;
    final XPathExpression titleExpression;
    final XPathExpression logbooksExpression;
    final XPathExpression logbookListExpression;
    final XPathExpression entrymakersExpression;
    final XPathExpression usernameListExpression;
    final XPathExpression stickyExpression;
    final XPathExpression tagsExpression;
    final XPathExpression tagListExpression;
    final XPathExpression referencesExpression;
    final XPathExpression revisionReasonExpression;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        LOG_ENTRY_SCHEMA_URL = bundle.getString("LOG_ENTRY_SCHEMA_URL");
        FETCH_URL = bundle.getString("FETCH_URL");
    }

    {
        try {
            titleExpression = xpath.compile("/Logentry/title");
            logbooksExpression = xpath.compile("/Logentry/Logbooks");
            logbookListExpression = xpath.compile("/Logentry/Logbooks/logbook");
            entrymakersExpression = xpath.compile("/Logentry/Entrymakers");
            usernameListExpression = xpath.compile(
                    "/Logentry/Entrymakers/Entrymaker/username");
            stickyExpression = xpath.compile("/Logentry/sticky");
            tagsExpression = xpath.compile("/Logentry/Tags");
            tagListExpression = xpath.compile("/Logentry/Tags/tag");
            referencesExpression = xpath.compile("/Logentry/References");
            revisionReasonExpression = xpath.compile(
                    "/Logentry/revision_reason");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to construct XML XPath query", e);
        }

    }

    /**
     * Construct a new LogEntry with the specified title and log books
     * designation.
     *
     * @param title The title
     * @param books A comma-separated list of log books
     * @throws LogRuntimeException If unable to construct a new LogEntry
     */
    public LogEntry(String title, String books) throws LogRuntimeException {
        super("Logentry");

        XMLUtil.appendElementWithText(doc, root, "title", title);

        Element logbooks = doc.createElement("Logbooks");
        root.appendChild(logbooks);
        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooks, "logbook",
                books);
    }

    /**
     * Construct a new LogEntry from the specified XML file.
     *
     * @param filePath The path to the XML file
     * @throws SchemaUnavailableException If the XML schema is unavailable
     * @throws MalformedXMLException If the XML is malformed
     * @throws InvalidXMLException If the XML is invalid
     * @throws LogIOException If unable to construct due to IO
     * @throws LogRuntimeException If unable to construct
     */
    public LogEntry(String filePath) throws SchemaUnavailableException,
            MalformedXMLException, InvalidXMLException, LogIOException,
            LogRuntimeException {
        try {
            doc = builder.parse(filePath);
            root = doc.getDocumentElement();
        } catch (SAXException e) {
            throw new MalformedXMLException("File is not well formed XML.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to parse XML file.", e);
        }

        // We could call builder.setSchema() and it would be a validating
        // parser, but then no way to differentiate Malformed vs Invalid
        validate();
    }

    /**
     * Factory method to obtain an existing LogEntry for viewing or revising.
     * If the intention is for viewing provide null for the reason.
     * 
     * @param lognumber The log number.
     * @param reason The reason for the revision
     * @return The LogEntry
     * @throws SchemaUnavailableException If the XML schema is unavailable
     * @throws MalformedXMLException If the XML is malformed
     * @throws InvalidXMLException If the XML is invalid
     * @throws LogIOException If unable to construct due to IO
     * @throws LogRuntimeException If unable to construct
     */
    public static LogEntry getLogEntry(long lognumber, String reason) 
            throws SchemaUnavailableException, MalformedXMLException, 
            InvalidXMLException, LogIOException, LogRuntimeException {
        String filePath = getGetPath(lognumber);
        LogEntry entry = new LogEntry(filePath);
        entry.setRevisionReason(reason);
        return entry;
    }

    static String getGetPath(long id) {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append(FETCH_URL);

        if (!FETCH_URL.endsWith("/")) {
            strBuilder.append("/");
        }

        strBuilder.append(id);
        strBuilder.append("/xml");

        return strBuilder.toString();
    }

    void setRevisionReason(String reason) {
        Element revisionReasonElement = null;

        try {
            revisionReasonElement = (Element) revisionReasonExpression.evaluate(
                    doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (revisionReasonElement == null) {
            revisionReasonElement = doc.createElement("revision_reason");
            root.appendChild(revisionReasonElement);
        }

        revisionReasonElement.setTextContent(reason);
    }

    /**
     * Add an array of log books to this log entry.
     * 
     * @param books The log books
     * @throws LogRuntimeException If unable to add log books 
     */
    public void addLogboks(String[] books) throws LogRuntimeException {
        addLogbooks(IOUtil.arrayToCSV(books));
    }

    /**
     * Add a comma-separated list of log books to this log entry.
     * 
     * @param books The log books
     * @throws LogRuntimeException If unable to add log books 
     */
    public void addLogbooks(String books) throws LogRuntimeException {
        if (books == null || books.isEmpty()) {
            return;
        }

        Element logbooksElement = null;

        try {
            logbooksElement = (Element) logbooksExpression.evaluate(doc, 
                    XPathConstants.NODE);

            if (logbooksElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooksElement, 
                "logbook", books);
    }

    /**
     * Replace the existing log books with the specified array.
     * 
     * @param books The log books
     * @throws LogRuntimeException If unable to set log books 
     */
    public void setLogbooks(String[] books) throws LogRuntimeException {
        setLogbooks(IOUtil.arrayToCSV(books));
    }

    /**
     * Replace the existing log books with the specified comma-separated-values.
     * 
     * @param books The log books
     * @throws LogRuntimeException If unable to set the log books 
     */
    public void setLogbooks(String books) throws LogRuntimeException {
        if (books == null) {
            books = "";
        }

        Element logbooksElement = null;

        try {
            logbooksElement = (Element) logbooksExpression.evaluate(doc, 
                    XPathConstants.NODE);

            if (logbooksElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        XMLUtil.removeChildren(logbooksElement);
        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooksElement, 
                "logbook", books);
    }

    /**
     * Return the log books as comma-separated-values.
     * 
     * @return The log books
     * @throws LogRuntimeException If unable to return the log books 
     */
    public String getLogbooksCSV() throws LogRuntimeException {
        return IOUtil.arrayToCSV(getLogbooks());
    }

    /**
     * Return the log books as an array.
     * 
     * @return The log books
     * @throws LogRuntimeException If unable to return the log books 
     */
    public String[] getLogbooks() throws LogRuntimeException {
        NodeList logbookElements = null;

        try {
            logbookElements = (NodeList) logbookListExpression.evaluate(doc, 
                    XPathConstants.NODESET);

            if (logbookElements == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        return XMLUtil.buildArrayFromText(logbookElements);
    }

    /**
     * Add an array of tags to the log entry.
     * 
     * @param tags The tags
     * @throws LogRuntimeException If unable to add tags 
     */
    public void addTags(String[] tags) throws LogRuntimeException {
        addTags(IOUtil.arrayToCSV(tags));
    }

    /**
     * Add a comma-separated list of tags to the log entry.
     * 
     * @param tags The tags
     * @throws LogRuntimeException If unable to add tags 
     */
    public void addTags(String tags) throws LogRuntimeException {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        Element tagsElement = null;

        try {
            tagsElement = (Element) tagsExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (tagsElement == null) {
            tagsElement = doc.createElement("Tags");
            root.appendChild(tagsElement);
        }

        XMLUtil.appendCommaDelimitedElementsWithText(doc, tagsElement, "tag", 
                tags);
    }

    /**
     * Replace the existing tags with the specified array of tags.
     * 
     * @param tags The tags
     * @throws LogRuntimeException If unable to set tags 
     */
    public void setTags(String[] tags) throws LogRuntimeException {
        setTags(IOUtil.arrayToCSV(tags));
    }

    /**
     * Replace the existing tags with the specified comma-separated-values.
     * 
     * @param tags The tags
     * @throws LogRuntimeException If unable to set the tags 
     */
    public void setTags(String tags) throws LogRuntimeException {
        Element tagsElement = null;

        try {
            tagsElement = (Element) tagsExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (tagsElement == null) {
            if (tags != null && !tags.isEmpty()) {
                tagsElement = doc.createElement("Tags");
                root.appendChild(tagsElement);
            }
        } else {
            XMLUtil.removeChildren(tagsElement);
        }

        if (tags != null && !tags.isEmpty()) {
            XMLUtil.appendCommaDelimitedElementsWithText(doc, tagsElement, 
                    "tag", tags);
        }
    }

    /**
     * Return the tags as comma-separated-values.
     * 
     * @return The tags
     * @throws LogRuntimeException If unable to return the tags 
     */
    public String getTagsCSV() throws LogRuntimeException {
        return IOUtil.arrayToCSV(getTags());
    }

    /**
     * Return the tags as an array.
     * 
     * @return The tags
     * @throws LogRuntimeException If unable to get the tags 
     */
    public String[] getTags() throws LogRuntimeException {
        NodeList tagElements = null;
        String[] tags;

        try {
            tagElements = (NodeList) tagListExpression.evaluate(doc, 
                    XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (tagElements != null) {
            tags = XMLUtil.buildArrayFromText(tagElements);
        } else {
            tags = new String[0];
        }

        return tags;
    }

    /**
     * Add a reference to this log entry.
     * 
     * @param ref The reference
     * @throws LogRuntimeException If unable to add a reference 
     */
    public void addReference(Reference ref) throws LogRuntimeException {
        if (ref == null) {
            return;
        }

        Element referencesElement = null;

        try {
            referencesElement = (Element) referencesExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (referencesElement == null) {
            referencesElement = doc.createElement("References");
            root.appendChild(referencesElement);
        }

        Element refElement = doc.createElement("reference");
        referencesElement.appendChild(refElement);

        refElement.setAttribute("type", ref.getType() == null ? "" : 
                ref.getType().name().toLowerCase());
        refElement.setTextContent(ref.getId());
    }

    /**
     * Return the references.
     * 
     * @return The references
     * @throws LogRuntimeException If unable to get the references 
     */
    public Reference[] getReferences() throws LogRuntimeException {
        List<Reference> references = new ArrayList<Reference>();

        Element referencesElement = null;

        try {
            referencesElement = (Element) referencesExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (referencesElement != null) {
            NodeList children = referencesElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                if (!(children.item(i) instanceof Element)) {
                    throw new LogRuntimeException(
                            "Unexpected node type in XML DOM; "
                            + "expected reference element.");
                }

                Element refElement = (Element) children.item(i);
                Reference.RefType type = null;
                String typeStr = refElement.getAttribute("type");

                if (typeStr != null && !typeStr.isEmpty()) {
                    try {
                        type = Reference.RefType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new LogRuntimeException(
                                "Unexpected RefType in XML reference.", e);
                    }
                }

                String id = refElement.getTextContent();
                references.add(new Reference(type, id));
            }
        }
        return references.toArray(new Reference[]{});
    }

    /**
     * Remove the references from the log entry.
     * 
     * @throws LogRuntimeException If unable to remove the references
     */
    public void deleteReferences() throws LogRuntimeException {
        Element referencesElement = null;

        try {
            referencesElement = (Element) referencesExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (referencesElement != null) {
            root.removeChild(referencesElement);
        }
    }

    /**
     * Set the title to a new value.
     * 
     * @param title The new title
     * @throws LogRuntimeException If unable to set the title 
     */
    public void setTitle(String title) throws LogRuntimeException {
        Element titleElement = null;

        try {
            titleElement = (Element) titleExpression.evaluate(doc, 
                    XPathConstants.NODE);

            if (titleElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        titleElement.setTextContent(title);
    }

    /**
     * Return the title.
     * 
     * @return The title
     * @throws LogRuntimeException If unable to get the title
     */
    public String getTitle() throws LogRuntimeException {
        Element titleElement = null;

        try {
            titleElement = (Element) titleExpression.evaluate(doc, 
                    XPathConstants.NODE);

            if (titleElement == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        return titleElement.getTextContent();
    }

    /**
     * Add an array of entry makers to the log entry.
     * 
     * @param entrymakers The entry makers
     * @throws LogRuntimeException If unable to add entry makers
     */
    public void addEntryMakers(String[] entrymakers)
            throws LogRuntimeException {
        addEntryMakers(IOUtil.arrayToCSV(entrymakers));
    }

    /**
     * Add a comma-separated list of entry makers to the log entry.
     * 
     * @param entrymakers The entry makers
     * @throws LogRuntimeException If unable to add entry makers
     */
    public void addEntryMakers(String entrymakers) throws LogRuntimeException {
        Element entrymakersElement = null;

        try {
            entrymakersElement = (Element) entrymakersExpression.evaluate(doc, 
                    XPathConstants.NODE);

        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (entrymakersElement == null) {
            entrymakersElement = doc.createElement("Entrymakers");
            root.appendChild(entrymakersElement);
        }

        XMLUtil.appendCommaDelimitedElementsWithGrandchildAndText(doc, 
                entrymakersElement, "Entrymaker", "username", entrymakers);
    }

    /**
     * Replace the entry makers with the specified array.
     * 
     * @param entrymakers the entry makers
     * @throws LogRuntimeException If unable to set the entry makers
     */
    public void setEntryMakers(String[] entrymakers)
            throws LogRuntimeException {
        setEntryMakers(IOUtil.arrayToCSV(entrymakers));
    }

    /**
     * Replace the entry makers with the specified comma-separated-values.
     * 
     * @param entrymakers The entry makers
     * @throws LogRuntimeException If unable to set the entry makers
     */
    public void setEntryMakers(String entrymakers) throws LogRuntimeException {
        if (entrymakers == null) {
            entrymakers = "";
        }

        Element entrymakersElement = null;

        try {
            entrymakersElement = (Element) entrymakersExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (entrymakersElement == null) {
            entrymakersElement = doc.createElement("Entrymakers");
            root.appendChild(entrymakersElement);
        } else {
            XMLUtil.removeChildren(entrymakersElement);
        }

        XMLUtil.appendCommaDelimitedElementsWithGrandchildAndText(doc, 
                entrymakersElement, "Entrymaker", "username", entrymakers);
    }

    /**
     * Return the entry makers as comma-separated-values.
     * 
     * @return The entry makers
     * @throws LogRuntimeException If unable to get the entry makers 
     */
    public String getEntryMakersCSV() throws LogRuntimeException {
        return IOUtil.arrayToCSV(getEntryMakers());
    }

    /**
     * Return the entry makers as an array.
     * 
     * @return The entry makers
     * @throws LogRuntimeException If unable to get the entry makers 
     */
    public String[] getEntryMakers() throws LogRuntimeException {
        NodeList usernameElements = null;

        try {
            usernameElements = (NodeList) usernameListExpression.evaluate(doc, 
                    XPathConstants.NODESET);

            if (usernameElements == null) {
                throw new LogRuntimeException("Element not found in XML DOM.");
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        return XMLUtil.buildArrayFromText(usernameElements);
    }

    /**
     * Set the sticky value of this log entry.
     * 
     * @param sticky true if sticky, false if not
     * @throws LogRuntimeException If unable to set the sticky value
     */
    public void setSticky(boolean sticky) throws LogRuntimeException {
        Element stickyElement = null;

        try {
            stickyElement = (Element) stickyExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
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

    /**
     * Return true if sticky, false if not.
     * 
     * @return true if sticky, false if not
     * @throws LogRuntimeException If unable to get the sticky value
     */
    public boolean isSticky() throws LogRuntimeException {
        boolean sticky = false;

        Element stickyElement = null;

        try {
            stickyElement = (Element) stickyExpression.evaluate(doc, 
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (stickyElement != null) {
            String value = stickyElement.getTextContent();

            try {
                int number = Integer.parseInt(value);

                if (number != 0) {
                    sticky = true;
                }
            } catch (NumberFormatException e) {
                throw new LogRuntimeException(
                        "Unable to obtain sticky due to non-numeric format.", e);
            }
        }

        return sticky;
    }

    @Override
    String getSchemaURL() {
        return LOG_ENTRY_SCHEMA_URL;
    }

    /**
     * Set the body to the specified plain text content.
     * 
     * @param content The content
     * @throws LogRuntimeException If unable to set the body
     */
    public void setBody(String content) {
        setBody(new Body(Body.ContentType.TEXT, content));
    }

    /**
     * Set the body to the specified content and content type.
     * 
     * @param content The content
     * @param type the type
     * @throws LogRuntimeException If unable to set the body
     */
    public void setBody(String content, Body.ContentType type) {
        setBody(new Body(type, content));
    }

    @Override
    public void setBody(Body body) throws LogRuntimeException {
        super.setBody(body);
    }
}
