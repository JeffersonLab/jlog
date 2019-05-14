package org.jlab.elog;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.jlab.elog.exception.AttachmentSizeException;
import org.jlab.elog.exception.InvalidXMLException;
import org.jlab.elog.exception.LogIOException;
import org.jlab.elog.exception.LogRuntimeException;
import org.jlab.elog.exception.MalformedXMLException;
import org.jlab.elog.exception.SchemaUnavailableException;
import org.jlab.elog.util.IOUtil;
import org.jlab.elog.util.SecurityUtil;
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
    final XPathExpression problemReportExpression;

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
            problemReportExpression = xpath.compile("/Logentry/ProblemReport");
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
     * @throws AttachmentSizeException If attachments cross a size limit
     * @throws LogRuntimeException If unable to construct
     */
    public LogEntry(String filePath) throws SchemaUnavailableException,
            MalformedXMLException, InvalidXMLException, LogIOException,
            AttachmentSizeException, LogRuntimeException {

        parse(filePath);

        // We could call builder.setSchema() and it would be a validating
        // parser, but then no way to differentiate Malformed vs Invalid
        //validate();

        checkAndTallyAttachmentSize();
    }

    private void parse(String filePath) throws MalformedXMLException, LogIOException {
        Properties props = Library.getConfiguration();
        boolean ignoreServerCert = "true".equals(props.getProperty("IGNORE_SERVER_CERT_ERRORS"));

        try {
            if (ignoreServerCert) {
                try {
                    SecurityUtil.disableServerCertificateCheck();
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    throw new LogRuntimeException(
                            "Unable to disable server certificate check", e);
                }
            }
            doc = builder.parse(filePath);
            root = doc.getDocumentElement();

            SecurityUtil.enableServerCertificateCheck();
        } catch (SAXException e) {
            throw new MalformedXMLException("File is not well formed XML.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to parse XML file.", e);
        } finally {
            SecurityUtil.enableServerCertificateCheck();
        }
    }

    /**
     * Set the problem report information. Use null to clear it.
     *
     * @param report The problem report information.
     */
    public void setProblemReport(ProblemReport report) {
        Element problemReportElement = null;

        try {
            problemReportElement = (Element) problemReportExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (report == null) {
            if (problemReportElement == null) {
                doc.removeChild(problemReportElement);
            }

            return;
        }

        if (problemReportElement == null) {
            problemReportElement = doc.createElement("ProblemReport");
            root.appendChild(problemReportElement);
        }

        problemReportElement.setAttribute("type", report.getType().name());

        Element needsAttentionElement = XMLUtil.getChildElementByName(problemReportElement,
                "needs_attention");

        if (needsAttentionElement == null) {
            needsAttentionElement = doc.createElement("needs_attention");
            problemReportElement.appendChild(needsAttentionElement);
        }

        needsAttentionElement.setTextContent(report.isNeedsAttention() ? "1" : "0");

        Element systemIdElement = XMLUtil.getChildElementByName(problemReportElement, "system_id");

        if (systemIdElement == null) {
            systemIdElement = doc.createElement("system_id");
            problemReportElement.appendChild(systemIdElement);
        }

        systemIdElement.setTextContent(String.valueOf(report.getSystemId()));

        Element groupIdElement = XMLUtil.getChildElementByName(problemReportElement, "group_id");

        if (groupIdElement == null) {
            groupIdElement = doc.createElement("group_id");
            problemReportElement.appendChild(groupIdElement);
        }

        groupIdElement.setTextContent(String.valueOf(report.getGroupId()));

        Element componentsElement = XMLUtil.getChildElementByName(problemReportElement,
                "Components");

        if (report.getComponentId() == null) {
            if (componentsElement != null) {
                problemReportElement.removeChild(componentsElement);
            }
        } else {
            if (componentsElement == null) {
                componentsElement = doc.createElement("Components");
                problemReportElement.appendChild(componentsElement);
            }

            Element componentIdElement = XMLUtil.getChildElementByName(componentsElement,
                    "component_id");

            if (componentIdElement == null) {
                componentIdElement = doc.createElement("component_id");
                componentsElement.appendChild(componentIdElement);
            }

            componentIdElement.setTextContent(String.valueOf(report.getComponentId()));
        }
    }

    /**
     * Get the problem report information.
     *
     * @return The problem report information or null if none
     */
    public ProblemReport getProblemReport() {
        Element problemReportElement = null;
        ProblemReport report = null;

        try {
            problemReportElement = (Element) problemReportExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (problemReportElement != null) {
            ProblemReportType type = ProblemReportType.valueOf(problemReportElement.getAttribute(
                    "type"));

            Element needsAttentionElement = XMLUtil.getChildElementByName(problemReportElement,
                    "needs_attention");

            if (needsAttentionElement == null) {
                throw new LogRuntimeException("Unexpected XML DOM structure; "
                        + "ProblemReport needsAttention element missing.");
            }

            boolean needsAttention = needsAttentionElement.getTextContent().equals("1");

            Element systemIdElement = XMLUtil.getChildElementByName(problemReportElement,
                    "system_id");

            if (systemIdElement == null) {
                throw new LogRuntimeException("Unexpected XML DOM structure; "
                        + "ProblemReport system_id element missing.");
            }

            int systemId;

            try {
                systemId = Integer.parseInt(systemIdElement.getTextContent());
            } catch (NumberFormatException e) {
                throw new LogRuntimeException(
                        "Unexpected XML DOM structure; ProblemReport system_id value"
                        + " is not an integer or is out-of-range",
                        e);
            }

            Element groupIdElement = XMLUtil.getChildElementByName(problemReportElement,
                    "group_id");

            if (groupIdElement == null) {
                throw new LogRuntimeException("Unexpected XML DOM structure; "
                        + "ProblemReport group_id element missing.");
            }

            int groupId;

            try {
                groupId = Integer.parseInt(groupIdElement.getTextContent());
            } catch (NumberFormatException e) {
                throw new LogRuntimeException(
                        "Unexpected XML DOM structure; ProblemReport group_id value"
                        + " is not an integer or is out-of-range",
                        e);
            }

            Element componentsElement = XMLUtil.getChildElementByName(problemReportElement,
                    "Components");

            Integer componentId = null;
            if (componentsElement != null) {
                Element componentIdElement = XMLUtil.getChildElementByName(componentsElement,
                        "component_id");

                if (componentIdElement != null) {
                    try {
                        componentId = Integer.parseInt(componentIdElement.getTextContent());
                    } catch (NumberFormatException e) {
                        throw new LogRuntimeException(
                                "Unexpected XML DOM structure; ProblemReport component_id value"
                                + " is not an integer or is out-of-range",
                                e);
                    }
                }
            }

            report = new ProblemReport(type, needsAttention, systemId, groupId, componentId);
        }

        return report;
    }

    /**
     * Factory method to obtain an existing LogEntry for viewing or revising. If
     * the intention is for viewing provide null for the reason.
     *
     * @param lognumber The log number.
     * @param reason The reason for the revision
     * @return The LogEntry
     * @throws SchemaUnavailableException If the XML schema is unavailable
     * @throws MalformedXMLException If the XML is malformed
     * @throws InvalidXMLException If the XML is invalid
     * @throws LogIOException If unable to construct due to IO
     * @throws AttachmentSizeException If attachments cross a size limit
     * @throws LogRuntimeException If unable to construct
     */
    public static LogEntry getLogEntry(long lognumber, String reason)
            throws SchemaUnavailableException, MalformedXMLException,
            InvalidXMLException, LogIOException, AttachmentSizeException,
            LogRuntimeException {

        String filePath = buildHttpGetUrl(lognumber);
        LogEntry entry = new LogEntry(filePath);
        entry.setRevisionReason(reason);
        return entry;
    }

    /**
     * Constructs the HTTP GET URL to use for fetching log entries based on the
     * log number and the FETCH_URL configuration property.
     *
     * @param lognumber The log number
     * @return The HTTP GET URL
     * @throws LogRuntimeException If unable to construct the URL
     */
    static String buildHttpGetUrl(long lognumber) throws LogRuntimeException {
        StringBuilder strBuilder = new StringBuilder();

        Properties props = Library.getConfiguration();
        String fetchURL = props.getProperty("FETCH_URL");

        if (fetchURL == null) {
            throw new LogRuntimeException(
                    "Property FETCH_URL not found.");
        }

        strBuilder.append(fetchURL);

        if (!fetchURL.endsWith("/")) {
            strBuilder.append("/");
        }

        strBuilder.append(lognumber);
        strBuilder.append("/xml");

        return strBuilder.toString();
    }

    /**
     * Sets the revision reason.
     *
     * @param reason The revision reason
     * @throws LogRuntimeException If unable to set the revision reason
     */
    void setRevisionReason(String reason) throws LogRuntimeException {
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
     * Add an array of log books to this log entry. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * logbooks.
     *
     * @param books The log books
     * @throws LogRuntimeException If unable to add log books
     */
    public void addLogboks(String[] books) throws LogRuntimeException {
        addLogbooks(IOUtil.arrayToCSV(books));
    }

    /**
     * Add a comma-separated list of log books to this log entry. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * logbooks.
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
     * Replace the existing log books with the specified array. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * logbooks.
     *
     * @param books The log books
     * @throws LogRuntimeException If unable to set log books
     */
    public void setLogbooks(String[] books) throws LogRuntimeException {
        setLogbooks(IOUtil.arrayToCSV(books));
    }

    /**
     * Replace the existing log books with the specified comma-separated-values.
     * See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * logbooks.
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
     * Add an array of tags to the log entry. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * tags.
     *
     * @param tags The tags
     * @throws LogRuntimeException If unable to add tags
     */
    public void addTags(String[] tags) throws LogRuntimeException {
        addTags(IOUtil.arrayToCSV(tags));
    }

    /**
     * Add a comma-separated list of tags to the log entry. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * tags.
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
     * Replace the existing tags with the specified array of tags. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * tags.
     *
     * @param tags The tags
     * @throws LogRuntimeException If unable to set tags
     */
    public void setTags(String[] tags) throws LogRuntimeException {
        setTags(IOUtil.arrayToCSV(tags));
    }

    /**
     * Replace the existing tags with the specified comma-separated-values. See
     * the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * tags.
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
     * Add a reference to this log entry. See the <a
     * href="../../../overview-summary.html">Overview</a> for a list of valid
     * reference types.
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

        refElement.setAttribute("type", ref.getType());
        refElement.setTextContent(ref.getId());
    }

    /**
     * Return the references.
     *
     * @return The references
     * @throws LogRuntimeException If unable to get the references
     */
    public Reference[] getReferences() throws LogRuntimeException {
        List<Reference> references = new ArrayList<>();

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
                String type = refElement.getAttribute("type");
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
    String getSchemaURL() throws LogRuntimeException {
        Properties props = Library.getConfiguration();

        String url = props.getProperty("LOG_ENTRY_SCHEMA_URL");

        if (url == null) {
            throw new LogRuntimeException(
                    "Property LOG_ENTRY_SCHEMA_URL not found.");
        }

        return url;
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
