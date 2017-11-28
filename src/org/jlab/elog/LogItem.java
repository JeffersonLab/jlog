package org.jlab.elog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jlab.elog.exception.AttachmentSizeException;
import org.jlab.elog.exception.InvalidXMLException;
import org.jlab.elog.exception.LogCertificateException;
import org.jlab.elog.exception.LogException;
import org.jlab.elog.exception.LogIOException;
import org.jlab.elog.exception.LogRuntimeException;
import org.jlab.elog.exception.SchemaUnavailableException;
import org.jlab.elog.util.IOUtil;
import org.jlab.elog.util.SecurityUtil;
import org.jlab.elog.util.SystemUtil;
import org.jlab.elog.util.XMLUtil;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An item that can be submitted to the electronic log book. An item can be
 * serialized to XML for data transfer and then submitted via one of two
 * methods: (1) HTTP PUT request, (2) write to file system queue directory. This
 * API requires that submissions must first attempt the HTTP PUT request, and
 * then may optionally attempt the queue submission.
 *
 * @author ryans
 */
abstract class LogItem {

    private static final Logger logger = Logger.getLogger(
            LogItem.class.getName());
    private static final String PEM_FILE_NAME = ".elogcert";
    static final boolean VERIFY_SERVER = false;
    private static final FileNameMap mimeMap = URLConnection.getFileNameMap();
    LogException submitException = null;
    Document doc;
    Element root;
    DatatypeFactory typeFactory;
    DocumentBuilder builder;
    XPath xpath;
    XPathExpression lognumberExpression;
    XPathExpression lognumberTextExpression;
    XPathExpression createdExpression;
    XPathExpression bodyExpression;
    XPathExpression attachmentsExpression;
    XPathExpression authorTextExpression;
    XPathExpression notificationsExpression;
    XPathExpression notificationListExpression;
    XPathExpression responseStatusExpression;
    XPathExpression responseMessageExpression;
    XPathExpression responseLognumberExpression;
    long totalAttachmentBytes = 0;

    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LogRuntimeException(
                    "Unable to obtain XML document builder.", e);
        }

        try {
            typeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new LogRuntimeException(
                    "Unable to obtain XML datatype factory.", e);
        }

        XPathFactory xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();

        try {
            lognumberExpression = xpath.compile("/*/lognumber");
            lognumberTextExpression = xpath.compile("/*/lognumber/text()");
            createdExpression = xpath.compile("/*/created");
            bodyExpression = xpath.compile("/*/body");
            attachmentsExpression = xpath.compile("/*/Attachments");
            authorTextExpression = xpath.compile("/*/Author/username/text()");
            notificationsExpression = xpath.compile("/*/Notifications");
            notificationListExpression = xpath.compile(
                    "/*/Notifications/email");
            responseStatusExpression = xpath.compile("/Response/@stat");
            responseMessageExpression = xpath.compile("/Response/msg/text()");
            responseLognumberExpression = xpath.compile("/Response/lognumber");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to construct XML XPath query", e);
        }
    }

    /**
     * Construct a new LogItem, but do not initialize a DOM (Document). Note:
     * the caller should do the initialization of the DOM.
     *
     * @throws LogRuntimeException If unable to initialize the LogItem
     */
    public LogItem() throws LogRuntimeException {
    }

    /**
     * Construct a new LogItem with the specified root element tag name.
     *
     * @param rootTagName The root tag name
     * @throws LogRuntimeException If unable to initialize the LogItem
     */
    public LogItem(String rootTagName) throws LogRuntimeException {

        doc = builder.newDocument();

        root = doc.createElement(rootTagName);
        doc.appendChild(root);

        XMLUtil.appendElementWithText(doc, root, "created",
                XMLUtil.toXMLFormat(new GregorianCalendar()));

        Element authorElement = doc.createElement("Author");
        root.appendChild(authorElement);
        XMLUtil.appendElementWithText(doc, authorElement, "username",
                System.getProperty("user.name"));
    }

    /**
     * Check the attachment length against the limit rules defined in the
     * configuration.
     *
     * @param length The length to check
     * @throws AttachmentSizeException If an attachment size limit is crossed
     * @throws LogRuntimeException If unable to check the length
     */
    void checkAttachmentSize(long length) throws AttachmentSizeException,
            LogRuntimeException {
        Properties props = Library.getConfiguration();

        String singleFileLimitStr = props.getProperty(
                "ATTACH_SINGLE_MAX_BYTES");

        if (singleFileLimitStr == null) {
            throw new LogRuntimeException(
                    "Property ATTACH_SINGLE_MAX_BYTES not found.");
        }

        long singleFileLimit = 0;

        try {
            singleFileLimit = Long.parseLong(singleFileLimitStr);
        } catch (NumberFormatException e) {
            throw new LogRuntimeException(
                    "ATTACH_SINGLE_MAX_BYTES must be a number.", e);
        }

        if (length > singleFileLimit) {
            throw new AttachmentSizeException(
                    "The maximim attachment file size of "
                    + singleFileLimit
                    / 1024 / 1024 + " MB has been exceeded.");
        }

        String totalFileLimitStr = props.getProperty(
                "ATTACH_TOTAL_MAX_BYTES");

        if (totalFileLimitStr == null) {
            throw new LogRuntimeException(
                    "Property ATTACH_TOTAL_MAX_BYTES not found.");
        }

        long totalFileLimit = 0;

        try {
            totalFileLimit = Long.parseLong(totalFileLimitStr);
        } catch (NumberFormatException e) {
            throw new LogRuntimeException(
                    "ATTACH_TOTAL_MAX_BYTES must be a number.", e);
        }

        if (length + totalAttachmentBytes > totalFileLimit) {
            throw new AttachmentSizeException(
                    "The maximim total size for all attachments of "
                    + totalFileLimit
                    / 1024 / 1024 + " MB has been exceeded.");
        }
    }

    /**
     * Return the length of the specified attachment. This method writes the
     * attachment out to a temp directly and checks the length. Base64
     * attachments are decoded and URL attachments are downloaded.
     *
     * @param attachment The attachment
     * @return The length
     * @throws LogIOException If unable to obtain length due to IO
     */
    long getAttachmentLength(Attachment attachment) throws LogIOException {
        long length = 0;
        String prefix = "jlogattachment";
        String suffix = ".tmp";

        File file = null;

        try (InputStream in = attachment.getData()) {

            file = File.createTempFile(prefix, suffix);

            try (OutputStream out = new FileOutputStream(file)) {
                IOUtil.copy(in, out);
            }
        } catch (IOException e) {
            throw new LogIOException(
                    "Unable to write attachment to tmp directory for length measurement.",
                    e);
        } finally {
            IOUtil.deleteQuietly(file);
        }

        return length;
    }

    /**
     * Check that the attachments in the log item are within limits and update
     * the count on total attachment size so that further checking can be done
     * when attachments are added.
     *
     * @throws AttachmentSizeException If an attachment size limit is crossed
     * @throws LogIOException If unable to check and tally due to IO
     *
     */
    void checkAndTallyAttachmentSize() throws AttachmentSizeException,
            LogIOException {
        Attachment[] attachments = getAttachments();

        for (Attachment attachment : attachments) {
            long length = getAttachmentLength(attachment);
            checkAttachmentSize(length);
            totalAttachmentBytes += length;
        }
    }

    /**
     * Add a file attachment with an empty caption and a hastily guessed mime
     * type. The mime type is guessed by using the readily available
     * java.net.URLConnection file name map, which simply looks at file
     * extension and compares with the very limited lookup file at:
     * <verbatim>[JRE_HOME]\lib\content-types.properties</verbatim>
     *
     * @param filepath The file path
     * @throws AttachmentSizeException If the attachment crosses a size limit
     * @throws LogIOException If unable to add the attachment due to IO
     * @throws LogRuntimeException If unable to add the attachment
     */
    public void addAttachment(String filepath) throws AttachmentSizeException,
            LogIOException, LogRuntimeException {
        addAttachment(filepath, "", mimeMap.getContentTypeFor(filepath));
    }

    /**
     * Add a file attachment with the specified caption and a hastily guessed
     * mime type. The mime type is guessed by using the readily available
     * java.net.URLConnection file name map, which simply looks at file
     * extension and compares with the very limited lookup file at:
     * <verbatim>[JRE_HOME]\lib\content-types.properties</verbatim>
     *
     * @param filepath The file path
     * @param caption The caption
     * @throws AttachmentSizeException If the attachment crosses a size limit
     * @throws LogIOException If unable to add the attachment due to IO
     * @throws LogRuntimeException If unable to add the attachment
     */
    public void addAttachment(String filepath, String caption)
            throws AttachmentSizeException,
            LogIOException, LogRuntimeException {
        addAttachment(filepath, caption, mimeMap.getContentTypeFor(filepath));
    }

    /**
     * Add a file attachment with the specified caption and mime type.
     *
     * @param filepath The file path
     * @param caption The caption
     * @param mimeType The mime type
     * @throws AttachmentSizeException If the attachment crosses a size limit
     * @throws LogIOException If unable to add the attachment due to IO
     * @throws LogRuntimeException If unable to add the attachment
     */
    public void addAttachment(String filepath, String caption, String mimeType)
            throws AttachmentSizeException, LogIOException,
            LogRuntimeException {
        String data = null;
        Element attachmentsElement = null;

        File file = new File(filepath);

        checkAttachmentSize(file.length());

        try {
            data = XMLUtil.encodeBase64(IOUtil.fileToBytes(file));
            attachmentsElement = (Element) attachmentsExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (IOException e) {
            throw new LogIOException("Unable to access attachment file.", e);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (attachmentsElement == null) {
            attachmentsElement = doc.createElement("Attachments");
            root.appendChild(attachmentsElement);
        }

        Element attachmentElement = doc.createElement("Attachment");
        attachmentsElement.appendChild(attachmentElement);
        XMLUtil.appendElementWithText(doc, attachmentElement, "caption",
                caption);
        XMLUtil.appendElementWithText(doc, attachmentElement, "filename",
                file.getName());
        XMLUtil.appendElementWithText(doc, attachmentElement, "type", mimeType);
        Element dataElement = XMLUtil.appendElementWithText(doc,
                attachmentElement, "data", data);
        dataElement.setAttribute("encoding", "base64");
        totalAttachmentBytes += file.length();
    }

    /**
     * Return the file attachments.
     *
     * @return The attachments
     * @throws LogRuntimeException If unable to return the attachments
     */
    public Attachment[] getAttachments() throws LogRuntimeException {
        List<Attachment> attachments = new ArrayList<Attachment>();
        Element attachmentsElement = null;

        try {
            attachmentsElement = (Element) attachmentsExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (attachmentsElement != null) {
            NodeList children = attachmentsElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    attachments.add(new Attachment((Element) children.item(i)));
                }
            }
        }

        return attachments.toArray(new Attachment[]{});
    }

    /**
     * Remove the file attachments.
     *
     * @throws LogRuntimeException If unable to remove the file attachments
     */
    public void deleteAttachments() throws LogRuntimeException {
        Element attachmentsElement = null;

        try {
            attachmentsElement = (Element) attachmentsExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (attachmentsElement != null) {
            XMLUtil.removeChildren(attachmentsElement);
        }

        totalAttachmentBytes = 0;
    }

    /**
     * Set the comma-separated list of email addresses used for notification.
     *
     * @param addresses The email addresses
     * @throws LogRuntimeException If unable to set the email addresses
     */
    public void setEmailNotify(String addresses) throws LogRuntimeException {
        Element notificationsElement = null;

        try {
            notificationsElement = (Element) notificationsExpression
                    .evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (notificationsElement == null) {
            if (addresses != null && !addresses.isEmpty()) {
                notificationsElement = doc.createElement("Notifications");
                root.appendChild(notificationsElement);
            }
        } else {
            XMLUtil.removeChildren(notificationsElement);
        }

        if (addresses != null && !addresses.isEmpty()) {
            XMLUtil.appendCommaDelimitedElementsWithText(doc,
                    notificationsElement, "email", addresses);
        }
    }

    /**
     * Set the array of email addresses used for notification.
     *
     * @param addresses The email addresses
     * @throws LogRuntimeException If unable to set the email addresses
     */
    public void setEmailNotify(String[] addresses) throws LogRuntimeException {
        setEmailNotify(IOUtil.arrayToCSV(addresses));
    }

    /**
     * Return the comma-separated list of email addresses.
     *
     * @return The email addresses
     * @throws LogRuntimeException If unable to get the email addresses
     */
    public String getEmailNotifyCSV() throws LogRuntimeException {
        return IOUtil.arrayToCSV(getEmailNotify());
    }

    /**
     * Return the array of email addresses.
     *
     * @return The email addresses
     * @throws LogRuntimeException If unable to get the email addresses
     */
    public String[] getEmailNotify() throws LogRuntimeException {
        String[] addresses;
        NodeList notificationElements = null;

        try {
            notificationElements = (NodeList) notificationListExpression
                    .evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (notificationElements != null) {
            addresses = XMLUtil.buildArrayFromText(notificationElements);
        } else {
            addresses = new String[0];
        }

        return addresses;
    }

    /**
     * Return the author username.
     *
     * @return The author username
     * @throws LogRuntimeException If unable to get the author username
     */
    public String getAuthor() throws LogRuntimeException {
        String author = null;

        try {
            author = (String) authorTextExpression.evaluate(doc,
                    XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        return author;
    }

    /**
     * Set the log number.
     *
     * @param lognumber The log number
     * @throws LogRuntimeException If unable to set the log number
     *
     */
    void setLogNumber(long lognumber) throws LogRuntimeException {
        Element lognumberElement = null;

        try {
            lognumberElement = (Element) lognumberExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (lognumberElement == null) {
            lognumberElement = doc.createElement("lognumber");
            root.appendChild(lognumberElement);
        }

        lognumberElement.setTextContent(String.valueOf(lognumber));
    }

    /**
     * Return the log number or null if none assigned.
     *
     * @return The log number or null
     * @throws LogRuntimeException If unable to get the log number
     */
    public Long getLogNumber() throws LogRuntimeException {
        Long lognumber = null;
        String lognumberStr = null;

        try {
            lognumberStr = (String) lognumberTextExpression.evaluate(doc,
                    XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        // NOTE: C++ impl throws exception if null
        if (lognumberStr != null && !lognumberStr.isEmpty()) {
            try {
                lognumber = Long.parseLong(lognumberStr);
            } catch (NumberFormatException e) {
                throw new LogRuntimeException(
                        "Unable to obtain log number due to non-numeric format.", e);
            }
        }

        return lognumber;
    }

    /**
     * Return the created date/time.
     *
     * @return The created date/time
     * @throws LogRuntimeException If unable to get the created date/time
     */
    public GregorianCalendar getCreated() throws LogRuntimeException {
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

        String createdStr = createdElement.getTextContent();

        return XMLUtil.toGregorianCalendar(createdStr);
    }

    /**
     * Return the body.
     *
     * @return The body
     * @throws LogRuntimeException If unable to get the body
     */
    public Body getBody() throws LogRuntimeException {
        Body body = null;
        Element bodyElement = null;

        try {
            bodyElement = (Element) bodyExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }

        if (bodyElement != null) {
            String content = bodyElement.getTextContent();
            String typeStr = bodyElement.getAttribute("type");
            Body.ContentType type = Body.ContentType.TEXT;

            if (typeStr != null && !typeStr.isEmpty()) {
                try {
                    type = Body.ContentType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new LogRuntimeException(
                            "Unexpected ContentType in XML body.", e);
                }
            }

            body = new Body(type, content);
        }

        return body;
    }

    /**
     * Set the body.
     *
     * @param body The body
     * @throws LogRuntimeException If unable to set the body
     */
    void setBody(Body body) throws LogRuntimeException {
        if (body == null) {
            body = new Body(Body.ContentType.TEXT, "");
        }

        Element bodyElement = null;

        try {
            bodyElement = (Element) bodyExpression.evaluate(doc,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        }

        if (bodyElement != null) {
            root.removeChild(bodyElement);
        }

        if (body.getContent() != null && !body.getContent().isEmpty()) {
            bodyElement = doc.createElement("body");
            root.appendChild(bodyElement);

            if (body.getType() == Body.ContentType.HTML) {
                bodyElement.setAttribute("type", "html");
            }

            CDATASection data = doc.createCDATASection(body.getContent());
            bodyElement.appendChild(data);
        }
    }

    /**
     * Return the XML.
     *
     * @return The XML
     * @throws LogRuntimeException If unable to get the XML
     */
    public String getXML() throws LogRuntimeException {
        String xml = null;

        try {
            xml = XMLUtil.getXML(doc);
        } catch (TransformerConfigurationException e) {
            throw new LogRuntimeException(
                    "Unable to obtain XML document transformer.", e);
        } catch (TransformerException e) {
            throw new LogRuntimeException(
                    "Unable to transform XML document.", e);
        }

        return xml;
    }

    /**
     * Return the URL to the schema needed for validation of this log book item.
     *
     * @return The URL
     * @throws LogRuntimeException If unable to obtain schema URL
     */
    abstract String getSchemaURL() throws LogRuntimeException;

    /**
     * Validate the DOM that makes up this log item using the log item schema.
     *
     * @throws SchemaUnavailableException If the schema is unavailable
     * @throws InvalidXMLException If the XML generated from the DOM is invalid
     * @throws LogIOException If unable to validate due to IO
     */
    void validate() throws SchemaUnavailableException, InvalidXMLException,
            LogIOException {
        Schema schema = null;

        try {
            URL schemaURL = new URL(getSchemaURL());
            SchemaFactory factory = SchemaFactory.newInstance(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = factory.newSchema(schemaURL);
        } catch (MalformedURLException e) {
            throw new SchemaUnavailableException("Schema URL malformed.", e);
        } catch (SAXException e) {
            throw new SchemaUnavailableException("Unable to parse schema.", e);
        }

        Validator validator = schema.newValidator();

        DOMSource source = new DOMSource(doc);

        try {
            validator.validate(source);
        } catch (SAXException e) {
            throw new InvalidXMLException(
                    "The XML failed to validate against the schema.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to validate XML.", e);
        }
    }

    /**
     * Parse the XML response sent from the server after an HTTP PUT request.
     *
     * @param is The InputStream containing the response
     * @return The log number contained within the response
     * @throws LogIOException If unable to parse due to IO
     * @throws LogRuntimeException If unable to parse
     */
    long parseServerResponse(InputStream is) throws LogIOException,
            LogRuntimeException {
        long id;

        try {
            Document response = builder.parse(is);

            String status = (String) responseStatusExpression.evaluate(
                    response, XPathConstants.STRING);
            String message = (String) responseMessageExpression.evaluate(
                    response, XPathConstants.STRING);
            String lognumberStr = (String) responseLognumberExpression.evaluate(
                    response, XPathConstants.STRING);

            if (status == null || status.isEmpty()) {
                throw new LogIOException("Unrecognized Response from server.");
            }

            if (!"ok".equals(status)) {
                throw new LogIOException("Submission Failed: " + message);
            }

            id = Long.parseLong(lognumberStr);

            //System.out.println("Status: " + status);
            //System.out.println("Message: " + message);
            //System.out.println(XMLUtil.getXML(response));
        } catch (IOException e) {
            throw new LogIOException("Unable to parse response.", e);
        } catch (SAXException e) {
            throw new LogIOException("Unable to parse response.", e);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException(
                    "Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException(
                    "Unexpected node type in XML DOM.", e);
        } catch (NumberFormatException e) {
            throw new LogIOException("Log number not found in response.", e);
        }

        return id;
    }

    /**
     * Perform the HTTP PUT request to the server with the log item.
     *
     * @param pemFilePath The path to the client certificate file
     * @return The log number returned in the server response
     * @throws LogIOException If unable to perform the request due to IO
     * @throws LogCertificateException If unable to perform the request due to
     * certificate
     * @throws LogRuntimeException If unable to perform the request
     */
    long performHttpPutToServer(String pemFilePath) throws LogIOException,
            LogCertificateException, LogRuntimeException {
        long id;

        String xml = getXML();

        HttpsURLConnection con;

        try {
            URL url = new URL(buildHttpPutUrl());
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(
                    pemFilePath, VERIFY_SERVER));
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.setChunkedStreamingMode(0);
            con.setRequestProperty("Expect", "100-Continue");
            con.connect();

            try (OutputStream out = con.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8")) {
                writer.write(xml);
            }

            try (InputStream error = con.getErrorStream()) {
                if (error != null) {
                    String errorMsg = IOUtil.streamToString(error, "UTF-8");
                    throw new IOException(errorMsg);
                }
            }

            try (InputStream is = con.getInputStream()) {
                id = parseServerResponse(is);
            }
        } catch (MalformedURLException e) {
            throw new LogIOException(
                    "Invalid submission URL: check config file.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to submit to ELOG server.", e);
        } catch (ClassCastException e) {
            throw new LogIOException(
                    "Expected HTTP URL; check config file.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new LogCertificateException(
                    "Invalid SSL certificate algorithm.", e);
        } catch (CertificateException e) {
            throw new LogCertificateException(
                    "Unable to obtain SSL connection due to certificate error.",
                    e);
        } catch (KeyStoreException e) {
            throw new LogCertificateException(
                    "Unable to obtain SSL connection due to certificate error.",
                    e);
        } catch (KeyManagementException e) {
            throw new LogCertificateException(
                    "Unable to obtain SSL connection due to certificate error.",
                    e);
        } catch (UnrecoverableKeyException e) {
            throw new LogCertificateException(
                    "Unable to obtain SSL connection due to certificate error.",
                    e);
        } catch (InvalidKeySpecException e) {
            throw new LogCertificateException(
                    "Unable to obtain SSL connection due to certificate error.",
                    e);
        }

        return id;
    }

    /**
     * Return the Document object for the Document Object Model (DOM).
     *
     * @return The Document
     */
    Document getDocument() {
        return doc;
    }

    /**
     * Return the root Element of this log item.
     *
     * @return The root (document) Element
     */
    Element getRoot() {
        return root;
    }

    /**
     * Return the XPath object used to compile XPath expressions.
     *
     * @return The XPath object
     */
    XPath getXPath() {
        return xpath;
    }

    /**
     * Construct the HTTP PUT URL to use when submitting log entries and
     * comments using the the SUBMIT_URL configuration property and the
     * generateXMLFilename method.
     *
     * @return The HTTP PUT URL
     * @throws LogRuntimeException If unable to construct
     */
    String buildHttpPutUrl() throws LogRuntimeException {
        StringBuilder strBuilder = new StringBuilder();

        Properties props = Library.getConfiguration();
        String submitURL = props.getProperty("SUBMIT_URL");

        if (submitURL == null) {
            throw new LogRuntimeException(
                    "Property SUBMIT_URL not found.");
        }

        strBuilder.append(submitURL);

        if (!submitURL.endsWith("/")) {
            strBuilder.append("/");
        }

        strBuilder.append(generateXMLFilename());

        return strBuilder.toString();
    }

    /**
     * Returns the default client certificate path.
     *
     * @return The default client certificate path
     */
    String getDefaultCertificatePath() {
        return new File(System.getProperty("user.home"),
                PEM_FILE_NAME).getAbsolutePath();
    }

    /**
     * Submit the log item using the queue mechanism as a fallback and return
     * the log number. If the log number is zero then the submission was queued
     * instead of being consumed directly by the server.
     *
     * @return The log number, zero means queued
     * @throws InvalidXMLException If unable to submit due to invalid XML
     * @throws LogIOException If unable to submit due to IO
     */
    public long submit() throws InvalidXMLException, LogIOException {
        return submit(getDefaultCertificatePath());
    }

    /**
     * Submit the log item using the queue mechanism as a fallback and using the
     * specified client certificate and return the log number. If the log number
     * is zero then the submission was queued instead of being consumed directly
     * by the server. You can use the whyQueued method to obtain the
     * LogException encountered if any while attempting to submit directly to
     * the server.
     *
     * @param pemFilePath The path to the PEM-encoded client certificate
     * @return The log number, zero means queued
     * @throws InvalidXMLException If unable to submit to do invalid XML
     * @throws LogIOException If unable to submit due to IO
     */
    public long submit(String pemFilePath) throws InvalidXMLException,
            LogIOException {
        long id = 0L;

        try {
            id = performHttpPutToServer(pemFilePath);
        } catch (LogException e) {
            submitException = e;
            queue();
        }

        return id;
    }

    /**
     * Submit the log item using only direct submission to the server using the
     * client certificate named <em>.elogcert</em> in the user's home directory
     * and return the log number. If an error occurs during submission then an
     * Exception will be thrown instead of falling back to the queue method.
     *
     * @return The log number
     * @throws LogIOException If unable to submit due to IO
     * @throws LogCertificateException If unable to submit due to certificate
     * @throws LogRuntimeException If unable to submit
     */
    public long submitNow() throws LogIOException, LogCertificateException,
            LogRuntimeException {
        return performHttpPutToServer(getDefaultCertificatePath());
    }

    /**
     * Submit the log item using only direct submission to the server with the
     * specified client certificate and return the log number. If an error
     * occurs during submission then an Exception will be thrown instead of
     * falling back to the queue method.
     *
     * @param pemFilePath The path to the PEM-encoded client certificate
     * @return The log number
     * @throws LogIOException If unable to submit due to IO
     * @throws LogCertificateException If unable to submit due to certificate
     * @throws LogRuntimeException If unable to submit
     */
    public long submitNow(String pemFilePath) throws LogIOException, LogCertificateException,
            LogRuntimeException {
        return performHttpPutToServer(pemFilePath);
    }

    /**
     * Generate an XML filename for log entries and comments submission. The
     * format expected by the logbook server is:
     * [timestamp]_[pid]_[hostname]_[random].xml
     *
     * @return The filename
     */
    String generateXMLFilename() {
        StringBuilder filenameBuilder = new StringBuilder();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HHmmss_");

        String date = formatter.format(new Date());

        Integer pid = SystemUtil.getJVMProcessId();

        if (pid == null) {
            pid = 0;
        }

        String hostname = SystemUtil.getHostname();

        if (hostname == null) {
            hostname = "unknown";
        }

        int random = (int) (Math.random() * 10000);

        filenameBuilder.append(date);
        filenameBuilder.append(pid);
        filenameBuilder.append("_");
        filenameBuilder.append(hostname);
        filenameBuilder.append("_");
        filenameBuilder.append(random);
        filenameBuilder.append(".xml");

        return filenameBuilder.toString();
    }

    /**
     * Return the queue path. The queue path is determined by first looking at
     * the QUEUE_PATH configuration property. If it is defined then it is used,
     * if not, then the DEFAULT_WINDOWS_QUEUE_PATH or DEFAULT_UNIX_QUEUE_PATH
     * configuration property is used based on the detected OS.
     *
     * @return The queue path
     */
    String getQueuePath() {
        Properties props = Library.getConfiguration();

        String queuePath = props.getProperty("QUEUE_PATH");

        if (queuePath == null || queuePath.isEmpty()) {
            if (SystemUtil.isWindows()) {
                queuePath = props.getProperty("DEFAULT_WINDOWS_QUEUE_PATH");
            } else {
                queuePath = props.getProperty("DEFAULT_UNIX_QUEUE_PATH");
            }
        }

        if (queuePath == null || queuePath.isEmpty()) {
            throw new LogRuntimeException("The QUEUE_PATH property and the "
                    + "DEFAULT_-OS-_QUEUE_PATH property are both undefined.");
        }

        return queuePath;
    }

    /**
     * Return the LogException which prevented direct submission to the server
     * on the most recent attempt, or null if none. This method allows access to
     * the exception which is masked when the submit method is called and
     * returns with a zero value indicating the submission was queued.
     *
     * @return The LogException or null
     */
    public LogException whyQueued() {
        return submitException;
    }

    /**
     * Queue the log item into configured file path.
     *
     * @throws InvalidXMLException If the XML is invalid
     * @throws LogIOException If unable to queue due to IO
     */
    void queue() throws InvalidXMLException, LogIOException {
        String filename = generateXMLFilename();
        String filepath = new File(getQueuePath(), filename).getAbsolutePath();
        queue(filepath);
    }

    /**
     * Queue the log item into the specified file path.
     *
     * @param filepath The queue file path
     * @throws InvalidXMLException If the XML is invalid
     * @throws LogIOException If unable to queue due to IO
     */
    void queue(String filepath) throws InvalidXMLException, LogIOException {
        try {
            validate();
        } catch (SchemaUnavailableException e) {
            // Ignore!
        }

        String xml = getXML();

        try (FileOutputStream out = new FileOutputStream(filepath);
                OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8")) {
            writer.write(xml);
        } catch (IOException e) {
            throw new LogIOException("Unable to write XML file.", e);
        }
    }
}
