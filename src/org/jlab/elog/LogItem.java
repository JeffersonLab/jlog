package org.jlab.elog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
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
import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
 *
 * @author ryans
 */
abstract class LogItem {

    private static final String SUBMIT_URL;
    private static final String QUEUE_PATH;
    private static final String PEM_FILE_NAME = ".elogcert";

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

    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LogRuntimeException("Unable to obtain XML document builder.", e);
        }

        try {
            typeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new LogRuntimeException("Unable to obtain XML datatype factory.", e);
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
            notificationListExpression = xpath.compile("/*/Notifications/email");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to construct XML XPath query", e);
        }
    }

    public LogItem() throws LogRuntimeException {
    }

    public LogItem(String rootTagName) throws LogRuntimeException {

        doc = builder.newDocument();

        root = doc.createElement(rootTagName);
        doc.appendChild(root);

        XMLUtil.appendElementWithText(doc, root, "created", XMLUtil.toXMLFormat(new GregorianCalendar()));

        Element authorElement = doc.createElement("Author");
        root.appendChild(authorElement);
        XMLUtil.appendElementWithText(doc, authorElement, "username", System.getProperty("user.name"));
    }

    public void addAttachment(String filepath) throws LogIOException, LogRuntimeException {
        addAttachment(filepath, "", "");
    }

    public void addAttachment(String filepath, String caption, String mimeType) throws LogIOException, LogRuntimeException {
        File file = null;
        String data = null;
        Element attachmentsElement = null;

        try {
            file = new File(filepath);
            data = XMLUtil.encodeBase64(IOUtil.fileToBytes(file));
            attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);
        } catch (IOException e) {
            throw new LogIOException("Unable to access attachment file.", e);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        Element attachmentElement = doc.createElement("Attachment");
        attachmentsElement.appendChild(attachmentElement);
        XMLUtil.appendElementWithText(doc, attachmentElement, "caption", caption);
        XMLUtil.appendElementWithText(doc, attachmentElement, "filename", file.getName());
        XMLUtil.appendElementWithText(doc, attachmentElement, "type", mimeType);
        Element dataElement = XMLUtil.appendElementWithText(doc, attachmentElement, "data", data);
        dataElement.setAttribute("encoding", "base64");
    }

    public Attachment[] getAttachments() throws LogRuntimeException {
        List<Attachment> attachments = new ArrayList<Attachment>();
        Element attachmentsElement = null;

        try {
            attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (attachmentsElement != null) {
            NodeList children = attachmentsElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                attachments.add(new Attachment((Element) children.item(i)));
            }
        }

        return attachments.toArray(new Attachment[]{});
    }

    public void deleteAttachments() throws LogRuntimeException {
        Element attachmentsElement = null;

        try {
            attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
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

    public String getEmailNotify() throws LogRuntimeException {
        String addresses = null;
        NodeList notificationElements = null;

        try {
            notificationElements = (NodeList) notificationListExpression.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to evaluate XPath query on XML DOM.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected node type in XML DOM.", e);
        }

        if (notificationElements != null) {
            addresses = XMLUtil.buildCommaDelimitedFromText(notificationElements);
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

    public void setLogNumber(Long lognumber) throws LogRuntimeException {
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

        lognumberElement.setTextContent(lognumber.toString());
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

            if (typeStr != null) {
                try {
                    type = Body.ContentType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new LogRuntimeException("Unexpected ContentType in XML body", e);
                }
            }

            body = new Body(type, content);
        }

        return body;
    }

    public void setBody(Body body) throws LogRuntimeException {
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
        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no"); // Ignored?
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            xml = writer.toString();
        } catch (TransformerConfigurationException e) {
            throw new LogRuntimeException("Unable to obtain XML document transformer.", e);
        } catch (TransformerException e) {
            throw new LogRuntimeException("Unable to transform XML document.", e);
        }

        return xml;
    }

    abstract String getSchemaURL();

    public void validate() throws SchemaUnavailableException, InvalidXMLException, LogIOException {
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

    public Long submit() throws LogException {
        String pemFilePath = new File(System.getProperty("user.home"), PEM_FILE_NAME).getAbsolutePath();
        return submit(pemFilePath);
    }

    public Long submit(String pemFilePath) throws LogIOException, LogCertificateException {
        Long id = null;

        String xml = getXML();

        HttpsURLConnection con;
        OutputStreamWriter writer = null;
        InputStreamReader reader = null;

        try {
            URL url = new URL(SUBMIT_URL);
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(pemFilePath, true));
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.connect();
            writer = new OutputStreamWriter(con.getOutputStream());
            writer.write(xml);
            reader = new InputStreamReader(con.getInputStream());
            // TODO: read response       
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
                e.printStackTrace();
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return id;
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
