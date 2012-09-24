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
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(LogItem.class.getName());
    public static final String SUBMIT_URL;
    public static final String QUEUE_PATH;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        SUBMIT_URL = bundle.getString("SUBMIT_URL");
        QUEUE_PATH = bundle.getString("QUEUE_PATH");
    }

    public enum ContentType {

        TEXT, HTML
    };

    public static class Body {

        private final ContentType type;
        private final String content;

        public Body(ContentType type, String content) {
            this.type = type;
            this.content = content;
        }

        public ContentType getType() {
            return type;
        }

        public String getContent() {
            return content;
        }
    }
    protected Document doc;
    protected Element root;
    protected DatatypeFactory typeFactory;
    protected DocumentBuilder builder;
    protected XPath xpath;
    protected XPathExpression lognumberExpression;
    protected XPathExpression lognumberTextExpression;
    protected XPathExpression createdExpression;
    protected XPathExpression bodyExpression;
    protected XPathExpression attachmentsExpression;

    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LogException("Unable to obtain XML document builder.", e);
        }

        try {
            typeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new LogException("Unable to obtain XML datatype factory.", e);
        }

        XPathFactory xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();

        try {
            lognumberExpression = xpath.compile("/*/lognumber");
            lognumberTextExpression = xpath.compile("/*/lognumber/text()");
            createdExpression = xpath.compile("/*/created");
            bodyExpression = xpath.compile("/*/body");
            attachmentsExpression = xpath.compile("/*/Attachments");
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to construct XML XPath query", e);
        }
    }

    public LogItem() throws LogException {
    }

    public LogItem(String rootTagName) throws LogException {

        doc = builder.newDocument();

        root = doc.createElement(rootTagName);
        doc.appendChild(root);

        XMLUtil.appendElementWithText(doc, root, "created", XMLUtil.toXMLFormat(new GregorianCalendar()));
    }

    public void addAttachment(String filename) throws LogException {
        addAttachment(filename, "", "");
    }

    public void addAttachment(String filename, String caption, String mimeType) throws LogException {

        try {
            String data = XMLUtil.encodeBase64(IOUtil.fileToBytes(new File(filename)));
            Element attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);

            Element attachmentElement = doc.createElement("Attachment");
            attachmentsElement.appendChild(attachmentElement);
            XMLUtil.appendElementWithText(doc, attachmentElement, "caption", caption);
            XMLUtil.appendElementWithText(doc, attachmentElement, "filename", filename);
            XMLUtil.appendElementWithText(doc, attachmentElement, "type", mimeType);
            Element dataElement = XMLUtil.appendElementWithText(doc, attachmentElement, "data", data);
            dataElement.setAttribute("encoding", "base64");
        } catch (IOException e) {
            throw new LogException("Unable to access attachment file.", e);            
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM via XPath.", e);
        }
    }

    public Attachment[] getAttachments() {
        List<Attachment> attachments = new ArrayList<Attachment>();

        try {
            Element attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);
            NodeList children = attachmentsElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                attachments.add(new Attachment((Element) children.item(i)));
            }

        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM via XPath.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected Node type in XML DOM.", e);
        }

        return attachments.toArray(new Attachment[]{});
    }

    public void setLogNumber(Long lognumber) throws LogException {
        try {
            Element lognumberElement = (Element) lognumberExpression.evaluate(doc, XPathConstants.NODE);

            if (lognumberElement == null) {
                lognumberElement = doc.createElement("lognumber");
                root.appendChild(lognumberElement);
            }

            lognumberElement.setTextContent(lognumber.toString());
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }
    }

    public Long getLogNumber() throws LogException {
        Long lognumber = null;

        try {
            String lognumberStr = (String) lognumberTextExpression.evaluate(doc, XPathConstants.STRING);

            if (lognumberStr != null && !lognumberStr.isEmpty()) { // TODO: C++ impl throws exception if null
                try {
                    lognumber = Long.parseLong(lognumberStr);
                } catch (NumberFormatException e) {
                    throw new LogException("Unable to obtain log number due to non-numeric format.", e);
                }
            }
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }

        return lognumber;
    }

    public void setCreated(GregorianCalendar created) throws LogException {
        try {
            Element createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);
            createdElement.setTextContent(XMLUtil.toXMLFormat(created));
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }
    }

    public GregorianCalendar getCreated() throws LogException {
        GregorianCalendar created = null;

        try {
            Element createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);
            String createdStr = createdElement.getTextContent();
            created = XMLUtil.toGregorianCalendar(createdStr);
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }

        return created;
    }

    public Body getBody() throws LogException {
        Body body = null;

        try {
            Element bodyElement = (Element) bodyExpression.evaluate(doc, XPathConstants.NODE);

            if (bodyElement != null) {
                String content = bodyElement.getTextContent();
                String typeStr = bodyElement.getAttribute("type");
                ContentType type = ContentType.TEXT;

                if (typeStr != null) {
                    type = ContentType.valueOf(typeStr.toUpperCase());
                }

                body = new Body(type, content);
            }
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        } catch (IllegalArgumentException e) {
            throw new LogException("Unexpected ContentType in XML body", e);
        }

        return body;
    }

    public void setBody(Body body) throws LogException {

        try {
            Element bodyElement = (Element) bodyExpression.evaluate(doc, XPathConstants.NODE);

            if (bodyElement != null) {
                root.removeChild(bodyElement);
            }

            if (body.getContent() != null && !body.getContent().isEmpty()) {
                bodyElement = doc.createElement("body");
                root.appendChild(bodyElement);

                if (body.getType() == ContentType.HTML) {
                    bodyElement.setAttribute("type", "html");
                }

                CDATASection data = doc.createCDATASection(body.getContent());
                bodyElement.appendChild(data);
            }
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }

    }

    public String getXML() throws LogException {
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
            throw new LogException("Unable to obtain XML document transformer.", e);
        } catch (TransformerException e) {
            throw new LogException("Unable to transform XML document.", e);
        }

        return xml;
    }

    protected abstract String getSchemaURL();

    public boolean validate() throws LogException {
        boolean obtainedSchema = false;
        Schema schema = null;

        try {
            URL schemaURL = new URL(getSchemaURL());
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = factory.newSchema(schemaURL);
            obtainedSchema = true;
        } catch (MalformedURLException e) {
            //throw new LogException("Schema URL malformed.", e);
            e.printStackTrace();

        } catch (SAXException e) {
            //throw new LogException("Unable to parse schema.", e);
            e.printStackTrace();
        }

        if (obtainedSchema) {
            Validator validator = schema.newValidator();

            DOMSource source = new DOMSource(doc);

            try {
                validator.validate(source);
            } catch (SAXException e) {
                throw new LogException("Invalid XML.", e);
            } catch (IOException e) {
                throw new LogException("Unable to validate XML.", e);
            }

        }

        return obtainedSchema;
    }

    public Long submit() throws LogException {
        Long id = null;

        String xml = getXML();

        String certFilePath = "C:/Users/ryans/Desktop/ryans.pem";

        HttpsURLConnection con;
        OutputStreamWriter writer = null;
        InputStreamReader reader = null;

        try {
            URL url = new URL(SUBMIT_URL);
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(certFilePath, true));
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.connect();
            writer = new OutputStreamWriter(con.getOutputStream());
            writer.write(xml);
            reader = new InputStreamReader(con.getInputStream());
            // TODO: read response       
        } catch (MalformedURLException e) {
            throw new LogException("Invalid submission URL: check config file.", e);
        } catch (IOException e) {
            throw new LogException("Unable to write to ELOG server.", e);
        } catch (ClassCastException e) {
            throw new LogException("Expected HTTP URL; check config file.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new LogException("Invalid SSL certificate algorithm.", e);
        } catch (CertificateException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (KeyStoreException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (KeyManagementException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (UnrecoverableKeyException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (InvalidKeySpecException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
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

    public Long submit(String certificatePath) {
        return null;
    }

    protected String generateXMLFilename() {
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

    protected void queue() throws LogException {
        validate(); // Ignore return value indicating could not obtain schema

        String xml = getXML();

        String filename = generateXMLFilename();
        FileWriter writer = null;

        try {
            writer = new FileWriter(new File(QUEUE_PATH, filename));

            writer.write(xml);
        } catch (IOException e) {
            throw new LogException("Unable to write XML file.", e);
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
