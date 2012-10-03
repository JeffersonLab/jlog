package org.jlab.elog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.ResourceBundle;
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
 * An item that can be posted to the electronic log book.
 *
 * @author ryans
 */
abstract class LogItem {

    private static final Logger logger = Logger.getLogger(
            LogItem.class.getName());    
    
    private static final String SUBMIT_URL;
    private static final String QUEUE_PATH;
    private static final String PEM_FILE_NAME = ".elogcert";
    private static final FileNameMap mimeMap = URLConnection.getFileNameMap();

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        SUBMIT_URL = bundle.getString("SUBMIT_URL");
        QUEUE_PATH = bundle.getString("QUEUE_PATH");
    }
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
     * Add a file attachment with an empty caption and a hastily guessed mime
     * type. The mime type is guessed by using the readily available
     * java.net.URLConnection file name map, which simply looks at file
     * extension and compares with the very limited lookup file at:
     * <verbatim>[JRE_HOME]\lib\content-types.properties</verbatim>
     *
     * @param filepath The file path
     * @throws LogIOException If unable to add the attachment due to IO
     * @throws LogRuntimeException If unable to add the attachment
     */
    public void addAttachment(String filepath) throws LogIOException,
            LogRuntimeException {
        addAttachment(filepath, "", mimeMap.getContentTypeFor(filepath));
    }

    /**
     * Add a file attachment with the specified caption and mime type.
     *
     * @param filepath The file path
     * @param caption The The caption
     * @param mimeType The mime type
     * @throws LogIOException If unable to add the attachment due to IO
     * @throws LogRuntimeException If unable to add the attachment
     */
    public void addAttachment(String filepath, String caption, String mimeType)
            throws LogIOException, LogRuntimeException {
        File file = null;
        String data = null;
        Element attachmentsElement = null;

        try {
            file = new File(filepath);
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
    }

    /**
     * Returns the file attachments.
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
                attachments.add(new Attachment((Element) children.item(i)));
            }
        }

        return attachments.toArray(new Attachment[]{});
    }

    /**
     * Removes the file attachments.
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
    }

    public void setEmailNotify(String addresses) throws LogRuntimeException {
        Element notificationsElement = null;

        try {
            notificationsElement = (Element) notificationsExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
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
            XMLUtil.appendCommaDelimitedElementsWithText(doc, notificationsElement, "email", addresses);
        }
    }

    public void setEmailNotify(String[] addresses) throws LogRuntimeException {
        setEmailNotify(IOUtil.arrayToCSV(addresses));
    }

    public String getEmailNotifyCSV() throws LogRuntimeException {
        return IOUtil.arrayToCSV(getEmailNotify());
    }

    public String[] getEmailNotify() throws LogRuntimeException {
        String[] addresses;
        NodeList notificationElements = null;

        try {
            notificationElements = (NodeList) notificationListExpression.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (notificationElements != null) {
            addresses = XMLUtil.buildArrayFromText(notificationElements);
        } else {
            addresses = new String[0];
        }


        return addresses;
    }

    public String getAuthor() throws LogRuntimeException {
        String author = null;

        try {
            author = (String) authorTextExpression.evaluate(doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        return author;
    }

    void setLogNumber(long lognumber) throws LogRuntimeException {
        Element lognumberElement = null;

        try {
            lognumberElement = (Element) lognumberExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (lognumberElement == null) {
            lognumberElement = doc.createElement("lognumber");
            root.appendChild(lognumberElement);
        }

        lognumberElement.setTextContent(String.valueOf(lognumber));
    }

    public Long getLogNumber() throws LogRuntimeException {
        Long lognumber = null;
        String lognumberStr = null;

        try {
            lognumberStr = (String) lognumberTextExpression.evaluate(doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (lognumberStr != null && !lognumberStr.isEmpty()) { // TODO: C++ impl throws exception if null
            try {
                lognumber = Long.parseLong(lognumberStr);
            } catch (NumberFormatException e) {
                throw new LogRuntimeException("Unable to obtain log number due to non-numeric format.", e);
            }
        }

        return lognumber;
    }

    public GregorianCalendar getCreated() throws LogRuntimeException {
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

        String createdStr = createdElement.getTextContent();

        return XMLUtil.toGregorianCalendar(createdStr);
    }

    public Body getBody() throws LogRuntimeException {
        Body body = null;
        Element bodyElement = null;

        try {
            bodyElement = (Element) bodyExpression.evaluate(doc, XPathConstants.NODE);
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
                    throw new LogRuntimeException("Unexpected ContentType in XML body.", e);
                }
            }

            body = new Body(type, content);
        }

        return body;
    }

    void setBody(Body body) throws LogRuntimeException {
        if (body == null) {
            body = new Body(Body.ContentType.TEXT, "");
        }

        Element bodyElement = null;

        try {
            bodyElement = (Element) bodyExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
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

    public String getXML() throws LogRuntimeException {
        String xml = null;

        try {
            xml = XMLUtil.getXML(doc);
        } catch (TransformerConfigurationException e) {
            throw new LogRuntimeException("Unable to obtain XML document transformer.", e);
        } catch (TransformerException e) {
            throw new LogRuntimeException("Unable to transform XML document.", e);
        }

        return xml;
    }

    /**
     * Return the URL to the schema needed for validation of this log book item.
     * 
     * @return The URL
     */
    abstract String getSchemaURL();

    void validate() throws SchemaUnavailableException, InvalidXMLException, LogIOException {
        Schema schema = null;

        try {
            URL schemaURL = new URL(getSchemaURL());
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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
            throw new InvalidXMLException("The XML failed to validate against the schema.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to validate XML.", e);
        }
    }

    long parseResponse(InputStream is) throws LogIOException, LogRuntimeException {
        long id;

        try {
            Document response = builder.parse(is);

            String status = (String) responseStatusExpression.evaluate(response, XPathConstants.STRING);
            String message = (String) responseMessageExpression.evaluate(response, XPathConstants.STRING);
            String lognumberStr = (String) responseLognumberExpression.evaluate(response, XPathConstants.STRING);

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
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        } catch (NumberFormatException e) {
            throw new LogIOException("Log number not found in response.", e);
        }

        return id;
    }

    long putToServer(String pemFilePath) throws LogIOException, LogCertificateException, LogRuntimeException {
        long id;

        String xml = getXML();

        HttpsURLConnection con;
        OutputStreamWriter writer = null;
        InputStream is = null;
        InputStream error = null;

        try {
            URL url = new URL(getPutPath());
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(pemFilePath, true));
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.connect();
            writer = new OutputStreamWriter(con.getOutputStream());
            writer.write(xml);
            writer.close();

            is = con.getInputStream();
            error = con.getErrorStream();

            if (error != null) {
                String errorMsg = IOUtil.streamToString(error, "UTF-8");
                System.err.println(errorMsg);
            }

            id = parseResponse(is);
        } catch (MalformedURLException e) {
            throw new LogIOException("Invalid submission URL: check config file.", e);
        } catch (IOException e) {
            throw new LogIOException("Unable to submit to ELOG server.", e);
        } catch (ClassCastException e) {
            throw new LogIOException("Expected HTTP URL; check config file.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new LogCertificateException("Invalid SSL certificate algorithm.", e);
        } catch (CertificateException e) {
            throw new LogCertificateException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (KeyStoreException e) {
            throw new LogCertificateException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (KeyManagementException e) {
            throw new LogCertificateException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (UnrecoverableKeyException e) {
            throw new LogCertificateException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (InvalidKeySpecException e) {
            throw new LogCertificateException("Unable to obtain SSL connection due to certificate error.", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, 
                        "Unable to close output stream during put request.", e);
            }

            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, 
                        "Unable to close input stream during put request.", e);
            }

            try {
                if (error != null) {
                    error.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, 
                        "Unable to close error stream during put request.", e);
            }
        }

        return id;
    }

    Document getDocument() {
        return doc;
    }

    Element getRoot() {
        return root;
    }

    XPath getXPath() {
        return xpath;
    }

    String getPutPath() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append(SUBMIT_URL);

        if (!SUBMIT_URL.endsWith("/")) {
            strBuilder.append("/");
        }

        strBuilder.append(generateXMLFilename());

        return strBuilder.toString();
    }

    String getDefaultCertificatePath() {
        return new File(System.getProperty("user.home"), PEM_FILE_NAME).getAbsolutePath();
    }

    public long submit() throws LogException {
        return submit(getDefaultCertificatePath());
    }

    public long submit(String pemFilePath) throws InvalidXMLException, LogIOException {
        long id = 0L;

        try {
            id = putToServer(pemFilePath);
        } catch (Exception e) {
            // Ignore exceptions
            queue();
        }

        return id;
    }

    public long sumbitNow() throws LogIOException, LogCertificateException, LogRuntimeException {
        return putToServer(getDefaultCertificatePath());
    }

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

    void queue() throws InvalidXMLException, LogIOException {
        String filename = generateXMLFilename();
        String filepath = new File(QUEUE_PATH, filename).getAbsolutePath();
        queue(filepath);
    }

    void queue(String filepath) throws InvalidXMLException, LogIOException {
        try {
            validate();
        } catch (SchemaUnavailableException e) {
            // Ignore!
        }

        String xml = getXML();

        FileWriter writer = null;

        try {
            writer = new FileWriter(filepath);

            writer.write(xml);
        } catch (IOException e) {
            throw new LogIOException("Unable to write XML file.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Unable to close XML file.");
                }
            }
        }
    }
}
